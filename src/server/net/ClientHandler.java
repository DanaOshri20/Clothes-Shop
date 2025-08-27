package server.net;

import server.domain.employees.AuthService;
import server.domain.invantory.InventoryService;
import server.domain.customers.CustomerService;
import server.domain.sales.SalesService;

import server.shared.Branch;
import server.domain.invantory.Product;
import server.domain.customers.Customer;

import server.util.Loggers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final AuthService auth;
    private final InventoryService inventory;
    private final CustomerService customers;
    private final SalesService sales;

    private String loggedUsername = null;

    public ClientHandler(Socket socket, AuthService auth, InventoryService inventory,
                         CustomerService customers, SalesService sales) {
        this.socket = socket;
        this.auth = auth;
        this.inventory = inventory;
        this.customers = customers;
        this.sales = sales;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {

            out.println("OK WELCOME");
            String line;

            while ((line = in.readLine()) != null) {
                String[] t = line.trim().split(" ");
                if (t.length == 0) continue;
                String cmd = t[0].toUpperCase();

                if ("LOGIN".equals(cmd)) { // LOGIN <username> <password> <role: employee|admin>
                    if (t.length < 4) { out.println("ERR BAD_ARGS"); continue; }
                    AuthService.LoginResult result = "admin".equalsIgnoreCase(t[3])
                            ? auth.loginAdmin(t[1], t[2])
                            : auth.loginEmployee(t[1], t[2]);
                    
                    if (result == AuthService.LoginResult.SUCCESS) { 
                        loggedUsername = t[1]; 
                        out.println("OK LOGIN"); 
                    } else if (result == AuthService.LoginResult.ALREADY_CONNECTED) {
                        out.println("ERR LOGIN ALREADY_CONNECTED");
                    } else {
                        out.println("ERR LOGIN INVALID_CREDENTIALS");
                    }
                }
                else if ("LOGOUT".equals(cmd)) {
                    if (loggedUsername != null) auth.logout(loggedUsername);
                    out.println("OK BYE");
                    return;
                }
                else if ("LIST".equals(cmd)) { // LIST <branch>
                    if (t.length < 2) { out.println("ERR BAD_ARGS"); continue; }
                    Branch branch = Branch.valueOf(t[1].toUpperCase());
                    for (Product p : inventory.listByBranch(branch)) {
                        out.println("ITEM " + p.sku() + "," + p.category() + ","
                                + p.branch() + "," + p.quantity() + "," + p.price());
                    }
                    out.println("OK END");
                }
                else if ("BUY".equals(cmd)) { // BUY <branch> <sku> <quantity>
                    if (t.length < 4) { out.println("ERR BAD_ARGS"); continue; }
                    Branch branch = Branch.valueOf(t[1].toUpperCase());
                    String sku = t[2];
                    int quantity = Integer.parseInt(t[3]);
                    inventory.updateQuantity(branch, sku, +quantity);
                    out.println("OK BUY");
                }
                else if ("SELL".equals(cmd)) { // SELL <branch> <sku> <quantity> <customerId>
                    if (t.length < 5) { out.println("ERR BAD_ARGS"); continue; }
                    Branch branch = Branch.valueOf(t[1].toUpperCase());
                    String sku = t[2];
                    int quantity = Integer.parseInt(t[3]);
                    String customerId = t[4];

                    Product product = inventory.findProduct(branch, sku)
                            .orElseThrow(() -> new IllegalStateException("SKU not found in branch"));
                    Customer customer = customers.findById(customerId)
                            .orElseThrow(() -> new IllegalStateException("Customer not found"));

                    if (product.quantity() < quantity) {
                        out.println("ERR NOT_ENOUGH_STOCK");
                        continue;
                    }

                    // Discount based on current type (before promotion)
                    SalesService.SaleSummary summary = sales.sell(product, quantity, customer);
                    inventory.updateQuantity(branch, sku, -quantity);

                    // Record purchase and auto-promote for next time
                    customers.recordPurchase(customerId);

                    out.println("OK SALE " +
                            summary.basePrice() + " " +
                            summary.discountValue() + " " +
                            summary.finalPrice() + " " +
                            summary.customerType());
                }
                else if ("CUSTOMER_ADD".equals(cmd)) { // CUSTOMER_ADD <id> <fullName_underscored> <phone> [type]
                    if (t.length < 4) { out.println("ERR BAD_ARGS"); continue; }
                    String id = t[1];
                    String fullName = t[2].replace('_', ' ');
                    String phone = t[3];
                    String type = (t.length >= 5) ? t[4].toUpperCase() : "NEW";
                    try {
                        customers.addCustomer(id, fullName, phone, type);
                        out.println("OK CUSTOMER_ADDED");
                    } catch (Exception ex) {
                        out.println("ERR " + ex.getMessage().replace(' ', '_'));
                    }
                }
                else if ("CUSTOMER_LIST".equals(cmd)) { // returns CUST lines
                    for (Customer c : customers.listAll()) {
                        out.println("CUST " + c.id() + "," + c.fullName() + "," + c.phone() + "," + c.type().code());
                    }
                    out.println("OK END");
                }
                else if ("ADD_PRODUCT".equals(cmd)) { // ADD_PRODUCT <branch> <category> <quantity> <price>
                    if (t.length < 5) { out.println("ERR BAD_ARGS"); continue; }
                    Branch branch = Branch.valueOf(t[1].toUpperCase());
                    String category = t[2].replace('_', ' ');
                    int quantity = Integer.parseInt(t[3]);
                    java.math.BigDecimal price = new java.math.BigDecimal(t[4]);
                    try {
                        String newSku = inventory.addNewProduct(branch, category, quantity, price);
                        out.println("OK PRODUCT_ADDED " + newSku + " " + category.replace(' ', '_'));
                    } catch (Exception ex) {
                        out.println("ERR " + ex.getMessage().replace(' ', '_'));
                    }
                }
                else if ("REMOVE_PRODUCT".equals(cmd)) { // REMOVE_PRODUCT <branch> <sku>
                    if (t.length < 3) { out.println("ERR BAD_ARGS"); continue; }
                    Branch branch = Branch.valueOf(t[1].toUpperCase());
                    String sku = t[2];
                    try {
                        boolean removed = inventory.removeProduct(branch, sku);
                        if (removed) out.println("OK REMOVED");
                        else out.println("ERR SKU_NOT_FOUND");
                    } catch (Exception ex) {
                        out.println("ERR " + ex.getMessage().replace(' ', '_'));
                    }
                }
                else {
                    out.println("ERR UNKNOWN_CMD");
                }
            }
        } catch (Exception e) {
            Loggers.system().severe("Client error: " + e);
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            if (loggedUsername != null) auth.logout(loggedUsername);
        }
    }
}
