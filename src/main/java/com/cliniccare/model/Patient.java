package com.cliniccare.model;

public class Patient extends Person {
    private int age;
    private String gender;
    private String email;

    public Patient(int id, String name, String contact, int age, String gender, String email) {
        super(id, name, contact);
        this.age = age;
        this.gender = gender;
        this.email = email;
    }
public int getAge() {
    return age;
}
public void setAge(int age) {
    this.age = age;
}
public String getGender() {
    return gender;
}
public void setGender(String gender) {
    this.gender = gender;
}
public String getEmail() {
    return email;
}
public void setEmail(String email) {
    this.email = email;
}

@Override 
public String displayDetails() {
    return "Patient #" + getId() + ": " + getName()
    + ", Age " + age + ", " + gender
    + ", Contact " + getContact() + ", Email " + email;
    }
}
