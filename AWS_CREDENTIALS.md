# AWS Credentials Configuration Guide

This application uses AWS SDK's **DefaultCredentialsProvider**, which automatically looks for credentials in the standard AWS credential chain. **You do NOT need to configure credentials in `application.conf`**.

## Quick Setup Options

### Option 1: AWS CLI (Recommended for Local Development)

```bash
# Install AWS CLI if not already installed
# Then configure:
aws configure

# This will prompt you for:
# - AWS Access Key ID
# - AWS Secret Access Key  
# - Default region name (e.g., us-east-1)
# - Default output format (json)
```

This creates credentials in `~/.aws/credentials` and config in `~/.aws/config`.

### Option 2: Environment Variables

```bash
export AWS_ACCESS_KEY_ID=your_access_key_here
export AWS_SECRET_ACCESS_KEY=your_secret_key_here
export AWS_REGION=us-east-1

# If using temporary credentials (e.g., from AWS SSO):
export AWS_SESSION_TOKEN=your_session_token_here
```

### Option 3: AWS Credentials File (Manual)

Create `~/.aws/credentials`:
```ini
[default]
aws_access_key_id = your_access_key_here
aws_secret_access_key = your_secret_key_here
```

Create `~/.aws/config`:
```ini
[default]
region = us-east-1
```

### Option 4: IAM Role (For EC2/ECS/Lambda)

If running on AWS infrastructure (EC2, ECS, Lambda), the application will automatically use the IAM role attached to the instance/container/function. No additional configuration needed.

## Credential Chain Order

The AWS SDK checks for credentials in this order (first match wins):

1. **Environment Variables** (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`)
2. **Java System Properties** (`aws.accessKeyId`, `aws.secretKey`)
3. **Web Identity Token** (for EKS/IRSA)
4. **Shared Credentials File** (`~/.aws/credentials`)
5. **Shared Config File** (`~/.aws/config`)
6. **Container Credentials** (ECS task role)
7. **Instance Profile Credentials** (EC2 instance role)

## Required IAM Permissions

### For OpenSearch Access:
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

### For Bedrock Access (if using LLM conversion):
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "bedrock:InvokeModel"
      ],
      "Resource": "arn:aws:bedrock:region::foundation-model/anthropic.claude-3-sonnet-20240229-v1:0"
    }
  ]
}
```

## Verify Your Credentials

Test that your credentials are configured correctly:

```bash
# Test AWS credentials
aws sts get-caller-identity

# Should output something like:
# {
#     "UserId": "AIDAXXXXXXXXXXXXXXXXX",
#     "Account": "123456789012",
#     "Arn": "arn:aws:iam::123456789012:user/your-username"
# }
```

## Troubleshooting

### "Unable to load credentials"
- Verify credentials are set in one of the locations above
- Check file permissions: `chmod 600 ~/.aws/credentials`
- Verify environment variables are exported: `echo $AWS_ACCESS_KEY_ID`

### "Access Denied" errors
- Verify IAM permissions are correct (see above)
- Check that the credentials have access to the specific AWS resources
- For Bedrock: Ensure model access is enabled in AWS Console

### "Invalid credentials"
- Verify access key and secret key are correct
- Check for typos or extra spaces
- If using temporary credentials, ensure `AWS_SESSION_TOKEN` is set and not expired

## Security Best Practices

1. **Never commit credentials to git** - Use environment variables or AWS CLI
2. **Use IAM roles** when running on AWS infrastructure (EC2/ECS/Lambda)
3. **Rotate credentials regularly** - Update access keys periodically
4. **Use least privilege** - Grant only the minimum permissions needed
5. **Use temporary credentials** when possible (AWS SSO, STS assume role)

## Additional Resources

- [AWS SDK for Java - Credentials](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html)
- [AWS CLI Configuration](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html)
- [IAM Best Practices](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html)

