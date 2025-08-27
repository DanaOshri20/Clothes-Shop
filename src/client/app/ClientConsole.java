package client.app;

import server.domain.employees.EmployeeDirectory;
import server.domain.employees.PasswordPolicy;
import server.shared.Branch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Console client with numeric menus.
 * Connects to StoreServer (127.0.0.1:5050) and uses text protocol:
 *   LOGIN, LOGOUT, LIST, BUY, SELL, CUSTOMER_ADD, CUSTOMER_LIST
 * Also supports ChatServer (127.0.0.1:6060) for chat.
 */
public class ClientConsole {

    private static final String HOST = "127.0.0.1";
    private static final int STORE_PORT = 5050;
    private static final int CHAT_PORT  = 6060; // ChatServer should run here

    private final Scanner in = new Scanner(System.in);
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    private String loggedUsername = null;
    private String role = null;             // "admin" or "employee"
    private Branch employeeBranch = null;   // set for employees
    private String employeeRole = null;     // SALESPERSON/CASHIER/SHIFT_MANAGER (info only)

    public static void main(String[] args) {
        new ClientConsole().run();
    }

    public void run() {
        System.out.println("=== Shop Network Client ===");
        try {
            connectToStore();
            String welcome = reader.readLine(); // "OK WELCOME"
            if (welcome != null) System.out.println(welcome);

            if (!loginScreen()) {
                System.out.println("Login failed. Exiting.");
                return;
            }

            if ("admin".equals(role)) {
                adminMenuLoop();
            } else {
                employeeMenuLoop();
            }

        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        } finally {
            safeCloseStore();
        }
    }

    // -------------------- Store connection --------------------
    private void connectToStore() throws IOException {
        socket = new Socket(HOST, STORE_PORT);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
    }

    private void safeCloseStore() {
        try {
            if (writer != null) writer.flush();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) { }
    }

    // -------------------- Login (auto role detection) --------------------
    private boolean loginScreen() throws IOException {
        while (true) {
            System.out.println("\n=== Login ===");
            System.out.print("Username: ");
            String username = in.nextLine().trim();
            String password = readPassword("Password: ");

           
            role = ("admin".equalsIgnoreCase(username) && "admin".equals(password)) ? "admin" : "employee";

            writer.println("LOGIN " + username + " " + password + " " + role);
            String resp = reader.readLine();
            if (resp != null && resp.startsWith("OK LOGIN")) {
                loggedUsername = username;

                if ("employee".equals(role)) {
                    
                    EmployeeDirectory directory = new EmployeeDirectory();
                    EmployeeDirectory.EmployeeRecord rec = directory.findByUsername(username)
                            .orElseThrow(() -> new IllegalStateException("Employee not found in directory"));
                    employeeBranch = rec.branch();
                    employeeRole   = rec.role();
                    System.out.println("Login successful as " + prettyEmployeeRole(employeeRole));
                    System.out.println("Your branch: " + employeeBranch);
                } else {
                    System.out.println("Login successful as ADMIN");
                }
                return true;
            } else {
                System.out.println(resp == null ? "Login failed." : resp);
                
                // Check for specific error types
                if (resp != null && resp.equals("ERR LOGIN ALREADY_CONNECTED")) {
                    System.out.println("Already connected. Please logout from other sessions first.");
                } else {
                    System.out.println("Invalid credentials. Please try again.");
                }
                
                // Ask if user wants to try again
                System.out.print("Try again? (y/n): ");
                String retry = in.nextLine().trim().toLowerCase();
                if (!retry.equals("y") && !retry.equals("yes")) {
                    System.out.println("Login cancelled by user.");
                    return false;
                }
                // Continue loop to try again
            }
        }
    }

    // Password visible (no masking)
    private String readPassword(String prompt) {
        System.out.print(prompt);
        return in.nextLine();
    }

    // -------------------- Admin Menu --------------------
    private void adminMenuLoop() throws IOException {
        EmployeeDirectory directory = new EmployeeDirectory();

        while (true) {
            System.out.println("\n-- Admin Menu --");
            System.out.println("1) List inventory (choose branch)");
            System.out.println("2) Add employee");
            System.out.println("3) Set password policy");
            System.out.println("4) List employees");
            System.out.println("5) Delete employee by ID");
            System.out.println("0) Logout");
            System.out.print("Choice: ");
            String c = in.nextLine().trim();

            switch (c) {
                case "0":
                    logout();
                    return;
                case "1":
                    Branch branch = askBranch();
                    doList(branch);
                    break;
                case "2":
                    addEmployeeFlow(directory);
                    break;
                case "3":
                    setPasswordPolicyFlow();
                    break;
                case "4":
                    listEmployeesFlow(directory);
                    break;
                case "5":
                    deleteEmployeeFlow(directory);
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private void addEmployeeFlow(EmployeeDirectory directory) {
        try {
            System.out.println("\n=== Add Employee ===");
            System.out.print("Username: ");
            String username = in.nextLine().trim();

            String password = readPassword("Password: ");
            String confirm  = readPassword("Confirm password: ");
            if (!password.equals(confirm)) {
                System.out.println("Passwords do not match.");
                return;
            }

            String roleCode = askRole();     // SALESPERSON/CASHIER/SHIFT_MANAGER
            Branch branch = askBranch();     // HOLON/TEL_AVIV/RISHON

            System.out.print("Account number: ");
            String accountNumber = in.nextLine().trim();
            System.out.print("Phone: ");
            String phone = in.nextLine().trim();

            EmployeeDirectory.EmployeeRecord rec =
                    directory.addEmployee(username, password, roleCode, branch, accountNumber, phone);
            System.out.println("Employee added. ID=" + rec.employeeId() + ", Username=" + rec.username()
                    + ", Role=" + rec.role() + ", Branch=" + rec.branch());

        } catch (IllegalArgumentException ex) {
            System.out.println("Failed: " + ex.getMessage());
            showPolicyHintIfRelevant(ex.getMessage());
        } catch (Exception e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }

    private void deleteEmployeeFlow(EmployeeDirectory directory) {
        System.out.println("\n=== Delete Employee ===");
        System.out.print("Employee ID to delete: ");
        String id = in.nextLine().trim();

        java.util.Optional<EmployeeDirectory.EmployeeRecord> recOpt = directory.findById(id);
        if (!recOpt.isPresent()) {
            System.out.println("Employee not found: " + id);
            return;
        }
        EmployeeDirectory.EmployeeRecord r = recOpt.get();
                displayEmployeeDeleteConfirmation(r);
        if (!askYesNo("Are you sure? (y/n): ")) {
            System.out.println("Cancelled.");
            return;
        }
        boolean ok = directory.deleteById(id);
        System.out.println(ok ? "Employee deleted." : "Delete failed.");
    }

    private void setPasswordPolicyFlow() {
        System.out.println("\n=== Password Policy ===");
        displayPasswordPolicy();

        int min = askPositiveInt("New minimum length (>=1): ");
        boolean needDigit  = askYesNo("Require digit? (y/n): ");
        boolean needLetter = askYesNo("Require letter? (y/n): ");

        PasswordPolicy.configure(min, needDigit, needLetter);
        System.out.println("Policy updated and saved.");
    }

    private void listEmployeesFlow(EmployeeDirectory directory) {
        displayEmployeesTable(directory.listAll());
    }
    
    private void displayEmployeesTable(java.util.List<EmployeeDirectory.EmployeeRecord> employees) {
        if (employees.isEmpty()) {
            System.out.println("No employees found.");
            return;
        }
        
        System.out.println("\n" + "=".repeat(85));
        System.out.println("                              EMPLOYEE DIRECTORY");
        System.out.println("=".repeat(85));
        
        // Print table header
        System.out.printf("%-5s %-15s %-18s %-12s %-15s %-15s%n", 
            "ID", "Username", "Role", "Branch", "Account", "Phone");
        System.out.println("-".repeat(85));
        
        // Print each employee
        for (EmployeeDirectory.EmployeeRecord r : employees) {
            String role = prettyEmployeeRole(r.role());
            System.out.printf("%-5s %-15s %-18s %-12s %-15s %-15s%n",
                r.employeeId(), r.username(), role, r.branch(), r.accountNumber(), r.phone());
        }
        
        System.out.println("-".repeat(85));
        System.out.println("Total employees: " + employees.size());
        System.out.println("=".repeat(85) + "\n");
    }

    private void showPolicyHintIfRelevant(String msg) {
        if (msg != null && msg.toLowerCase().contains("password")) {
            displayPasswordPolicy();
        }
    }

    private String askRole() {
        while (true) {
            System.out.println("Select role:");
            System.out.println("1) SALESPERSON");
            System.out.println("2) CASHIER");
            System.out.println("3) SHIFT_MANAGER");
            System.out.print("Choice: ");
            String c = in.nextLine().trim();
            if ("1".equals(c)) return "SALESPERSON";
            if ("2".equals(c)) return "CASHIER";
            if ("3".equals(c)) return "SHIFT_MANAGER";
            System.out.println("Invalid choice.");
        }
    }

    // -------------------- Employee Menu --------------------
    private void employeeMenuLoop() throws IOException {
        while (true) {
            System.out.println("\n-- Employee Menu (" + loggedUsername + ", " + employeeBranch + ", " + prettyEmployeeRole(employeeRole) + ") --");
            System.out.println("1) List inventory (my branch)");
            System.out.println("2) Stock management");
            System.out.println("3) Customers");
            System.out.println("4) Chat");
            System.out.println("0) Logout");
            System.out.print("Choice: ");
            String c = in.nextLine().trim();

            if ("0".equals(c)) {
                logout();
                return;
            } else if ("1".equals(c)) {
                doList(employeeBranch);
            } else if ("2".equals(c)) {
                stockManagementMenu();
            } else if ("3".equals(c)) {
                customersMenu();
            } else if ("4".equals(c)) {
                startChatClient();
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }

    private void stockManagementMenu() throws IOException {
        while (true) {
            System.out.println("\n-- Stock Management --");
            System.out.println("1) Sell product to customer");
            System.out.println("2) Order product to branch");
            System.out.println("3) Add new product to inventory");
            System.out.println("4) Remove product from stock");
            System.out.println("0) Back");
            System.out.print("Choice: ");
            String c = in.nextLine().trim();
            if ("0".equals(c)) return;
            else if ("1".equals(c)) doSell(employeeBranch);
            else if ("2".equals(c)) doBuy(employeeBranch);
            else if ("3".equals(c)) doAddProduct(employeeBranch);
            else if ("4".equals(c)) doRemoveProduct(employeeBranch);
            else System.out.println("Invalid choice.");
        }
    }

    // -------------------- Customers submenu --------------------
    private void customersMenu() throws IOException {
        while (true) {
            System.out.println("\n-- Customers --");
            System.out.println("1) Add customer");
            System.out.println("2) List customers");
            System.out.println("0) Back");
            System.out.print("Choice: ");
            String c = in.nextLine().trim();
            if ("0".equals(c)) return;
            else if ("1".equals(c)) doAddCustomer();
            else if ("2".equals(c)) doListCustomers();
            else System.out.println("Invalid choice.");
        }
    }

    private void doAddCustomer() throws IOException {
        System.out.println("\n=== Add Customer ===");
        System.out.print("ID (national ID): ");
        String id = in.nextLine().trim();

        System.out.print("Full name: ");
        String fullName = in.nextLine().trim();

        System.out.print("Phone: ");
        String phone = in.nextLine().trim();

        String type = "NEW"; // Default to NEW customer type

        String nameToken = fullName.replace(' ', '_');
        writer.println("CUSTOMER_ADD " + id + " " + nameToken + " " + phone + " " + type);
        String resp = reader.readLine();
        System.out.println(resp == null ? "No response" : resp.replace('_', ' '));
    }

    private void doListCustomers() throws IOException {
        writer.println("CUSTOMER_LIST");
        String line;
        
        // Collect all customer data first
        java.util.List<String> customers = new java.util.ArrayList<>();
        while ((line = reader.readLine()) != null) {
            if ("OK END".equals(line)) break;
            if (line.startsWith("CUST ")) {
                customers.add(line.substring(5));
            }
        }
        
        if (customers.isEmpty()) {
            System.out.println("No customers found.");
            return;
        }
        
        // Display as a formatted table
        displayCustomersTable(customers);
    }
    
    private void displayCustomersTable(java.util.List<String> customers) {
        System.out.println("\n" + "=".repeat(75));
        System.out.println("                              CUSTOMER DIRECTORY");
        System.out.println("=".repeat(75));
        
        // Print table header
        System.out.printf("%-15s %-25s %-15s %-12s%n", 
            "ID", "Full Name", "Phone", "Type");
        System.out.println("-".repeat(75));
        
        // Print each customer
        for (String customer : customers) {
            String[] parts = customer.split(",");
            if (parts.length >= 4) {
                String id = parts[0];
                String fullName = parts[1];
                String phone = parts[2];
                String type = parts[3];
                
                System.out.printf("%-15s %-25s %-15s %-12s%n",
                    id, fullName, phone, type);
            }
        }
        
        System.out.println("-".repeat(75));
        System.out.println("Total customers: " + customers.size());
        System.out.println("=".repeat(75) + "\n");
    }
    
    private void displaySaleSummary(String customerType, String basePrice, String discount, String finalPrice) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("                    SALE SUMMARY");
        System.out.println("=".repeat(50));
        
        System.out.printf("%-20s %s%n", "Customer type:", customerType);
        System.out.printf("%-20s $%s%n", "Base price:", basePrice);
        System.out.printf("%-20s $%s%n", "Discount:", discount);
        System.out.printf("%-20s $%s%n", "Final price:", finalPrice);
        
        System.out.println("=".repeat(50) + "\n");
    }
    
    private void displayEmployeeDeleteConfirmation(EmployeeDirectory.EmployeeRecord r) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("                    EMPLOYEE DELETE CONFIRMATION");
        System.out.println("=".repeat(60));
        
        System.out.printf("%-20s %s%n", "ID:", r.employeeId());
        System.out.printf("%-20s %s%n", "Username:", r.username());
        System.out.printf("%-20s %s%n", "Role:", prettyEmployeeRole(r.role()));
        System.out.printf("%-20s %s%n", "Branch:", r.branch());
        
        System.out.println("=".repeat(60));
    }
    
    private void displayPasswordPolicy() {
        System.out.println("\n" + "=".repeat(55));
        System.out.println("                    CURRENT PASSWORD POLICY");
        System.out.println("=".repeat(55));
        
        System.out.printf("%-25s %s%n", "Minimum length:", PasswordPolicy.minimumLength());
        System.out.printf("%-25s %s%n", "Require digit:", PasswordPolicy.requireDigit());
        System.out.printf("%-25s %s%n", "Require letter:", PasswordPolicy.requireLetter());
        
        System.out.println("=".repeat(55));
    }

    private String askCustomerType() {
        while (true) {
            System.out.println("Customer type (default NEW):");
            System.out.println("1) NEW");
            System.out.println("2) RETURNING");
            System.out.println("3) VIP");
            System.out.print("Choice: ");
            String c = in.nextLine().trim();
            if (c.isEmpty() || "1".equals(c)) return "NEW";
            if ("2".equals(c)) return "RETURNING";
            if ("3".equals(c)) return "VIP";
            System.out.println("Invalid choice.");
        }
    }

    // -------------------- Chat client --------------------
    // Menu minimal: request by branch, accept incoming; manager options at the end
    private void startChatClient() {
        System.out.println("\n=== Chat ===");
        System.out.println("Connecting to chat server on port " + CHAT_PORT + " ...");

        try (Socket chat = new Socket(HOST, CHAT_PORT);
             BufferedReader chatIn = new BufferedReader(new InputStreamReader(chat.getInputStream()));
             PrintWriter chatOut = new PrintWriter(new OutputStreamWriter(chat.getOutputStream()), true)) {

            // Identify to chat server
            String myRole = (employeeRole == null ? "SALESPERSON" : employeeRole);
            String myBranch = (employeeBranch == null ? Branch.HOLON.name() : employeeBranch.name());
            chatOut.println("HELLO " + loggedUsername + " " + myRole + " " + myBranch);
            String hello = chatIn.readLine();
            if (hello == null || !hello.startsWith("OK HELLO")) {
                System.out.println(hello == null ? "No response" : hello);
                return;
            }

            // Track incoming requests (id -> "user@branch") and pairing state
            final Map<String, String> incoming = new ConcurrentHashMap<String, String>();
            final AtomicBoolean paired = new AtomicBoolean(false);

            // Reader thread: prints events, updates incoming & paired flag
            Thread eventThread = new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        String line;
                        while ((line = chatIn.readLine()) != null) {
                            if (line.startsWith("INCOMING_REQUEST ")) {
                                // INCOMING_REQUEST <id> <fromUser> <fromBranch>
                                String[] p = line.split(" ");
                                if (p.length >= 4) {
                                    String id = p[1];
                                    String fromUser = p[2];
                                    String fromBranch = p[3];
                                    incoming.put(id, fromUser + "@" + fromBranch);
                                }
                            } else if (line.startsWith("REQUEST_TAKEN ") || line.startsWith("REQUEST_CANCELLED ")) {
                                String[] p = line.split(" ");
                                if (p.length >= 2) incoming.remove(p[1]);
                            } else if (line.startsWith("PAIRED ")) {
                                paired.set(true);
                            } else if (line.startsWith("INFO LEFT_CONVERSATION") || line.startsWith("INFO CONVERSATION_ENDED")) {
                                paired.set(false);
                            }
                            System.out.println("[CHAT] " + line);
                        }
                    } catch (IOException ignored) { }
                }
            });
            eventThread.setDaemon(true);
            eventThread.start();

            boolean isManager = "SHIFT_MANAGER".equalsIgnoreCase(myRole);

            // Chat main menu
            while (true) {
                
                if (paired.getAndSet(false)) {
                    System.out.println("(Paired! Entering live chat...)");
                    chatChatLoop(chatOut);
                    continue; 
                }

                System.out.println("\n-- Chat Menu --");
                System.out.println("1) Request: employee by branch");
                System.out.println("2) Show INCOMING requests (" + incoming.size() + ") and ACCEPT");
                if (isManager) {
                    System.out.println("3) List active conversations");
                    System.out.println("4) Join conversation (Shift Manager)");
                }
                System.out.println("0) Back");
                System.out.print("Choice: ");
                String c = in.nextLine().trim();

                if ("0".equals(c)) {
                    chatOut.println("QUIT");
                    break;

                } else if ("1".equals(c)) {
                    Branch b = askBranch();
                    paired.set(false); 
                    chatOut.println("REQUEST_BRANCH " + b.name());
                    System.out.println("(Broadcast to branch " + b + " sent; waiting for someone to ACCEPT...)");
                    if (waitForPairing(paired, 15000)) {
                        chatChatLoop(chatOut);
                    } else {
                        System.out.println("No one accepted yet. Try again later or check incoming requests.");
                    }

                } else if ("2".equals(c)) {
                    if (incoming.isEmpty()) {
                        System.out.println("No incoming requests at the moment.");
                    } else {
                        System.out.println("Incoming requests:");
                        for (Map.Entry<String,String> e : incoming.entrySet()) {
                            System.out.println("  " + e.getKey() + " from " + e.getValue());
                        }
                        System.out.print("Enter request ID to ACCEPT (or blank to cancel): ");
                        String id = in.nextLine().trim();
                        if (!id.isEmpty() && incoming.containsKey(id)) {
                            paired.set(false);
                            chatOut.println("ACCEPT " + id);
                            if (waitForPairing(paired, 10000)) {
                                chatChatLoop(chatOut);
                            } else {
                                System.out.println("Not paired yet (request may be taken/cancelled).");
                            }
                        }
                    }

                } else if (isManager && "3".equals(c)) {
                    chatOut.println("LIST_CONVS"); 
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}

                } else if (isManager && "4".equals(c)) {
                    System.out.print("Conversation ID: ");
                    String id = in.nextLine().trim();
                    if (!id.isEmpty()) {
                        paired.set(false);
                        chatOut.println("JOIN " + id);
                        if (waitForPairing(paired, 5000)) {
                            chatChatLoop(chatOut);
                        } else {
                            System.out.println("Join failed or not paired.");
                        }
                    }

                } else {
                    System.out.println("Invalid choice.");
                }
            }

        } catch (IOException e) {
            System.out.println("Chat error: " + e.getMessage());
        }
    }

    /** Wait (up to timeoutMs) until the event thread signals PAIRED */
    private boolean waitForPairing(AtomicBoolean paired, long timeoutMs) {
        long start = System.currentTimeMillis();
        while (!paired.get() && System.currentTimeMillis() - start < timeoutMs) {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
        return paired.get();
    }

    /** Live chat loop during an active conversation */
    private void chatChatLoop(PrintWriter chatOut) {
        System.out.println("Type your messages. Use '/end' to leave the conversation, '/quit' to disconnect from chat server.");
        while (true) {
            String msg = in.nextLine();
            if (msg.equalsIgnoreCase("/end")) { chatOut.println("END"); break; }
            if (msg.equalsIgnoreCase("/quit")) { chatOut.println("QUIT"); break; }
            chatOut.println("MSG " + msg);
        }
    }

    // -------------------- Store actions --------------------
    private void doList(Branch branch) throws IOException {
        writer.println("LIST " + branch.name());
        String line;
        
        // Collect all inventory items first
        java.util.List<String> items = new java.util.ArrayList<>();
        while ((line = reader.readLine()) != null) {
            if ("OK END".equals(line)) break;
            if (line.startsWith("ITEM ")) {
                items.add(line.substring(5));
            }
        }
        
        if (items.isEmpty()) {
            System.out.println("No inventory items found for branch: " + branch.name());
            return;
        }
        
        // Display as a formatted table
        displayInventoryTable(items, branch.name());
    }
    
    private void displayInventoryTable(java.util.List<String> items, String branchName) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("                    INVENTORY - " + branchName + " BRANCH");
        System.out.println("=".repeat(60));
        
        // Print table header (removed Branch column)
        System.out.printf("%-8s %-15s %-10s %-12s%n", 
            "ID", "Category", "Quantity", "Price");
        System.out.println("-".repeat(60));
        
        // Print each item (removed Branch column)
        for (String item : items) {
            String[] parts = item.split(",");
            if (parts.length >= 5) {
                String id = parts[0];
                String category = parts[1];
                // String branch = parts[2]; // Still parsed but not displayed
                String quantity = parts[3];
                String price = parts[4];
                
                System.out.printf("%-8s %-15s %-10s %-12s%n",
                    id, category, quantity, price + "$");
            }
        }
        
        System.out.println("-".repeat(60));
        System.out.println("Total items: " + items.size());
        System.out.println("=".repeat(60) + "\n");
    }

    private void doSell(Branch branch) throws IOException {
        System.out.print("Enter ID: ");
        String sku = in.nextLine().trim();
        int quantity = askPositiveInt("Enter quantity: ");
        System.out.print("Enter customer ID: ");
        String customerId = in.nextLine().trim();

        writer.println("SELL " + branch.name() + " " + sku + " " + quantity + " " + customerId);
        String resp = reader.readLine();
        if (resp != null && resp.startsWith("OK SALE ")) {
            String[] parts = resp.split(" ");
            if (parts.length >= 6) {
                displaySaleSummary(parts[5], parts[2], parts[3], parts[4]);
            } else System.out.println(resp);
        } else System.out.println(resp == null ? "No response" : resp);
    }

    private void doBuy(Branch branch) throws IOException {
        System.out.print("Enter ID: ");
        String sku = in.nextLine().trim();
        int quantityToAdd = askPositiveInt("Enter quantity to add: ");
        writer.println("BUY " + branch.name() + " " + sku + " " + quantityToAdd);
        String resp = reader.readLine();
        System.out.println(resp == null ? "No response" : resp);
    }

    private void doAddProduct(Branch branch) throws IOException {
        System.out.print("Enter new category (non-existing allowed): ");
        String category = in.nextLine().trim();
        int quantity = askPositiveInt("Enter initial quantity: ");
        System.out.print("Enter price: ");
        String price = in.nextLine().trim();
        if (category.isEmpty() || price.isEmpty()) {
            System.out.println("Category and price are required.");
            return;
        }
        writer.println("ADD_PRODUCT " + branch.name() + " " + category.replace(' ', '_') + " " + quantity + " " + price);
        String resp = reader.readLine();
        System.out.println(resp == null ? "No response" : resp.replace('_', ' '));
    }

    private void doRemoveProduct(Branch branch) throws IOException {
        System.out.print("Enter ID: ");
        String sku = in.nextLine().trim();
        if (sku.isEmpty()) { System.out.println("ID is required."); return; }
        if (!askYesNo("Are you sure you want to remove this product from stock? (y/n): ")) {
            System.out.println("Cancelled.");
            return;
        }
        writer.println("REMOVE_PRODUCT " + branch.name() + " " + sku);
        String resp = reader.readLine();
        System.out.println(resp == null ? "No response" : resp);
    }

    private void logout() throws IOException {
        writer.println("LOGOUT");
        String bye = reader.readLine();
        if (bye != null) System.out.println(bye);
    }

    // -------------------- Helpers --------------------
    private Branch askBranch() {
        while (true) {
            System.out.println("Select branch:");
            System.out.println("1) HOLON");
            System.out.println("2) TEL_AVIV");
            System.out.println("3) RISHON");
            System.out.print("Choice: ");
            String c = in.nextLine().trim();
            if ("1".equals(c)) return Branch.HOLON;
            if ("2".equals(c)) return Branch.TEL_AVIV;
            if ("3".equals(c)) return Branch.RISHON;
            System.out.println("Invalid choice.");
        }
    }

    private int askPositiveInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = in.nextLine().trim();
            try {
                int v = Integer.parseInt(s);
                if (v > 0) return v;
            } catch (NumberFormatException ignored) { }
            System.out.println("Please enter a positive integer.");
        }
    }

    private boolean askYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = in.nextLine().trim().toLowerCase();
            if (s.equals("y") || s.equals("yes")) return true;
            if (s.equals("n") || s.equals("no")) return false;
            System.out.println("Please answer y/n.");
        }
    }

    private String prettyEmployeeRole(String roleCode) {
        if (roleCode == null) return "Employee";
        switch (roleCode.toUpperCase()) {
            case "SHIFT_MANAGER": return "Shift Manager";
            case "CASHIER":       return "Cashier";
            case "SALESPERSON":   return "Salesperson";
            default:              return "Employee";
        }
    }
}
