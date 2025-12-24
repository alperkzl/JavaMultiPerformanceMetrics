#!/usr/bin/env python3
"""
3D Interactive Plot for Single-Objective Algorithm Analysis (using Plotly)

Creates an interactive 3D scatter plot that can be rotated, zoomed, and explored.
Shows all three objectives:
  - X: Makespan (s)
  - Y: Energy Consumption (Wh)
  - Z: Avg. Wait Time (s)

Points are colored by algorithm type:
  - Green: GA variants
  - Blue: GA_ISL variants
  - Red: SA variants

Features:
  - Rotate by dragging
  - Zoom with scroll wheel
  - Hover over points to see details
  - Toggle algorithms in legend

Usage:
    python3 plot_3d_interactive.py --data <json_file> [options]
"""

import argparse
import json
import sys
from typing import Dict, List, Any

try:
    import plotly.graph_objects as go
    from plotly.subplots import make_subplots
except ImportError:
    print("Error: Plotly is required for interactive 3D plots.", file=sys.stderr)
    print("Install with: pip3 install plotly", file=sys.stderr)
    sys.exit(1)

# Algorithm type to color mapping
TYPE_COLORS = {
    'GA': '#228B22',      # Green (Forest Green)
    'GA_ISL': '#0000FF',  # Blue
    'SA': '#FF0000',      # Red
}

TYPE_NAMES = {
    'GA': 'GA Variants',
    'GA_ISL': 'GA_ISL Variants',
    'SA': 'SA Variants',
}


def plot_3d_interactive(data: Dict[str, Any], args: argparse.Namespace) -> None:
    """Create the 3D interactive scatter plot."""

    algorithms = data.get('algorithms', {})
    objective_x = data.get('objective_x', 'Makespan (s)')
    objective_y = data.get('objective_y', 'Energy Consumption (Wh)')
    objective_z = data.get('objective_z', 'Avg. Wait Time (s)')
    task_counts = data.get('task_counts', [])

    # Create figure
    fig = go.Figure()

    # Group algorithms by type
    type_data = {}
    for algo_name, algo_data in algorithms.items():
        algo_type = algo_data.get('type', 'GA')
        if algo_type not in type_data:
            type_data[algo_type] = {
                'x': [], 'y': [], 'z': [],
                'labels': [], 'hover_texts': [],
                'color': algo_data.get('color', TYPE_COLORS.get(algo_type, '#000000'))
            }

        points = algo_data.get('points', [])
        for p in points:
            type_data[algo_type]['x'].append(p['x'])
            type_data[algo_type]['y'].append(p['y'])
            type_data[algo_type]['z'].append(p['z'])
            type_data[algo_type]['labels'].append(p['label'])
            type_data[algo_type]['hover_texts'].append(
                f"<b>{p['label']}</b><br>" +
                f"Makespan: {p['x']:.2f} s<br>" +
                f"Energy: {p['y']:.2f} Wh<br>" +
                f"Avg Wait: {p['z']:.2f} s"
            )

    # Add traces for each algorithm type
    for algo_type, type_info in type_data.items():
        if not type_info['x']:
            continue

        fig.add_trace(go.Scatter3d(
            x=type_info['x'],
            y=type_info['y'],
            z=type_info['z'],
            mode='markers+text' if args.labels else 'markers',
            marker=dict(
                size=args.marker_size,
                color=type_info['color'],
                opacity=0.8,
                line=dict(width=1, color='white')
            ),
            text=type_info['labels'] if args.labels else None,
            textposition='top center',
            textfont=dict(size=8),
            hovertemplate="%{customdata}<extra></extra>",
            customdata=type_info['hover_texts'],
            name=TYPE_NAMES.get(algo_type, algo_type),
            showlegend=args.legend
        ))

    # Set title
    if args.title:
        title = args.title
    else:
        title = "3D Interactive View: Makespan vs Energy vs Avg. Wait Time"

    # Update layout
    fig.update_layout(
        title=dict(
            text=title,
            font=dict(size=16, family='Arial, sans-serif'),
            x=0.5
        ),
        scene=dict(
            xaxis=dict(
                title=dict(text=objective_x, font=dict(size=12)),
                gridcolor='lightgray',
                showbackground=True,
                backgroundcolor='rgb(245, 245, 245)'
            ),
            yaxis=dict(
                title=dict(text=objective_y, font=dict(size=12)),
                gridcolor='lightgray',
                showbackground=True,
                backgroundcolor='rgb(245, 245, 245)'
            ),
            zaxis=dict(
                title=dict(text=objective_z, font=dict(size=12)),
                gridcolor='lightgray',
                showbackground=True,
                backgroundcolor='rgb(245, 245, 245)'
            ),
            camera=dict(
                eye=dict(x=1.5, y=1.5, z=1.0)
            )
        ),
        legend=dict(
            yanchor="top",
            y=0.99,
            xanchor="left",
            x=0.01,
            bgcolor='rgba(255, 255, 255, 0.8)',
            bordercolor='rgba(0, 0, 0, 0.3)',
            borderwidth=1
        ),
        margin=dict(l=0, r=0, t=50, b=0),
        paper_bgcolor='white'
    )

    # Save as HTML
    fig.write_html(args.output, include_plotlyjs=True, full_html=True)
    print(f"3D Interactive Plot saved to: {args.output}")
    print("Open this HTML file in a web browser to interact with the plot.")


def main():
    parser = argparse.ArgumentParser(
        description='Create 3D interactive scatter plot for single-objective algorithm analysis'
    )

    parser.add_argument('--data', required=True,
                        help='Path to JSON data file')
    parser.add_argument('--output', default='plot_3d_interactive.html',
                        help='Output HTML file path')
    parser.add_argument('--title', default=None,
                        help='Plot title (auto-generated if not specified)')
    parser.add_argument('--legend', type=str, default='true',
                        help='Show legend: true/false')
    parser.add_argument('--labels', type=str, default='false',
                        help='Show point labels: true/false')
    parser.add_argument('--marker-size', type=int, default=8,
                        help='Size of markers')

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
    plot_3d_interactive(data, args)


if __name__ == '__main__':
    main()
