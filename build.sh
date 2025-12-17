#!/bin/bash

# Build script for TaskProcessor Java application

# Create output directory
mkdir -p out

# Set classpath with all dependencies
CLASSPATH="lib/poi-5.2.3.jar:lib/poi-ooxml-5.2.3.jar:lib/poi-ooxml-lite-5.2.3.jar:lib/commons-compress-1.21.jar:lib/xmlbeans-5.1.1.jar:lib/commons-io-2.11.0.jar:lib/commons-collections4-4.4.jar:lib/log4j-api-2.18.0.jar"

# Compile all Java files
echo "Compiling Java files..."
javac -cp "$CLASSPATH" -d out src/taskprocessor/*.java

if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo ""
    echo "Run with:"
    echo "  ./run.sh <n> <includeSingleObjective> <objective1> <objective2>"
    echo ""
    echo "Example:"
    echo "  ./run.sh 700 true Energy Makespan"
else
    echo "Build failed!"
    exit 1
fi
