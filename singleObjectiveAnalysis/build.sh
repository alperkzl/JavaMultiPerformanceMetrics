#!/bin/bash
#
# Build script for Single Objective Analyzer
#
# This script compiles the Java source files and prepares the application.
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
SRC_DIR="$SCRIPT_DIR/src"
OUT_DIR="$SCRIPT_DIR/out"
LIB_DIR="$PROJECT_ROOT/lib"

echo "========================================"
echo "  Building Single Objective Analyzer"
echo "========================================"
echo "Script directory: $SCRIPT_DIR"
echo "Project root: $PROJECT_ROOT"
echo ""

# Function to download dependencies
download_dependencies() {
    echo "Downloading Apache POI dependencies..."
    mkdir -p "$LIB_DIR"
    cd "$LIB_DIR"

    # Apache POI and dependencies
    MAVEN_REPO="https://repo1.maven.org/maven2"

    declare -a JARS=(
        "org/apache/poi/poi/5.2.3/poi-5.2.3.jar"
        "org/apache/poi/poi-ooxml/5.2.3/poi-ooxml-5.2.3.jar"
        "org/apache/poi/poi-ooxml-lite/5.2.3/poi-ooxml-lite-5.2.3.jar"
        "org/apache/commons/commons-compress/1.21/commons-compress-1.21.jar"
        "org/apache/xmlbeans/xmlbeans/5.1.1/xmlbeans-5.1.1.jar"
        "commons-io/commons-io/2.11.0/commons-io-2.11.0.jar"
        "org/apache/commons/commons-collections4/4.4/commons-collections4-4.4.jar"
        "org/apache/logging/log4j/log4j-api/2.18.0/log4j-api-2.18.0.jar"
    )

    for jar_path in "${JARS[@]}"; do
        jar_name=$(basename "$jar_path")
        if [ ! -f "$jar_name" ]; then
            echo "Downloading $jar_name..."
            curl -sLko "$jar_name" "$MAVEN_REPO/$jar_path"
        fi
    done

    cd "$SCRIPT_DIR"
    echo "Dependencies downloaded."
    echo ""
}

# Check if lib directory exists and has dependencies
if [ ! -d "$LIB_DIR" ]; then
    download_dependencies
fi

# Check for required JAR files
POI_JAR=$(find "$LIB_DIR" -name "poi-5*.jar" 2>/dev/null | grep -v ooxml | head -1)
OOXML_JAR=$(find "$LIB_DIR" -name "poi-ooxml-5*.jar" 2>/dev/null | grep -v lite | head -1)

if [ -z "$POI_JAR" ] || [ -z "$OOXML_JAR" ]; then
    echo "Dependencies not found. Downloading..."
    download_dependencies
    POI_JAR=$(find "$LIB_DIR" -name "poi-5*.jar" 2>/dev/null | grep -v ooxml | head -1)
    OOXML_JAR=$(find "$LIB_DIR" -name "poi-ooxml-5*.jar" 2>/dev/null | grep -v lite | head -1)
fi

if [ -z "$POI_JAR" ] || [ -z "$OOXML_JAR" ]; then
    echo "Error: Failed to download Apache POI libraries"
    exit 1
fi

# Create output directories
mkdir -p "$OUT_DIR"
mkdir -p "$PROJECT_ROOT/out"

# Build classpath
CLASSPATH="$LIB_DIR/*:$PROJECT_ROOT/out:$SRC_DIR"
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OSTYPE" == "win32" ]]; then
    # Windows uses semicolons
    CLASSPATH="$LIB_DIR/*;$PROJECT_ROOT/out;$SRC_DIR"
fi

# First, compile taskprocessor if needed (for ExcelReader dependency)
if [ ! -f "$PROJECT_ROOT/out/taskprocessor/ExcelReader.class" ]; then
    echo "Compiling taskprocessor dependency..."
    javac -cp "$CLASSPATH" -d "$PROJECT_ROOT/out" "$PROJECT_ROOT/src/taskprocessor/ExcelReader.java"
fi

echo "Compiling Single Objective Analyzer..."

# Compile single objective analyzer classes
javac -cp "$CLASSPATH" -d "$OUT_DIR" -sourcepath "$SRC_DIR:$PROJECT_ROOT/src" \
    "$SRC_DIR/singleobjective/Solution.java" \
    "$SRC_DIR/singleobjective/AveragePoint.java" \
    "$SRC_DIR/singleobjective/AlgorithmData.java" \
    "$SRC_DIR/singleobjective/DataParser.java" \
    "$SRC_DIR/singleobjective/ReportGenerator.java" \
    "$SRC_DIR/singleobjective/PythonPlotCaller.java" \
    "$SRC_DIR/singleobjective/SingleObjectiveAnalyzer.java"

if [ $? -eq 0 ]; then
    echo ""
    echo "Build successful!"
    echo "Output directory: $OUT_DIR"
    echo ""
    echo "To run the analyzer:"
    echo "  ./run.sh [options]"
    echo ""
    echo "For help:"
    echo "  ./run.sh --help"
else
    echo "Build failed!"
    exit 1
fi
