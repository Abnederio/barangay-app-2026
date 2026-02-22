package com.turgo.barangayapp.Model;

import jakarta.persistence.*;
import java.time.LocalDate;

// If you plan to save this in the database, you'll need @Entity.
// If it's just a payload, you can use it as a DTO.
@Entity
@Table(name = "service_forms")
public class ServiceForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- 1. PERSONAL IDENTIFICATION (Your existing fields) ---
    private String firstName;
    private String middleName;
    private String lastName;
    private String suffix;
    private LocalDate birthday;
    private String sex;
    private String civilStatus;

    // --- 2. CONTACT & LOCATION ---
    private String address;
    private String contactNumber;
    private String emailAddress; // Good for sending digital copies/updates

    // --- 3. SOCIO-DEMOGRAPHIC & ECONOMIC (Crucial for Indigency/Assistance) ---
    private String educationalAttainment;
    private String occupation;
    private String monthlyIncome; // E.g., "Below 10k", "10k-20k"

    // --- 4. BARANGAY SPECIFIC DATA ---
    private String purokOrSitio; // Very common in barangay records
    private Integer yearsOfResidency;
    private boolean isRegisteredVoter;
    private String precinctNumber; // Optional, if they are a voter

    // --- 5. APPLICATION DETAILS ---
    @Column(nullable = false)
    private String purpose; // WHY are they applying? (e.g., "For Employment", "For Bank Account")

    private String validIdUrl; // URL to an uploaded valid ID (Cloudinary)

    // --- 6. EMERGENCY CONTACT ---
    private String emergencyContactName;
    private String emergencyContactNumber;

    public ServiceForm() {
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCivilStatus() {
        return civilStatus;
    }

    public void setCivilStatus(String civilStatus) {
        this.civilStatus = civilStatus;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEducationalAttainment() {
        return educationalAttainment;
    }

    public void setEducationalAttainment(String educationalAttainment) {
        this.educationalAttainment = educationalAttainment;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(String monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public String getPurokOrSitio() {
        return purokOrSitio;
    }

    public void setPurokOrSitio(String purokOrSitio) {
        this.purokOrSitio = purokOrSitio;
    }

    public Integer getYearsOfResidency() {
        return yearsOfResidency;
    }

    public void setYearsOfResidency(Integer yearsOfResidency) {
        this.yearsOfResidency = yearsOfResidency;
    }

    public boolean isRegisteredVoter() {
        return isRegisteredVoter;
    }

    public void setRegisteredVoter(boolean registeredVoter) {
        isRegisteredVoter = registeredVoter;
    }

    public String getPrecinctNumber() {
        return precinctNumber;
    }

    public void setPrecinctNumber(String precinctNumber) {
        this.precinctNumber = precinctNumber;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getValidIdUrl() {
        return validIdUrl;
    }

    public void setValidIdUrl(String validIdUrl) {
        this.validIdUrl = validIdUrl;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactNumber() {
        return emergencyContactNumber;
    }

    public void setEmergencyContactNumber(String emergencyContactNumber) {
        this.emergencyContactNumber = emergencyContactNumber;
    }
}