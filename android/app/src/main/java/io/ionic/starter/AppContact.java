package io.ionic.starter;

import java.util.ArrayList;

public class AppContact {
    String firstName;
    String lastName;
    ArrayList<String> emailAddresses;
    ArrayList<String> phoneNumbers;

    public AppContact() {
        this.firstName = "";
        this.lastName = "";
        this.emailAddresses = new ArrayList<String>();
        this.phoneNumbers = new ArrayList<String>();
    }

    public void addEmailAddress(String email) {
        this.emailAddresses.add(email);
    }

    public void addPhoneNumber(String phone) {
        this.phoneNumbers.add(phone);
    }
}
