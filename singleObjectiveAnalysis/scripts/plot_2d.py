#!/usr/bin/env python3
"""
2D Pareto-style Plot for Single-Objective Algorithm Analysis

Creates a scatter plot comparing two objectives across all algorithms.
Each algorithm has its own distinct color:
  - SA variants: Red tones (Red, Orange, Dark Red)
  - GA variants: Blue tones (Blue, Light Blue, Dark Blue)
  - GA_ISL variants: Green tones (Green, Light Green, Dark Green)

Usage:
    python3 plot_2d.py --data <json_file> [options]
"""

import argparse
import json
import sys
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import matplotlib.lines as mlines
import numpy as np
from typing import Dict, List, Any

# Individual algorithm color mapping
ALGORITHM_COLORS = {
    # SA variants - Red tones
    'SA_Makespan': '#FF0000',      # Red
    'SA_Energy': '#FF8C00',        # Orange
    'SA_AvgWait': '#8B0000',       # Dark Red
    # GA variants - Blue tones
    'GA_MAKESPAN': '#0000FF',      # Blue
    'GA_Energy': '#87CEEB',        # Light Blue (Sky Blue)
    'GA_AvgWait': '#00008B',       # Dark Blue
    # GA_ISL variants - Green tones
    'GA_ISL_Makespan': '#228B22',  # Green (Forest Green)
    'GA_ISL_Energy': '#90EE90',    # Light Green
    'GA_ISL_AvgWait': '#006400',   # Dark Green
}

# Fallback type colors
TYPE_COLORS = {
    'GA': '#0000FF',      # Blue
    'GA_ISL': '#228B22',  # Green
    'SA': '#FF0000',      # Red
}

# Marker shape mapping
MARKER_SHAPES = {
    'circle': 'o',
    'square': 's',
    'triangle': '^',
    'diamond': 'D',
    'star': '*',
    'plus': '+',
    'x': 'x',
}


def plot_2d(data: Dict[str, Any], args: argparse.Namespace) -> None:
    """Create the 2D scatter plot."""

    algorithms = data.get('algorithms', {})
    objective1 = data.get('objective1', 'Objective 1')
    objective2 = data.get('objective2', 'Objective 2')
    task_counts = data.get('task_counts', [])

    # Create figure
    fig, ax = plt.subplots(figsize=(args.width, args.height))

    # Set title
    if args.title:
        title = args.title
    else:
        title = f"{objective1} vs {objective2}"
    ax.set_title(title, fontsize=14, fontweight='bold')

    # Get marker
    marker = MARKER_SHAPES.get(args.marker_shape, 'o')
    marker_size = args.marker_size

    # Track plotted algorithms for legend
    plotted_algorithms = []

    # Plot each algorithm
    for algo_name, algo_data in algorithms.items():
        points = algo_data.get('points', [])
        algo_type = algo_data.get('type', 'GA')
        # Use color from JSON (which comes from Java), fallback to local mapping
        color = algo_data.get('color', ALGORITHM_COLORS.get(algo_name, TYPE_COLORS.get(algo_type, '#000000')))

        if not points:
            continue

        x_vals = [p['x'] for p in points]
        y_vals = [p['y'] for p in points]
        labels = [p['label'] for p in points]

        # Plot points
        scatter = ax.scatter(x_vals, y_vals, c=color, s=marker_size**2,
                   marker=marker, edgecolors='white', linewidths=0.5,
                   alpha=0.8, zorder=2, label=algo_name)

        plotted_algorithms.append((algo_name, color))

        # Add labels if enabled
        if args.labels:
            for x, y, label in zip(x_vals, y_vals, labels):
                ax.annotate(label, (x, y),
                           textcoords="offset points", xytext=(5, 5),
                           fontsize=7, alpha=0.8,
                           bbox=dict(boxstyle='round,pad=0.2', facecolor='white', alpha=0.7))

    # Set axis labels
    ax.set_xlabel(objective1, fontsize=12)
    ax.set_ylabel(objective2, fontsize=12)

    # Add grid
    ax.grid(True, linestyle='--', alpha=0.3)

    # Create legend if enabled
    if args.legend and plotted_algorithms:
        legend_handles = []
        legend_labels = []

        for algo_name, color in plotted_algorithms:
            legend_handles.append(mlines.Line2D([], [], color=color, marker=marker,
                                                linestyle='None', markersize=8,
                                                markeredgecolor='white', markeredgewidth=0.5))
            legend_labels.append(algo_name)

        ax.legend(legend_handles, legend_labels, loc='upper right',
                 framealpha=0.9, fontsize=9)

    # Adjust layout
    plt.tight_layout()

    # Save figure
    plt.savefig(args.output, dpi=args.dpi, bbox_inches='tight',
                facecolor='white', edgecolor='none')
    plt.close()

    print(f"2D Plot saved to: {args.output}")


def main():
    parser = argparse.ArgumentParser(
        description='Create 2D scatter plot for single-objective algorithm analysis'
    )

    parser.add_argument('--data', required=True,
                        help='Path to JSON data file')
    parser.add_argument('--output', default='plot_2d.png',
                        help='Output image file path')
    parser.add_argument('--title', default=None,
                        help='Plot title (auto-generated if not specified)')
    parser.add_argument('--legend', type=str, default='true',
                        help='Show legend: true/false')
    parser.add_argument('--labels', type=str, default='false',
                        help='Show point labels: true/false')
    parser.add_argument('--marker-size', type=int, default=8,
                        help='Size of markers')
    parser.add_argument('--marker-shape', default='circle',
                        choices=['circle', 'square', 'triangle', 'diamond', 'star', 'plus', 'x'],
                        help='Shape of markers')
    parser.add_argument('--dpi', type=int, default=150,
                        help='Image DPI')
    parser.add_argument('--width', type=float, default=12,
                        help='Figure width in inches')
    parser.add_argument('--height', type=float, default=8,
                        help='Figure height in inches')

    args = parser.parse_args()

    # Convert string booleans
    args.legend = args.legend.lower() == 'true'
    args.labels = args.labels.lower() == 'true'

    # Load data
    try:
        with open(args.data, 'r') as f:
            data = json.load(f)
    except FileNotFoundError:
        print(f"Error: Data file not found: {args.data}", file=sys.stderr)
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"Error: Invalid JSON in data file: {e}", file=sys.stderr)
        sys.exit(1)

    # Create plot
    plot_2d(data, args)


if __name__ == '__main__':
    main()
