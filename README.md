# OpenSearch Natural Language Query POC

A Java-based Proof of Concept (POC) that converts natural language queries into OpenSearch DSL queries and executes them against AWS OpenSearch services.

## Features

- 🔍 **Natural Language to DSL Conversion**: Converts natural language queries to OpenSearch DSL
  - **Rule-based conversion**: Fast, lightweight conversion using pattern matching
  - **LLM-based conversion**: Advanced conversion using AWS Bedrock (optional)
- 🔌 **AWS OpenSearch Integration**: Direct integration with AWS OpenSearch services
- ⚙️ **Configurable**: Flexible configuration via environment variables or config files
- 📊 **Results Display**: Clean, formatted display of search results

## Prerequisites

- **Java 11+** (JDK 11 or higher)
- **Maven 3.6+**
- **AWS Account** with OpenSearch domain configured
- **AWS Credentials** configured (via AWS CLI, environment variables, or IAM role)
- **AWS Bedrock Access** (optional, only if using LLM-based conversion)

## Setup

### 1. Clone and Build

```bash
cd opensearch-nl-query-java
mvn clean install
```

### 2. Configure AWS Credentials

Ensure AWS credentials are configured using one of these methods:

**Option A: AWS CLI**
```bash
aws configure
```

**Option B: Environment Variables**
```bash
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export AWS_REGION=us-east-1
```

**Option C: IAM Role** (if running on EC2/ECS/Lambda)

### 3. Configure Application

Create a `.env` file or set environment variables:

```bash
# Required: OpenSearch endpoint
export OPENSEARCH_ENDPOINT=https://search-your-domain.us-east-1.es.amazonaws.com

# Optional: Override defaults
export AWS_REGION=us-east-1
export OPENSEARCH_INDEX=documents
export MAX_RESULTS=10

# Optional: Enable LLM-based conversion (AWS Bedrock)
export USE_LLM_CONVERSION=true
export BEDROCK_MODEL_ID=anthropic.claude-3-sonnet-20240229-v1:0
export BEDROCK_REGION=us-east-1
```

Or edit `src/main/resources/application.conf`:

```hocon
opensearch {
  endpoint = "https://search-your-domain.us-east-1.es.amazonaws.com"
  defaultIndex = "documents"
  maxResults = 10
}

aws {
  region = "us-east-1"
}

nl {
  conversion {
    useLLM = false
    bedrock {
      modelId = "anthropic.claude-3-sonnet-20240229-v1:0"
      region = "us-east-1"
    }
  }
}
```

## Usage

### Interactive Mode

Run the application without arguments to enter interactive mode:

```bash
mvn exec:java -Dexec.mainClass="com.opensearch.nlquery.App"
```

Or run the JAR:

```bash
java -jar target/nlquery-1.0.0.jar
```

Then enter queries interactively:

```
Query: find documents about machine learning
Query: search for files created after 2023
Query: show me all documents with status active
Query: exit
```

### Command Line Mode

Pass the query as command-line arguments:

```bash
mvn exec:java -Dexec.mainClass="com.opensearch.nlquery.App" -Dexec.args="find documents about machine learning"
```

Or:

```bash
java -jar target/nlquery-1.0.0.jar "find documents about machine learning"
```

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    App (Main Entry)                     │
└─────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│   Config     │   │  NL Query    │   │ OpenSearch  │
│   Loader     │   │   Service    │   │   Service   │
└──────────────┘   └──────────────┘   └──────────────┘
                            │                   │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ Rule-Based   │   │   LLM        │   │   AWS       │
│  Converter   │   │  Converter   │   │ OpenSearch  │
└──────────────┘   └──────────────┘   └──────────────┘
```

## Query Conversion

### Rule-Based Conversion (Default)

Fast, lightweight conversion using pattern matching. Handles common query patterns:

- Simple searches: "find documents about X"
- Filtered searches: "search for files with status active"
- Range queries: "show documents created after 2023"

**Example:**
```
Input:  "find documents about machine learning"
Output: {
  "query": {
    "bool": {
      "must": [{
        "match": {
          "_all": {
            "query": "machine learning",
            "operator": "and"
          }
        }
      }]
    }
  },
  "size": 10
}
```

### LLM-Based Conversion (Optional)

Advanced conversion using AWS Bedrock (Claude models). Better at understanding complex queries and generating sophisticated DSL.

**Enable:**
```bash
export USE_LLM_CONVERSION=true
export BEDROCK_MODEL_ID=anthropic.claude-3-sonnet-20240229-v1:0
export BEDROCK_REGION=us-east-1
```

**Available Models:**
- `anthropic.claude-3-sonnet-20240229-v1:0` (recommended)
- `anthropic.claude-3-haiku-20240307-v1:0` (faster, cheaper)
- `anthropic.claude-3-opus-20240229-v1:0` (most capable)

**Benefits:**
- Handles complex, multi-condition queries
- Better understanding of intent
- Generates more sophisticated DSL queries
- Handles edge cases better
- Integrated with AWS ecosystem

**Trade-offs:**
- Requires AWS Bedrock access
- Slightly slower (API call overhead)
- Additional cost per query

## Example Queries

### Simple Text Search
```
Query: find documents about artificial intelligence
```

### Filtered Search
```
Query: search for files with status published
```

### Date Range
```
Query: show documents created between 2023 and 2024
```

### Complex Query
```
Query: find all active documents about machine learning created after 2023
```

## Project Structure

```
opensearch-nl-query-java/
├── pom.xml                          # Maven configuration
├── README.md                        # This file
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/opensearch/nlquery/
│   │   │       ├── App.java                    # Main entry point
│   │   │       ├── config/
│   │   │       │   ├── AppConfig.java          # Configuration model
│   │   │       │   └── ConfigLoader.java       # Configuration loader
│   │   │       ├── converter/
│   │   │       │   ├── LLMQueryConverter.java  # LLM-based converter (Bedrock)
│   │   │       │   └── RuleBasedQueryConverter.java  # Rule-based converter
│   │   │       ├── model/
│   │   │       │   └── SearchResult.java       # Search result model
│   │   │       └── service/
│   │   │           ├── NaturalLanguageQueryService.java  # NL to DSL service
│   │   │           └── OpenSearchService.java  # OpenSearch client service
│   │   └── resources/
│   │       ├── application.conf     # Configuration file
│   │       └── logback.xml          # Logging configuration
│   └── test/
│       └── java/                    # Test files (to be added)
└── logs/                            # Log files (created at runtime)
```

## Configuration Reference

### Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `OPENSEARCH_ENDPOINT` | AWS OpenSearch endpoint URL | Yes | - |
| `AWS_REGION` | AWS region | No | `us-east-1` |
| `OPENSEARCH_INDEX` | Default index to search | No | `documents` |
| `MAX_RESULTS` | Maximum results to return | No | `10` |
| `USE_LLM_CONVERSION` | Enable LLM-based conversion | No | `false` |
| `BEDROCK_MODEL_ID` | AWS Bedrock model ID (if using LLM) | Conditional | `anthropic.claude-3-sonnet-20240229-v1:0` |
| `BEDROCK_REGION` | AWS region for Bedrock | No | Uses `AWS_REGION` |
| `CONNECTION_TIMEOUT` | Connection timeout (ms) | No | `5000` |
| `SOCKET_TIMEOUT` | Socket timeout (ms) | No | `10000` |

### Configuration File

See `src/main/resources/application.conf` for detailed configuration options.

## Troubleshooting

### Connection Issues

**Error: "Failed to initialize OpenSearch client"**
- Verify `OPENSEARCH_ENDPOINT` is correct
- Check AWS credentials are configured
- Ensure network connectivity to OpenSearch endpoint
- Verify IAM permissions for OpenSearch access

**Error: "Connection timeout"**
- Increase `CONNECTION_TIMEOUT` in configuration
- Check network/firewall settings
- Verify OpenSearch domain is accessible

### Query Issues

**Error: "Query conversion failed"**
- Check query syntax
- For LLM conversion, verify AWS Bedrock access and model ID
- Review logs for detailed error messages

**No results returned**
- Verify index exists and has data
- Check query is generating correct DSL (review logs)
- Try simpler query to test connectivity

### LLM Conversion Issues

**Error: "Bedrock API error"**
- Verify AWS credentials have Bedrock permissions
- Check Bedrock model access is enabled in AWS Console
- Verify `BEDROCK_MODEL_ID` is correct
- Ensure `BEDROCK_REGION` matches your AWS region

## Development

### Building

```bash
mvn clean compile
```

### Running Tests

```bash
mvn test
```

### Creating JAR

```bash
mvn clean package
```

This creates an executable JAR in `target/nlquery-1.0.0.jar`

### Running from JAR

```bash
java -jar target/nlquery-1.0.0.jar "your query here"
```

## Future Enhancements

- [ ] Support for more complex query patterns
- [ ] Query result caching
- [ ] Batch query processing
- [ ] Query history and analytics
- [ ] Support for multiple indices
- [ ] Advanced filtering and aggregation
- [ ] Integration with other LLM providers
- [ ] Query validation and suggestions
- [ ] REST API endpoint
- [ ] Web UI for query interface

## License

This is a Proof of Concept project for internal use.

## Support

For issues or questions, please contact the development team.

