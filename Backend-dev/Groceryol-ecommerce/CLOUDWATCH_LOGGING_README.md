# AWS CloudWatch Logging Implementation

This implementation provides centralized logging to AWS CloudWatch with controller-specific log groups, treating each controller as a separate "lambda" for better organization and monitoring.

## 🎯 **Overview**

The system now automatically sends all application logs to AWS CloudWatch with the following features:

- **Controller-Specific Log Groups**: Each controller gets its own log group
- **Automatic Log Organization**: Logs are automatically categorized by component
- **Structured Logging**: JSON-formatted logs with metadata
- **Real-time Monitoring**: Logs appear in CloudWatch in real-time
- **Performance Tracking**: API response times and request details
- **Error Tracking**: Comprehensive error logging with stack traces

## 🏗️ **Architecture**

### **Components**

1. **CloudWatchConfig**: AWS CloudWatch client configuration
2. **CloudWatchLogService**: Core logging service for CloudWatch
3. **CloudWatchAppender**: Custom Logback appender
4. **LoggingInterceptor**: HTTP request/response logging
5. **WebMvcConfig**: Interceptor registration

### **Log Flow**

```
Application Logs → Logback → CloudWatchAppender → CloudWatchLogService → AWS CloudWatch
```

## 📁 **Log Group Structure**

### **Automatic Log Group Creation**

The system automatically creates log groups with this structure:

```
/aws/application/Groceryol-ecommerce/
├── UserController/
├── OrderController/
├── ProductController/
├── PaymentController/
├── CartController/
├── CategoryController/
├── ReviewController/
├── SearchController/
├── WishlistController/
├── AddressController/
├── DiscountController/
└── [Other Controllers]/
```

### **Log Streams**

Each log group contains daily streams:
```
2025-01-20/UserController
2025-01-20/OrderController
2025-01-20/ProductController
...
```

## ⚙️ **Configuration**

### **Application Properties**

Add these to your `application.properties`:

```properties
# AWS CloudWatch Configuration
aws.cloudwatch.log.group.prefix=Groceryol-ecommerce
aws.cloudwatch.log.retention.days=7
aws.cloudwatch.enabled=true

# Existing AWS Configuration
aws.accessKeyId=your-access-key
aws.secretKey=your-secret-key
aws.region=ap-south-1
```

### **Dependencies**

The following dependency is automatically added to `pom.xml`:

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>cloudwatchlogs</artifactId>
    <version>${aws.sdk.version}</version>
</dependency>
```

## 🚀 **Usage**

### **Automatic Logging**

Once configured, logging happens automatically:

1. **Controller Logs**: All controller methods automatically log to CloudWatch
2. **Service Logs**: Service layer operations are logged
3. **Repository Logs**: Database operations are tracked
4. **API Requests**: HTTP requests/responses are automatically logged

### **Manual Logging**

You can also manually send logs to CloudWatch:

```java
@Autowired
private CloudWatchLogService cloudWatchLogService;

// Simple log
cloudWatchLogService.sendLog("UserController", "INFO", "User created successfully");

// Log with metadata
Map<String, Object> metadata = new HashMap<>();
metadata.put("userId", "123");
metadata.put("action", "create");
cloudWatchLogService.sendLog("UserController", "INFO", "User created", metadata);

// Error logging
cloudWatchLogService.sendErrorLog("UserController", "Failed to create user", exception);

// API request logging
cloudWatchLogService.sendApiLog("UserController", "POST", "/users", "123", "req456", 150, 201);
```

## 📊 **Log Format**

### **Standard Log Entry**

```json
{
  "timestamp": 1705737600000,
  "level": "INFO",
  "controller": "UserController",
  "message": "User created successfully",
  "metadata": {
    "userId": "123",
    "action": "create"
  },
  "thread": "http-nio-8085-exec-1"
}
```

### **API Request Log**

```json
{
  "timestamp": 1705737600000,
  "level": "INFO",
  "controller": "UserController",
  "message": "API Request processed",
  "metadata": {
    "method": "POST",
    "endpoint": "/users",
    "userId": "123",
    "requestId": "req456",
    "responseTime": 150,
    "statusCode": 201
  },
  "thread": "http-nio-8085-exec-1"
}
```

### **Error Log**

```json
{
  "timestamp": 1705737600000,
  "level": "ERROR",
  "controller": "UserController",
  "message": "Failed to create user",
  "metadata": {
    "exception": "ValidationException",
    "exceptionMessage": "Email is required",
    "stackTrace": "..."
  },
  "thread": "http-nio-8085-exec-1"
}
```

## 🔍 **Monitoring & Search**

### **CloudWatch Console**

1. Go to AWS CloudWatch Console
2. Navigate to Logs → Log groups
3. Find your application's log groups
4. Search and filter logs by:
   - Controller name
   - Log level
   - Time range
   - Custom metadata

### **Search Examples**

```
# Find all errors in UserController
{ $.level = "ERROR" && $.controller = "UserController" }

# Find slow API requests (>500ms)
{ $.metadata.responseTime > 500 }

# Find specific user actions
{ $.metadata.userId = "123" }

# Find specific endpoints
{ $.metadata.endpoint = "/users" }
```

## 📈 **Performance & Cost**

### **Optimizations**

- **Asynchronous Logging**: Logs are sent asynchronously to avoid blocking
- **Batch Processing**: Multiple logs can be batched together
- **Connection Pooling**: Efficient AWS client usage
- **Error Handling**: Graceful fallback if CloudWatch is unavailable

### **Cost Considerations**

- **Log Storage**: $0.50 per GB ingested
- **Log Analysis**: $0.005 per GB scanned
- **Data Transfer**: Free within same region
- **Retention**: Configure retention period (default: 7 days)

## 🛠️ **Troubleshooting**

### **Common Issues**

1. **Logs not appearing in CloudWatch**
   - Check AWS credentials and permissions
   - Verify region configuration
   - Check CloudWatch service status

2. **Permission errors**
   - Ensure IAM user has CloudWatch Logs permissions
   - Required permissions: `logs:CreateLogGroup`, `logs:CreateLogStream`, `logs:PutLogEvents`

3. **Performance issues**
   - Check network connectivity to AWS
   - Monitor CloudWatch service quotas
   - Review log volume and frequency

### **Debug Mode**

Enable debug logging for troubleshooting:

```properties
logging.level.com.example.service.CloudWatchLogService=DEBUG
logging.level.com.example.config.CloudWatchAppender=DEBUG
```

### **Health Check**

Test CloudWatch connectivity:

```bash
# Check if logs are being sent
curl -X POST http://localhost:8085/orders/cleanup-pending
# Check CloudWatch console for new log entries
```

## 🔐 **Security**

### **IAM Permissions**

Minimum required permissions for CloudWatch Logs:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents",
        "logs:DescribeLogGroups",
        "logs:DescribeLogStreams"
      ],
      "Resource": "arn:aws:logs:ap-south-1:*:log-group:/aws/application/Groceryol-ecommerce/*"
    }
  ]
}
```

### **Data Privacy**

- **No PII**: Sensitive data is not logged by default
- **Encryption**: Logs are encrypted in transit and at rest
- **Access Control**: Log access controlled by IAM policies

## 🚀 **Deployment**

### **Local Development**

1. Ensure AWS credentials are configured
2. Start the application
3. Check CloudWatch console for log groups

### **Production**

1. Use IAM roles instead of access keys
2. Configure appropriate log retention
3. Set up CloudWatch alarms for errors
4. Monitor log volume and costs

## 📚 **Additional Resources**

- [AWS CloudWatch Logs Documentation](https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/)
- [Spring Boot Logging](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging)
- [Logback Configuration](http://logback.qos.ch/manual/configuration.html)

## 🎉 **Benefits**

1. **Centralized Monitoring**: All logs in one place
2. **Real-time Visibility**: Immediate access to application logs
3. **Better Debugging**: Structured logs with context
4. **Performance Insights**: API response time tracking
5. **Cost Effective**: Pay-per-use pricing
6. **Scalable**: Handles high log volumes
7. **Searchable**: Powerful log search and filtering
8. **Integrations**: Works with other AWS services

Your application now has enterprise-grade logging with AWS CloudWatch! 🎯
