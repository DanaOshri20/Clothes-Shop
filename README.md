# FinalProject - Shop Network System

## Overview
This project is a multi-tier client-server application simulating a shop network with multiple branches. It allows employees to manage inventory, customers, sales, and communicate via a real-time chat system.

## Features
- Employee and customer management
- Inventory and sales processing
- Role-based access (Admin, Salesperson, Cashier, Shift Manager)
- Real-time inter-branch chat
- File-based data storage (CSV)
- Comprehensive logging (system, auth, chat, transactions)

## Directory Structure
```
FinalProject/
├── src/            # Source code (client & server)
├── data/           # Data files (CSV)
├── logs/           # Log files
├── out/            # Compiled classes
├── *.sh            # Shell scripts to run/compile
├── PROJECT_DOCUMENTATION.md  # Full technical documentation
└── README.md       # This file
```

## Quick Start
1. **Compile the project:**
   ```bash
   ./compile.sh
   ```
2. **Start the main server:**
   ```bash
   ./run-server.sh
   ```
3. **Start the chat server:**
   ```bash
   ./run-chat.sh
   ```
4. **Start a client:**
   ```bash
   ./run-client.sh
   ```

## Data Files
- `data/employees.txt` - Employee records
- `data/products.txt` - Product inventory
- `data/customers.txt` - Customer records
- `data/sales.txt` - Sales history
- `data/password_policy.txt` - Password rules

## Logs
- `logs/system.log` - System events
- `logs/auth.log` - Authentication events
- `logs/chat/` - Chat messages and events
- `logs/transactions.log` - Sales transactions

## Documentation
- For a detailed explanation of the system architecture, class diagrams, and code examples, see [PROJECT_DOCUMENTATION.md](./PROJECT_DOCUMENTATION.md).

## Requirements
- Java 8 or higher
- Unix/Mac shell (for `.sh` scripts)

## Authors
- [Your Name Here]

---
*For more details, see the full documentation or contact the project maintainer.*
# Clothes-Shop
