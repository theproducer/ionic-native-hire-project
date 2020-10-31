package io.ionic.starter;

import java.util.ArrayList;

public class AppContact {
    String id;
    String firstName;
    String lastName;
    ArrayList<String> emailAddresses;
    ArrayList<String> phoneNumbers;

    public AppContact() {
        this.id = "";
        this.firstName = "";
        this.lastName = "";
        this.emailAddresses = new ArrayList<String>();
        this.phoneNumbers = new ArrayList<String>();
    }

    public void setFirstName(String firstName) {
        if (firstName == null) {
            this.firstName = "";
        } else {
            this.firstName = firstName;
        }
    }

    public void setLastName(String lastName) {
        if (lastName == null) {
            this.lastName = "";
        } else {
            this.lastName = lastName;
        }
    }

    public void addEmailAddress(String email) {
        this.emailAddresses.add(email);
    }

    public void addPhoneNumber(String phone) {
        this.phoneNumbers.add(phone);
    }
}
