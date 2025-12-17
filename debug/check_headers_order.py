#!/usr/bin/env python3
"""
Header Order Validation Script for Cloud VM Task Scheduling Experimental Data

This script checks all xlsx files in Multi-Objective Algorithms and Single-Objective
Algorithms directories for discrepancies in the ORDER of header columns.

Expected header column order:
    1. Makespan
    2. Avg Waiting Time
    3. Avg Execution Time
    4. Avg Finish Time
    5. Energy Use Wh
    6. Avg VM Utilization %
    7. Avg Host Utilization%
    8. Avg Host IDLE Time (s)

The script normalizes headers (handles minor variations like extra spaces) and
checks if fields appear in the correct order.

Only processes files matching the naming conventions documented in README.md:
    - Multi-Objective: ALG_NAME_(objective)_rnd_SEED_hh_mm_ss_sol_XX.xlsx
    - Single-Objective: ALG_NAME_rnd_SEED_hh_mm_ss_sol_1.xlsx
"""

import os
import re
import sys
from pathlib import Path
from datetime import datetime
from collections import defaultdict

try:
    from openpyxl import load_workbook
except ImportError:
    print("ERROR: openpyxl is required. Install with: pip install openpyxl")
    sys.exit(1)


# Expected header columns in correct order
EXPECTED_HEADERS = [
    "Makespan",
    "Avg Waiting Time",
    "Avg Execution Time",
    "Avg Finish Time",
    "Energy Use Wh",
    "Avg VM Utilization %",
    "Avg Host Utilization%",
    "Avg Host IDLE Time (s)"
]

# Normalized versions of expected headers (for flexible matching)
# Maps normalized form -> canonical name
def normalize_header(header):
    """
    Normalize a header string for comparison.
    - Lowercase
    - Collapse multiple spaces to single space
    - Strip whitespace
    - Remove spaces before % symbol for consistency
    """
    if header is None:
        return ""
    h = str(header).lower().strip()
    h = re.sub(r'\s+', ' ', h)  # Collapse multiple spaces
    h = re.sub(r'\s+%', '%', h)  # Remove space before %
    h = re.sub(r'%\s+', '%', h)  # Remove space after %
    return h

# Create normalized expected headers
NORMALIZED_EXPECTED = [normalize_header(h) for h in EXPECTED_HEADERS]

# Create mapping from normalized -> original expected header
NORM_TO_EXPECTED = {normalize_header(h): h for h in EXPECTED_HEADERS}

# Regex pattern for valid file names
VALID_FILENAME_PATTERN = re.compile(
    r'^.+_rnd_\d+_\d{2}_\d{2}_\d{2}_sol_\d+\.xlsx$'
)

# Multi-objective algorithm names
MULTI_OBJECTIVE_ALGORITHMS = [
    "MOEA_AMOSA",
    "MOEA_eNSGAII",
    "MOEA_NSGAII",
    "MOEA_SPEAII"
]

# Single-objective algorithm prefixes (folder names map to file prefixes)
SINGLE_OBJECTIVE_MAPPING = {
    "GA_AvgWait": ["GA_STT"],
    "GA_Energy": ["GA_POWER"],
    "GA_ISL_AvgWait": ["GA_STT_ISL"],
    "GA_ISL_Energy": ["GA_POW_ISL"],
    "GA_ISL_Makespan": ["GA_MKS_ISL"],
    "GA_MAKESPAN": ["GA_MAKESPAN"],
    "LJF_BEST": ["LJF_BEST"],
    "LJF_WORST": ["LJF_WORST"],
    "SA_AvgWait": ["SA_STT"],
    "SA_Energy": ["SA_POWER"],
    "SA_Makespan": ["SA_MAKESPAN"],
    "SJF_BEST": ["SJF_BEST"],
    "SJF_WORST": ["SJF_WORST"]
}


def is_valid_filename(filename):
    """Check if filename matches the documented naming convention."""
    return VALID_FILENAME_PATTERN.match(filename) is not None


def extract_algorithm_name(filepath, is_multi_objective):
    """Extract the algorithm name from a file path."""
    filename = os.path.basename(filepath)

    if is_multi_objective:
        for algo in MULTI_OBJECTIVE_ALGORITHMS:
            if filename.startswith(algo):
                return algo
        match = re.match(r'^(MOEA_\w+?)(?:_eVSs|_mVSs)?_rnd_', filename)
        if match:
            return match.group(1)
    else:
        parent_folder = os.path.basename(os.path.dirname(filepath))
        if parent_folder in SINGLE_OBJECTIVE_MAPPING:
            return parent_folder

    return "UNKNOWN"


def get_header_from_xlsx(filepath):
    """
    Read the header row (first row) from an xlsx file.
    Returns a list of cell values or (None, error_message) if error.
    """
    try:
        wb = load_workbook(filepath, read_only=True, data_only=True)
        sheet = wb.active

        headers = []
        for cell in sheet[1]:
            value = cell.value
            if value is not None:
                headers.append(str(value).strip() if value else "")
            else:
                headers.append("")

        wb.close()

        # Remove trailing empty cells
        while headers and headers[-1] == "":
            headers.pop()

        return headers
    except Exception as e:
        return None, str(e)


def find_header_in_expected(actual_header):
    """
    Find which expected header matches the actual header (normalized comparison).
    Returns the index in EXPECTED_HEADERS or -1 if not found.
    """
    norm_actual = normalize_header(actual_header)

    for i, norm_expected in enumerate(NORMALIZED_EXPECTED):
        if norm_actual == norm_expected:
            return i

    return -1


def compare_headers_order(actual_headers, expected_headers):
    """
    Compare actual headers against expected headers focusing on ORDER.
    Normalizes headers before comparison.

    Returns (is_match, difference_description, details)
    """
    if isinstance(actual_headers, tuple):
        return False, f"Read error: {actual_headers[1]}", None

    if actual_headers is None:
        return False, "Could not read headers", None

    # Check column count
    if len(actual_headers) != len(expected_headers):
        return False, f"Column count mismatch: expected {len(expected_headers)}, got {len(actual_headers)}", None

    # Map each actual header to its expected position
    actual_positions = []
    unrecognized = []

    for i, actual in enumerate(actual_headers):
        expected_idx = find_header_in_expected(actual)
        if expected_idx == -1:
            unrecognized.append((i, actual))
        actual_positions.append(expected_idx)

    # Check for unrecognized headers
    if unrecognized:
        issues = [f"Col {i+1}: '{h}' not recognized" for i, h in unrecognized]
        return False, "Unrecognized headers: " + "; ".join(issues), {
            "type": "unrecognized",
            "details": unrecognized
        }

    # Check for order violations
    order_issues = []
    for actual_pos, expected_idx in enumerate(actual_positions):
        if actual_pos != expected_idx:
            actual_header = actual_headers[actual_pos]
            expected_at_pos = expected_headers[actual_pos]
            order_issues.append({
                "position": actual_pos + 1,
                "found": actual_header,
                "expected": expected_at_pos,
                "found_should_be_at": expected_idx + 1
            })

    if order_issues:
        issue_strs = []
        for issue in order_issues:
            issue_strs.append(
                f"Col {issue['position']}: found '{issue['found']}' (should be at col {issue['found_should_be_at']}), "
                f"expected '{issue['expected']}'"
            )
        return False, "Order violations: " + "; ".join(issue_strs), {
            "type": "order",
            "details": order_issues
        }

    return True, None, None


def scan_directory(base_path, is_multi_objective):
    """
    Scan a directory for xlsx files and validate header order.
    Returns (file_counts, violations, skipped_files)
    """
    file_counts = defaultdict(int)
    violations = []
    skipped_files = []

    for root, dirs, files in os.walk(base_path):
        for filename in files:
            if not filename.endswith('.xlsx'):
                continue

            filepath = os.path.join(root, filename)

            if not is_valid_filename(filename):
                skipped_files.append(filepath)
                continue

            algorithm = extract_algorithm_name(filepath, is_multi_objective)
            file_counts[algorithm] += 1

            headers = get_header_from_xlsx(filepath)
            is_match, difference, details = compare_headers_order(headers, EXPECTED_HEADERS)

            if not is_match:
                violations.append((filepath, algorithm, difference, headers, details))

    return file_counts, violations, skipped_files


def main():
    script_dir = Path(__file__).parent
    repo_root = script_dir.parent

    multi_obj_path = repo_root / "Multi-Objective Algorithms"
    single_obj_path = repo_root / "Single - Objective Algorithms"

    output_file = script_dir / "header_order_validation_report.txt"

    all_results = []
    total_files = 0
    total_violations = 0

    print("=" * 80)
    print("HEADER ORDER VALIDATION SCRIPT")
    print(f"Started: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 80)
    print()
    print("Expected Header Order:")
    for i, h in enumerate(EXPECTED_HEADERS, 1):
        print(f"  {i}. {h}")
    print()
    print("Note: Headers are normalized for comparison (minor spacing differences ignored)")
    print()

    # Process Multi-Objective Algorithms
    print("-" * 80)
    print("MULTI-OBJECTIVE ALGORITHMS")
    print("-" * 80)

    if multi_obj_path.exists():
        file_counts, violations, skipped = scan_directory(multi_obj_path, is_multi_objective=True)

        print("\nFile counts per algorithm:")
        for algo in sorted(file_counts.keys()):
            print(f"  {algo}: {file_counts[algo]} files")

        total = sum(file_counts.values())
        total_files += total
        total_violations += len(violations)

        print(f"\nTotal valid files: {total}")
        print(f"Skipped files (naming convention): {len(skipped)}")
        print(f"Files with order violations: {len(violations)}")

        all_results.append({
            "section": "Multi-Objective Algorithms",
            "file_counts": dict(file_counts),
            "violations": violations,
            "skipped": skipped
        })
    else:
        print(f"Directory not found: {multi_obj_path}")

    # Process Single-Objective Algorithms
    print()
    print("-" * 80)
    print("SINGLE-OBJECTIVE ALGORITHMS")
    print("-" * 80)

    if single_obj_path.exists():
        file_counts, violations, skipped = scan_directory(single_obj_path, is_multi_objective=False)

        print("\nFile counts per algorithm:")
        for algo in sorted(file_counts.keys()):
            print(f"  {algo}: {file_counts[algo]} files")

        total = sum(file_counts.values())
        total_files += total
        total_violations += len(violations)

        print(f"\nTotal valid files: {total}")
        print(f"Skipped files (naming convention): {len(skipped)}")
        print(f"Files with order violations: {len(violations)}")

        all_results.append({
            "section": "Single - Objective Algorithms",
            "file_counts": dict(file_counts),
            "violations": violations,
            "skipped": skipped
        })
    else:
        print(f"Directory not found: {single_obj_path}")

    # Summary
    print()
    print("=" * 80)
    print("SUMMARY")
    print("=" * 80)
    print(f"Total files processed: {total_files}")
    print(f"Total order violations found: {total_violations}")

    # Detailed violation report
    if total_violations > 0:
        print()
        print("=" * 80)
        print("VIOLATION DETAILS")
        print("=" * 80)

        for result in all_results:
            if result["violations"]:
                print(f"\n{result['section']}:")
                print("-" * 40)
                for filepath, algo, diff, actual_headers, details in result["violations"]:
                    rel_path = os.path.relpath(filepath, repo_root)
                    print(f"\nFile: {rel_path}")
                    print(f"Algorithm: {algo}")
                    print(f"Issue: {diff}")
                    if actual_headers and not isinstance(actual_headers, tuple):
                        print(f"Actual headers: {actual_headers}")
    else:
        print("\nNo order violations found! All headers are in the correct order.")

    # Write to file
    with open(output_file, 'w') as f:
        f.write("=" * 80 + "\n")
        f.write("HEADER ORDER VALIDATION REPORT\n")
        f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write("=" * 80 + "\n\n")

        f.write("Expected Header Order:\n")
        for i, h in enumerate(EXPECTED_HEADERS, 1):
            f.write(f"  {i}. {h}\n")
        f.write("\nNote: Headers are normalized for comparison (minor spacing differences ignored)\n\n")

        for result in all_results:
            f.write("-" * 80 + "\n")
            f.write(f"{result['section']}\n")
            f.write("-" * 80 + "\n\n")

            f.write("File counts per algorithm:\n")
            for algo in sorted(result["file_counts"].keys()):
                f.write(f"  {algo}: {result['file_counts'][algo]} files\n")

            total = sum(result["file_counts"].values())
            f.write(f"\nTotal valid files: {total}\n")
            f.write(f"Skipped files (naming convention): {len(result['skipped'])}\n")
            f.write(f"Files with order violations: {len(result['violations'])}\n\n")

        f.write("=" * 80 + "\n")
        f.write("SUMMARY\n")
        f.write("=" * 80 + "\n")
        f.write(f"Total files processed: {total_files}\n")
        f.write(f"Total order violations found: {total_violations}\n\n")

        if total_violations > 0:
            f.write("=" * 80 + "\n")
            f.write("VIOLATION DETAILS\n")
            f.write("=" * 80 + "\n")

            for result in all_results:
                if result["violations"]:
                    f.write(f"\n{result['section']}:\n")
                    f.write("-" * 40 + "\n")
                    for filepath, algo, diff, actual_headers, details in result["violations"]:
                        rel_path = os.path.relpath(filepath, repo_root)
                        f.write(f"\nFile: {rel_path}\n")
                        f.write(f"Algorithm: {algo}\n")
                        f.write(f"Issue: {diff}\n")
                        if actual_headers and not isinstance(actual_headers, tuple):
                            f.write(f"Actual headers: {actual_headers}\n")
        else:
            f.write("\nNo order violations found! All headers are in the correct order.\n")

    print()
    print(f"Report saved to: {output_file}")
    print()

    return 0 if total_violations == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
