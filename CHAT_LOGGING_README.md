# Chat Logging System Implementation

## Overview

This document describes the comprehensive chat logging system implemented for the chat server. The system tracks all chat activities including user connections, conversations, messages, and system events.

## Features

### 1. **Session Logging**
- **User Connections**: Logs when users connect to the chat server
- **User Disconnections**: Logs when users disconnect or lose connection
- **Duplicate Login Attempts**: Tracks and warns about duplicate login attempts

### 2. **Conversation Logging**
- **Conversation Start**: Records when conversations begin between users
- **User Join**: Logs when users join existing conversations (e.g., managers)
- **User Leave**: Tracks when users leave conversations
- **Conversation End**: Records when conversations end with duration tracking

### 3. **Message Logging**
- **Chat Messages**: Logs all user messages with metadata
- **System Messages**: Tracks system-generated messages
- **Message IDs**: Assigns unique IDs to each message for tracking
- **Content Escaping**: Properly handles special characters in CSV format

### 4. **Request Logging**
- **Request Creation**: Logs when chat requests are created
- **Request Acceptance**: Records when requests are accepted
- **Request Cancellation**: Tracks cancelled requests with reasons

### 5. **System Event Logging**
- **Server Start/Stop**: Logs server lifecycle events
- **Error Tracking**: Comprehensive error logging with context
- **Warning Logging**: Tracks system warnings and issues

## File Structure

```
logs/
├── chat/                          # Chat-specific log directory
│   ├── messages.txt              # All chat messages with metadata
│   ├── conversations.txt         # Conversation lifecycle events
│   ├── requests.txt              # Chat request events
│   ├── sessions.txt              # User session events
│   └── chat.log                  # System log file (via Loggers)
└── [existing log files...]
```

## Log File Formats

### 1. **messages.txt**
```
timestamp,message_id,conversation_id,sender,receiver,message_type,content
2024-01-15 14:30:25,1,abc12345,john,ALL,CHAT,Hello everyone!
2024-01-15 14:30:30,2,abc12345,SYSTEM,ALL,SYSTEM,Manager joined
```

### 2. **conversations.txt**
```
timestamp,conversation_id,event_type,participants,details
2024-01-15 14:30:20,abc12345,STARTED,john,jane,Conversation started
2024-01-15 14:30:30,abc12345,JOINED,john,jane,User manager joined
2024-01-15 14:35:00,abc12345,ENDED,john,jane,Conversation ended (duration: 280s)
```

### 3. **requests.txt**
```
timestamp,request_id,requester,requester_branch,target_branch,event_type,details
2024-01-15 14:30:15,req12345,john,HOLON,TEL_AVIV,CREATED,Request created
2024-01-15 14:30:20,req12345,john,HOLON,TEL_AVIV,ACCEPTED,Accepted by jane (TEL_AVIV)
```

### 4. **sessions.txt**
```
timestamp,username,role,branch,event_type,details
2024-01-15 14:30:10,john,SALESPERSON,HOLON,CONNECTED,User connected to chat server
2024-01-15 14:35:00,john,SALESPERSON,HOLON,DISCONNECTED,User disconnected from chat server
```

## Implementation Details

### 1. **ChatLogger Class**
- **Location**: `src/server/util/ChatLogger.java`
- **Purpose**: Central logging service for all chat activities
- **Features**: Thread-safe logging with CSV file output

### 2. **Integration Points**
- **ChatServer**: Main server class with comprehensive logging
- **Session Management**: User connection/disconnection tracking
- **Conversation Handling**: Lifecycle event logging
- **Message Processing**: Content logging with metadata
- **Request Management**: Request lifecycle tracking

### 3. **Thread Safety**
- **ConcurrentHashMap**: Safe concurrent access to conversation timings
- **AtomicLong**: Thread-safe message ID generation
- **Synchronized Logging**: Safe file writing operations

## Setup Instructions

### 1. **Automatic Setup**
```bash
./setup-chat-logging.sh
```

### 2. **Manual Setup**
```bash
# Move ChatLogger to util directory
sudo mv ChatLogger.java src/server/util/

# Create chat logs directory
mkdir -p logs/chat
chmod 755 logs/chat
```

### 3. **Compilation**
```bash
./compile.sh
```

## Usage Examples

### 1. **Start the System**
```bash
# Terminal 1: Start main server
./run-server.sh

# Terminal 2: Start chat server
./run-chat.sh

# Terminal 3: Start client
./run-client.sh
```

### 2. **Monitor Logs**
```bash
# Watch chat messages in real-time
tail -f logs/chat/messages.txt

# Monitor conversation events
tail -f logs/chat/conversations.txt

# Track user sessions
tail -f logs/chat/sessions.txt
```

### 3. **Analyze Logs**
```bash
# Count total messages
wc -l logs/chat/messages.txt

# Find conversations by user
grep "john" logs/chat/conversations.txt

# Check server uptime
grep "SERVER_START\|SERVER_STOP" logs/chat/sessions.txt
```

## Benefits

### 1. **Audit Trail**
- Complete record of all chat activities
- User accountability and tracking
- Compliance and security requirements

### 2. **Debugging & Support**
- Detailed error logging with context
- User session tracking for troubleshooting
- Performance monitoring (conversation duration)

### 3. **Analytics & Reporting**
- Message volume analysis
- User activity patterns
- Conversation flow tracking
- Branch communication statistics

### 4. **Security & Compliance**
- User authentication tracking
- Access pattern monitoring
- Incident investigation support

## Performance Considerations

### 1. **File I/O Optimization**
- Buffered writing for better performance
- Asynchronous logging (can be enhanced)
- Log rotation for large files

### 2. **Memory Management**
- Minimal memory footprint
- Efficient data structures
- Proper cleanup of resources

### 3. **Scalability**
- Thread-safe implementation
- Concurrent user support
- Efficient CSV format for parsing

## Future Enhancements

### 1. **Advanced Features**
- Log compression and archiving
- Real-time log streaming
- Log search and filtering
- Performance metrics dashboard

### 2. **Integration**
- Database storage for long-term retention
- External log aggregation systems
- Alert system for critical events
- API for log access

### 3. **Monitoring**
- Log file size monitoring
- Disk space alerts
- Performance metrics collection
- Health check endpoints

## Troubleshooting

### 1. **Common Issues**
- **Permission Denied**: Check directory permissions
- **File Not Found**: Ensure ChatLogger.java is in correct location
- **Compilation Errors**: Verify all imports are correct

### 2. **Log File Issues**
- **Empty Logs**: Check file permissions and disk space
- **Corrupted Files**: Verify CSV format and encoding
- **Missing Events**: Check error handling in ChatLogger

### 3. **Performance Issues**
- **Slow Logging**: Monitor disk I/O performance
- **Large Files**: Implement log rotation
- **Memory Issues**: Check for memory leaks in logging

## Conclusion

The chat logging system provides comprehensive tracking of all chat activities, enabling better monitoring, debugging, and compliance. The implementation is designed to be efficient, thread-safe, and easily extensible for future requirements.

For questions or issues, refer to the code comments and this documentation.
