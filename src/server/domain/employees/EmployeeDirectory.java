package server.domain.employees;

import server.shared.Branch;
import server.util.FileDatabase;
import server.util.Loggers;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * EmployeeDirectory
 * File: data/employees.txt
 * Format: employeeId,username,hash,role,branch,accountNumber,phone
 * Roles: SALESPERSON | CASHIER | SHIFT_MANAGER
 */
public class EmployeeDirectory {

    public static final Set<String> ROLES = new HashSet<>(
            Arrays.asList("SALESPERSON", "CASHIER", "SHIFT_MANAGER"));

    public static record EmployeeRecord(
            String employeeId, String username, String role, Branch branch,
            String accountNumber, String phone) {}

    private final FileDatabase db = new FileDatabase(Path.of("data/employees.txt"));

    public Optional<EmployeeRecord> findByUsername(String username) {
        return listAll().stream().filter(r -> r.username().equals(username)).findFirst();
    }

    /** Find by employee numeric ID (string). */
    public Optional<EmployeeRecord> findById(String employeeId) {
        return listAll().stream().filter(r -> r.employeeId().equals(employeeId)).findFirst();
    }

    public List<EmployeeRecord> listAll() {
        List<String> lines = db.readAllLines();
        return lines.stream()
                .filter(s -> s != null && !s.trim().isEmpty() && !s.startsWith("#"))
                .map(this::parse)
                .collect(Collectors.toList());
    }

    public boolean usernameExists(String username) {
        return findByUsername(username).isPresent();
    }

    /** Add a new employee (validates role & password policy, ensures unique username). */
    public EmployeeRecord addEmployee(String username,
                                      String plainPassword,
                                      String role,
                                      Branch branch,
                                      String accountNumber,
                                      String phone) {
        if (username == null || username.trim().isEmpty())
            throw new IllegalArgumentException("Username is required");
        if (usernameExists(username))
            throw new IllegalArgumentException("Username already exists");
        String upperRole = role == null ? "" : role.trim().toUpperCase();
        if (!ROLES.contains(upperRole))
            throw new IllegalArgumentException("Role must be one of: " + ROLES);
        if (!PasswordPolicy.validate(plainPassword))
            throw new IllegalArgumentException("Password does not meet the policy");

        String id = nextEmployeeId();
        String hash = AuthService.sha256(plainPassword);
        String line = String.join(",",
                id,
                username,
                hash,
                upperRole,
                branch.name(),
                accountNumber == null ? "" : accountNumber,
                phone == null ? "" : phone
        );
        db.appendLine(line);
        
        // Log the employee addition
        Loggers.employees().info(String.format("EMPLOYEE_ADDED: ID=%s, Username=%s, Role=%s, Branch=%s, Account=%s, Phone=%s", 
            id, username, upperRole, branch.name(), accountNumber, phone));
        
        return parse(line);
    }

    /** Delete employee by ID. Returns true if removed. */
    public boolean deleteById(String employeeId) {
        List<String> lines = new ArrayList<>(db.readAllLines());
        boolean removed = false;

        for (Iterator<String> it = lines.iterator(); it.hasNext();) {
            String s = it.next();
            if (s == null) continue;
            String line = s.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] t = line.split(",", -1);
            if (t.length < 7) continue;
            if (t[0].equals(employeeId)) {
                it.remove();
                removed = true;
            }
        }
        if (removed) {
            db.writeAllLines(lines);
            
            // Log the employee deletion
            Loggers.employees().info(String.format("EMPLOYEE_DELETED: ID=%s", employeeId));
        }
        return removed;
    }

    private String nextEmployeeId() {
        int max = 0;
        for (EmployeeRecord r : listAll()) {
            try {
                max = Math.max(max, Integer.parseInt(r.employeeId()));
            } catch (NumberFormatException ignored) {}
        }
        return String.valueOf(max + 1);
    }

    private EmployeeRecord parse(String s) {
        String[] t = s.split(",", -1);
        // employeeId,username,hash,role,branch,accountNumber,phone
        if (t.length < 7) throw new IllegalStateException("Bad employees line: " + s);
        return new EmployeeRecord(
                t[0], t[1], t[3], Branch.valueOf(t[4]),
                t[5], t[6]
        );
    }
}
