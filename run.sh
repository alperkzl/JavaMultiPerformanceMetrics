#!/bin/bash

# Run script for TaskProcessor Java application

# Set classpath with all dependencies and compiled classes
CLASSPATH="out:lib/poi-5.2.3.jar:lib/poi-ooxml-5.2.3.jar:lib/poi-ooxml-lite-5.2.3.jar:lib/commons-compress-1.21.jar:lib/xmlbeans-5.1.1.jar:lib/commons-io-2.11.0.jar:lib/commons-collections4-4.4.jar:lib/log4j-api-2.18.0.jar"

# Get the directory where the script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Run the application
java -cp "$CLASSPATH" taskprocessor.TaskProcessor "$@" "$SCRIPT_DIR"
