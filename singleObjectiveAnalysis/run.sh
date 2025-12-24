#!/bin/bash
#
# Run script for Single Objective Analyzer
#
# Usage: ./run.sh [options]
#
# Examples:
#   ./run.sh                                    # Generate CSV reports only
#   ./run.sh --plot2d Makespan Energy          # Generate 2D plot
#   ./run.sh --plot3d                          # Generate 3D plots
#   ./run.sh --plot2d Energy AvgWait --plot3d  # Generate all plots
#   ./run.sh --help                            # Show help
#

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
OUT_DIR="$SCRIPT_DIR/out"
LIB_DIR="$PROJECT_ROOT/lib"

# Check if built
if [ ! -d "$OUT_DIR" ] || [ ! -f "$OUT_DIR/singleobjective/SingleObjectiveAnalyzer.class" ]; then
    echo "Application not built. Running build first..."
    "$SCRIPT_DIR/build.sh"
    if [ $? -ne 0 ]; then
        echo "Build failed. Cannot run."
        exit 1
    fi
fi

# Build classpath
CLASSPATH="$LIB_DIR/*:$OUT_DIR:$PROJECT_ROOT/out"
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OSTYPE" == "win32" ]]; then
    # Windows uses semicolons
    CLASSPATH="$LIB_DIR/*;$OUT_DIR;$PROJECT_ROOT/out"
fi

# Run the analyzer from project root
cd "$PROJECT_ROOT"
java -cp "$CLASSPATH" singleobjective.SingleObjectiveAnalyzer "$@"
