# Setup Guide for OpenSearch Natural Language Query POC

## Quick Start

1. **Prerequisites Check**
   ```bash
   java -version  # Should be 11+
   mvn -version   # Should be 3.6+
   ```

2. **Build the Project**
   ```bash
   cd opensearch-nl-query-java
   mvn clean install
   ```

3. **Configure Environment**
   ```bash
   export OPENSEARCH_ENDPOINT=https://search-your-domain.us-east-1.es.amazonaws.com
   export AWS_REGION=us-east-1
   export OPENSEARCH_INDEX=documents
   ```

4. **Run**
   ```bash
   mvn exec:java -Dexec.mainClass="com.opensearch.nlquery.App"
   ```

## AWS OpenSearch Configuration

### Option 1: AWS Managed OpenSearch Service

For AWS Managed OpenSearch (Elasticsearch Service), you'll need:

1. **IAM Policy** - Your AWS credentials need permissions:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": [
           "es:ESHttpGet",
           "es:ESHttpPost",
           "es:ESHttpPut"
         ],
         "Resource": "arn:aws:es:region:account-id:domain/domain-name/*"
       }
     ]
   }
   ```

2. **Endpoint Format**: `https://search-{domain-name}.{region}.es.amazonaws.com`

### Option 2: OpenSearch Serverless

For OpenSearch Serverless, you'll need:

1. **IAM Policy** with OpenSearch Serverless permissions
2. **Endpoint Format**: Varies based on your serverless collection setup

### Option 3: Fine-Grained Access Control

If your OpenSearch domain uses fine-grained access control:

1. You may need to provide master user credentials
2. Update `OpenSearchService.java` to use basic auth instead of AWS signing

## Troubleshooting Connection Issues

### Issue: "Failed to initialize OpenSearch client"

**Possible Causes:**
1. Incorrect endpoint URL
2. Missing AWS credentials
3. Network connectivity issues
4. IAM permissions not configured

**Solutions:**
1. Verify endpoint: `echo $OPENSEARCH_ENDPOINT`
2. Test AWS credentials: `aws sts get-caller-identity`
3. Test connectivity: `curl -I $OPENSEARCH_ENDPOINT`
4. Check IAM permissions in AWS Console

### Issue: "Authentication failed"

**Solutions:**
1. Verify AWS credentials are configured
2. Check IAM role/policy has OpenSearch permissions
3. For fine-grained access control, may need to update code to use basic auth

### Issue: "Connection timeout"

**Solutions:**
1. Increase timeout in `application.conf`:
   ```hocon
   opensearch {
     connectionTimeout = 10000
     socketTimeout = 20000
   }
   ```
2. Check security groups allow outbound HTTPS
3. Verify endpoint is accessible from your network

## Alternative: Using REST Client with Manual Signing

If the current OpenSearch client approach doesn't work, you can use a REST client with AWS request signing. See the `OpenSearchService.java` file for the current implementation. You may need to modify it based on your specific OpenSearch setup.

## Testing the Connection

Before running queries, test the connection:

```bash
# Test AWS credentials
aws sts get-caller-identity

# Test OpenSearch endpoint (may require authentication)
curl -X GET "$OPENSEARCH_ENDPOINT/_cluster/health" \
  --aws-sigv4 "aws:amz:us-east-1:es"
```

## Next Steps

Once the connection is working:
1. Verify your index exists: Check in AWS OpenSearch console
2. Test a simple query: `find documents`
3. Enable LLM conversion (optional): Set `USE_LLM_CONVERSION=true`

