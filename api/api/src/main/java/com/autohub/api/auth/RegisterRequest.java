package com.autohub.api.auth;

public class RegisterRequest {
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String email; // Added this field
    private String phoneNumber;
    private String businessName;
    private String address;

    public RegisterRequest() {}

    // GETTERS
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; } // Updated to return actual email
    public String getPhoneNumber() { return phoneNumber; }
    public String getBusinessName() { return businessName; }
    public String getAddress() { return address; }

    // SETTERS
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; } // Added this setter
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public void setAddress(String address) { this.address = address; }
}