package com.example.cafelariapos;

public class UserData {
    private String emailAddress;
    private String firstName;
    private String lastName;
    private String mobileNo;
    private String username;
    private String password;

    public UserData(String emailAddress, String firstName, String lastName, String mobileNo, String username, String password) {
        this.emailAddress = emailAddress;
        this.firstName = firstName;
        this.lastName = lastName;
        this.mobileNo = mobileNo;
        this.username = username;
        this.password = password;
    }

    public String getEmailAddress() { return emailAddress; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getMobileNo() { return mobileNo; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }

    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setMobileNo(String mobileNo) { this.mobileNo = mobileNo; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
}
