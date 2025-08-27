package server.domain.employees;

import server.shared.Branch;

public abstract class Employee {
    private final String employeeId;
    private final String username;
    private final String passwordHash;
    private final Branch branch;

    protected Employee(String employeeId, String username, String passwordHash, Branch branch) {
        this.employeeId = employeeId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.branch = branch;
    }

    public String employeeId() { return employeeId; }
    public String username() { return username; }
    public String passwordHash() { return passwordHash; }
    public Branch branch() { return branch; }

    public boolean canJoinExistingChat() { return false; }
    public abstract String role();
}
