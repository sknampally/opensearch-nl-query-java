#!/bin/bash
# Setup and run script for OpenSearch Natural Language Query POC

set -e

echo "=== OpenSearch Natural Language Query POC Setup ==="
echo ""

# Check for Java 11
JAVA_11_HOME=$(/usr/libexec/java_home -v 11 2>/dev/null || echo "")

if [ -z "$JAVA_11_HOME" ]; then
    echo "❌ Java 11 is not installed!"
    echo ""
    echo "Please install Java 11 using one of these methods:"
    echo ""
    echo "Method 1: Homebrew (recommended)"
    echo "  brew install openjdk@11"
    echo "  export JAVA_HOME=\$(/usr/libexec/java_home -v 11)"
    echo ""
    echo "Method 2: Download from Adoptium"
    echo "  1. Visit: https://adoptium.net/temurin/releases/?version=11"
    echo "  2. Download macOS .pkg installer"
    echo "  3. Install the package"
    echo "  4. Run: export JAVA_HOME=\$(/usr/libexec/java_home -v 11)"
    echo ""
    echo "After installing Java 11, run this script again."
    exit 1
fi

echo "✅ Java 11 found at: $JAVA_11_HOME"
export JAVA_HOME="$JAVA_11_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

echo ""
echo "Java version:"
java -version

echo ""
echo "=== Compiling project ==="
mvn clean compile

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Compilation successful!"
    echo ""
    echo "=== Running application ==="
    echo "Note: Make sure you have set the required environment variables:"
    echo "  - OPENSEARCH_ENDPOINT"
    echo "  - AWS_REGION (optional, defaults to us-east-1)"
    echo "  - OPENSEARCH_INDEX (optional, defaults to documents)"
    echo ""
    echo "Starting application..."
    echo ""
    mvn exec:java -Dexec.mainClass="com.opensearch.nlquery.App"
else
    echo ""
    echo "❌ Compilation failed. Please check the errors above."
    exit 1
fi

