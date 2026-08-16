package com.enterprise.spendsync.core.internal.domain;

import com.enterprise.spendsync.shared.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * User represents an enterprise actor with assigned P2P roles, hierarchy, and audit fields.
 * Compliant with ISO 27001 (Security/Audit) and ISO 37001 (SoD/Delegation).
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = true)
    private Tenant tenant;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(name = "employee_id", length = 50)
    private String employeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_user_id")
    private User managerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegated_approver_id")
    private User delegatedApprover;

    @Column(name = "delegation_start_date")
    private Instant delegationStartDate;

    @Column(name = "delegation_end_date")
    private Instant delegationEndDate;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "country", nullable = false, length = 2)
    private String country = "TR";

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "UTC";

    @Column(name = "preferred_language", nullable = false, length = 10)
    private String preferredLanguage = "tr";

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_email_verified", nullable = false)
    private boolean isEmailVerified = false;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Set<RoleType> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_assigned_legal_entities",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "legal_entity_id")
    )
    private Set<LegalEntity> assignedLegalEntities = new HashSet<>();

    public User() {
    }

    public User(String email, String passwordHash, String firstName, String lastName, String phoneNumber, String country) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.country = country != null ? country : "TR";
        this.timezone = "UTC";
        this.preferredLanguage = "tr";
        this.isActive = true;
        this.isEmailVerified = false;
        this.failedLoginAttempts = 0;
    }

    public void addRole(RoleType role) {
        this.roles.add(role);
    }

    public void removeRole(RoleType role) {
        this.roles.remove(role);
    }

    public void assignLegalEntity(LegalEntity legalEntity) {
        this.assignedLegalEntities.add(legalEntity);
    }

    public void unassignLegalEntity(LegalEntity legalEntity) {
        this.assignedLegalEntities.remove(legalEntity);
    }

    public boolean isAccountNonLocked() {
        return lockedUntil == null || Instant.now().isAfter(lockedUntil);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public User getManagerUser() {
        return managerUser;
    }

    public void setManagerUser(User managerUser) {
        this.managerUser = managerUser;
    }

    public User getDelegatedApprover() {
        return delegatedApprover;
    }

    public void setDelegatedApprover(User delegatedApprover) {
        this.delegatedApprover = delegatedApprover;
    }

    public Instant getDelegationStartDate() {
        return delegationStartDate;
    }

    public void setDelegationStartDate(Instant delegationStartDate) {
        this.delegationStartDate = delegationStartDate;
    }

    public Instant getDelegationEndDate() {
        return delegationEndDate;
    }

    public void setDelegationEndDate(Instant delegationEndDate) {
        this.delegationEndDate = delegationEndDate;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isEmailVerified() {
        return isEmailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        isEmailVerified = emailVerified;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public Set<RoleType> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleType> roles) {
        this.roles = roles;
    }

    public Set<LegalEntity> getAssignedLegalEntities() {
        return assignedLegalEntities;
    }

    public void setAssignedLegalEntities(Set<LegalEntity> assignedLegalEntities) {
        this.assignedLegalEntities = assignedLegalEntities;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        User user = (User) o;
        return Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), email);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + getId() +
                ", email='" + email + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", jobTitle='" + jobTitle + '\'' +
                ", country='" + country + '\'' +
                ", isActive=" + isActive +
                ", roles=" + roles +
                '}';
    }
}
