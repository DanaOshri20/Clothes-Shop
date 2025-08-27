#!/bin/bash

echo "=== Setting up Chat Logging System ==="

# Check if we can write to the util directory
if [ -w "src/server/util/" ]; then
    echo "Moving ChatLogger.java to src/server/util/"
    mv ChatLogger.java src/server/util/
else
    echo "Cannot write to src/server/util/ directory"
    echo "Please manually move ChatLogger.java to src/server/util/"
    echo "You can do this by:"
    echo "1. Opening Finder"
    echo "2. Navigating to the project directory"
    echo "3. Moving ChatLogger.java to src/server/util/"
    echo "4. Or run: sudo mv ChatLogger.java src/server/util/"
fi

# Create chat logs directory
echo "Creating chat logs directory..."
mkdir -p logs/chat

# Set permissions for logs directory
echo "Setting permissions for logs directory..."
chmod 755 logs/chat

echo "=== Chat Logging Setup Complete ==="
echo ""
echo "The chat logging system will now track:"
echo "- User connections and disconnections"
echo "- Conversation lifecycle (start, join, leave, end)"
echo "- All chat messages with metadata"
echo "- Chat requests and responses"
echo "- System events and errors"
echo ""
echo "Log files will be created in logs/chat/:"
echo "- messages.txt: All chat messages"
echo "- conversations.txt: Conversation events"
echo "- requests.txt: Chat request events"
echo "- sessions.txt: User session events"
echo "- chat.log: System log file"
echo ""
echo "To test the system:"
echo "1. Compile: ./compile.sh"
echo "2. Start server: ./run-server.sh"
echo "3. Start chat server: ./run-chat.sh"
echo "4. Connect clients and start chatting"
echo "5. Check logs/chat/ directory for detailed logs"
