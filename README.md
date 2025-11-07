# OpenSearch NL Query POC

Minimal Java example that turns natural-language questions into OpenSearch DSL using either a rule-based parser or AWS Bedrock (Claude) and then executes the DSL against an OpenSearch domain.

## Prerequisites
- Java 11+
- Maven 3.6+
- Access to an AWS OpenSearch endpoint
- AWS credentials available through the default credential chain (CLI profile, env vars, or IAM role)

## Quick Start
```bash
# build
mvn clean package

# run interactively
mvn exec:java -Dexec.mainClass="com.opensearch.nlquery.App"
```

The app reads configuration from environment variables or `src/main/resources/application.conf`. At minimum set:
```bash
export OPENSEARCH_ENDPOINT="https://search-your-domain.us-east-1.es.amazonaws.com"
export AWS_REGION="us-east-1"
```

To enable Bedrock-backed conversion also set:
```bash
export USE_LLM_CONVERSION=true
export BEDROCK_MODEL_ID=anthropic.claude-3-sonnet-20240229-v1:0
export BEDROCK_REGION=us-east-1
```

All logs go to the console and `logs/opensearch-nl-query.log` (ignored in git).

