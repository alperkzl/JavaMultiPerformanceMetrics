#!/usr/bin/env python3
"""
Header Validation Script for Cloud VM Task Scheduling Experimental Data

This script checks all xlsx files in Multi-Objective Algorithms and Single-Objective
Algorithms directories for discrepancies in the header row.

Expected header columns (exact match):
    Makespan | Avg Waiting Time | Avg Execution Time | Avg Finish Time |
    Energy Use Wh | Avg VM Utilization % | Avg Host Utilization% | Avg Host IDLE Time (s)

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


# Expected header columns (exact match)
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

# Regex pattern for valid file names
# Matches: ANYTHING_rnd_SEED_hh_mm_ss_sol_XX.xlsx
# This covers both multi-objective and single-objective naming conventions
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
        # Multi-objective: check for known algorithm prefixes
        for algo in MULTI_OBJECTIVE_ALGORITHMS:
            if filename.startswith(algo):
                return algo
        # Fallback: extract everything before _rnd_ or _eVSs_ or _mVSs_
        match = re.match(r'^(MOEA_\w+?)(?:_eVSs|_mVSs)?_rnd_', filename)
        if match:
            return match.group(1)
    else:
        # Single-objective: get from parent folder name
        parent_folder = os.path.basename(os.path.dirname(filepath))
        if parent_folder in SINGLE_OBJECTIVE_MAPPING:
            return parent_folder

    return "UNKNOWN"


def get_header_from_xlsx(filepath):
    """
    Read the header row (first row) from an xlsx file.
    Returns a list of cell values or None if error.
    """
    try:
        wb = load_workbook(filepath, read_only=True, data_only=True)
        sheet = wb.active

        # Get first row values
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


def compare_headers(actual_headers, expected_headers):
    """
    Compare actual headers against expected headers (exact match).
    Returns (is_match, difference_description)
    """
    if isinstance(actual_headers, tuple):
        # Error occurred during reading
        return False, f"Read error: {actual_headers[1]}"

    if actual_headers is None:
        return False, "Could not read headers"

    if len(actual_headers) != len(expected_headers):
        return False, f"Column count mismatch: expected {len(expected_headers)}, got {len(actual_headers)}"

    mismatches = []
    for i, (actual, expected) in enumerate(zip(actual_headers, expected_headers)):
        if actual != expected:
            mismatches.append(f"Col {i+1}: expected '{expected}', got '{actual}'")

    if mismatches:
        return False, "; ".join(mismatches)

    return True, None


def scan_directory(base_path, is_multi_objective):
    """
    Scan a directory for xlsx files and validate headers.
    Returns (file_counts, violations)
    """
    file_counts = defaultdict(int)  # algorithm -> count
    violations = []  # list of (filepath, algorithm, difference)
    skipped_files = []  # files not matching naming convention

    for root, dirs, files in os.walk(base_path):
        for filename in files:
            if not filename.endswith('.xlsx'):
                continue

            filepath = os.path.join(root, filename)

            # Check if filename matches the documented convention
            if not is_valid_filename(filename):
                skipped_files.append(filepath)
                continue

            # Extract algorithm name
            algorithm = extract_algorithm_name(filepath, is_multi_objective)
            file_counts[algorithm] += 1

            # Read and validate header
            headers = get_header_from_xlsx(filepath)
            is_match, difference = compare_headers(headers, EXPECTED_HEADERS)

            if not is_match:
                violations.append((filepath, algorithm, difference, headers))

    return file_counts, violations, skipped_files


def main():
    # Get repository root (parent of debug folder)
    script_dir = Path(__file__).parent
    repo_root = script_dir.parent

    multi_obj_path = repo_root / "Multi-Objective Algorithms"
    single_obj_path = repo_root / "Single - Objective Algorithms"

    # Output file
    output_file = script_dir / "header_validation_report.txt"

    # Results storage
    all_results = []
    total_files = 0
    total_violations = 0

    print("=" * 80)
    print("HEADER VALIDATION SCRIPT")
    print(f"Started: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 80)
    print()
    print("Expected Header (exact match):")
    print(f"  {' | '.join(EXPECTED_HEADERS)}")
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
        print(f"Files with header violations: {len(violations)}")

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
        print(f"Files with header violations: {len(violations)}")

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
    print(f"Total violations found: {total_violations}")

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
                for filepath, algo, diff, actual_headers in result["violations"]:
                    rel_path = os.path.relpath(filepath, repo_root)
                    print(f"\nFile: {rel_path}")
                    print(f"Algorithm: {algo}")
                    print(f"Issue: {diff}")
                    if actual_headers and not isinstance(actual_headers, tuple):
                        print(f"Actual headers: {actual_headers}")
    else:
        print("\nNo violations found! All headers match the expected format.")

    # Write to file
    with open(output_file, 'w') as f:
        f.write("=" * 80 + "\n")
        f.write("HEADER VALIDATION REPORT\n")
        f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write("=" * 80 + "\n\n")

        f.write("Expected Header (exact match):\n")
        f.write(f"  {' | '.join(EXPECTED_HEADERS)}\n\n")

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
            f.write(f"Files with header violations: {len(result['violations'])}\n\n")

        f.write("=" * 80 + "\n")
        f.write("SUMMARY\n")
        f.write("=" * 80 + "\n")
        f.write(f"Total files processed: {total_files}\n")
        f.write(f"Total violations found: {total_violations}\n\n")

        if total_violations > 0:
            f.write("=" * 80 + "\n")
            f.write("VIOLATION DETAILS\n")
            f.write("=" * 80 + "\n")

            for result in all_results:
                if result["violations"]:
                    f.write(f"\n{result['section']}:\n")
                    f.write("-" * 40 + "\n")
                    for filepath, algo, diff, actual_headers in result["violations"]:
                        rel_path = os.path.relpath(filepath, repo_root)
                        f.write(f"\nFile: {rel_path}\n")
                        f.write(f"Algorithm: {algo}\n")
                        f.write(f"Issue: {diff}\n")
                        if actual_headers and not isinstance(actual_headers, tuple):
                            f.write(f"Actual headers: {actual_headers}\n")
        else:
            f.write("\nNo violations found! All headers match the expected format.\n")

    print()
    print(f"Report saved to: {output_file}")
    print()

    return 0 if total_violations == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
