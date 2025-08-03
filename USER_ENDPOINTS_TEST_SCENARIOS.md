# 🧪 User Endpoints - Complete Test Scenarios

## 📋 Table of Contents
- [Overview](#overview)
- [GET /users - Get All Users](#get-users---get-all-users)
- [GET /users/{id} - Get User by ID](#get-usersid---get-user-by-id)
- [POST /users - Create User](#post-users---create-user)
- [PATCH /users/{id} - Update User](#patch-usersid---update-user)
- [DELETE /users/{id} - Delete User](#delete-usersid---delete-user)
- [Testing Checklist](#testing-checklist)

---

## 📊 Overview

| Method | Endpoint | Description | Access Control | Rate Limit | Status |
|--------|----------|-------------|----------------|------------|--------|
| **GET** | `/users` | Get all users with pagination & filtering | Admin only | 10/min | ✅ |
| **GET** | `/users/{id}` | Get specific user by ID | Self or Admin | 30/min | ✅ |
| **POST** | `/users` | Create new user | Public | None | ✅ |
| **PATCH** | `/users/{id}` | Update user partially | Self or Admin | None | ✅ |
| **DELETE** | `/users/{id}` | Soft delete user account | Self or Admin | 5/min | ✅ |

---

## 🔍 GET /users - Get All Users

**Endpoint:** `GET /users`  
**Access:** Admin only  
**Rate Limit:** 10 requests per minute  
**Description:** Retrieve paginated list of users with optional filtering and sorting

### ✅ Valid Test Cases

#### 1. Basic Pagination
```bash
GET /users?page=0&size=10
```
**Expected Response:**
```json
{
  "content": [
    {
      "id": 1,
      "firstName": "John",
      "lastName": "Doe",
      "email": "john@example.com",
      "username": "johndoe",
      "accountStatus": "ACTIVE",
      "role": "USER"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 150,
  "totalPages": 15,
  "last": false,
  "first": true,
  "numberOfElements": 10
}
```
**Status:** 200 OK

#### 2. With Sorting
```bash
GET /users?page=0&size=20&sortBy=firstName&sortDir=asc
```
**Expected Response:** 200 OK with users sorted by firstName ascending

#### 3. With Status Filter
```bash
GET /users?status=ACTIVE
```
**Expected Response:** 200 OK with only active users

#### 4. With Role Filter
```bash
GET /users?role=USER
```
**Expected Response:** 200 OK with only users having USER role

#### 5. With Search
```bash
GET /users?search=john
```
**Expected Response:** 200 OK with users whose first/last name contains "john" (case-insensitive)

#### 6. Combined Filters
```bash
GET /users?page=0&size=15&sortBy=createdAt&sortDir=desc&status=ACTIVE&search=john
```
**Expected Response:** 200 OK with filtered, sorted, paginated results

#### 7. Default Parameters
```bash
GET /users
```
**Expected Response:** 200 OK with default pagination (page=0, size=20, sortBy=id, sortDir=asc)

### ❌ Invalid Test Cases

#### 1. Unauthorized Access
```bash
# Without admin role
GET /users
```
**Expected Response:**
```json
{
  "error": "Access denied",
  "message": "Admin role required"
}
```
**Status:** 403 Forbidden

#### 2. Rate Limit Exceeded
```bash
# Make 11 requests within 1 minute
GET /users
```
**Expected Response:**
```json
{
  "error": "Rate limit exceeded",
  "message": "Too many requests. Please try again later.",
  "limit": 10,
  "timeWindow": "MINUTES"
}
```
**Status:** 429 Too Many Requests

#### 3. Invalid Page Parameter
```bash
GET /users?page=-1&size=10
```
**Expected Response:** 400 Bad Request

#### 4. Invalid Size Parameter
```bash
GET /users?page=0&size=0
GET /users?page=0&size=1001
```
**Expected Response:** 400 Bad Request

#### 5. Invalid Sort Direction
```bash
GET /users?sortBy=firstName&sortDir=invalid
```
**Expected Response:** 400 Bad Request

---

## 🔍 GET /users/{id} - Get User by ID

**Endpoint:** `GET /users/{id}`  
**Access:** Self or Admin  
**Rate Limit:** 30 requests per minute  
**Description:** Retrieve a specific user by their ID

### ✅ Valid Test Cases

#### 1. User Accessing Own Profile
```bash
# User ID 123 accessing their own profile
GET /users/123
```
**Expected Response:**
```json
{
  "id": 123,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "username": "johndoe",
  "phoneNumber": "+1234567890",
  "dob": "1990-01-15",
  "gender": "MALE",
  "activityLevel": "MODERATE",
  "dailyCalorieIntakeTarget": 2000,
  "dailyCalorieBurnTarget": 500,
  "weight": 70.5,
  "height": 175.0,
  "heightCm": {
    "value": 175.0,
    "unit": "cm"
  },
  "heightFeet": {
    "value": 5.74,
    "unit": "feet"
  },
  "role": "USER",
  "accountStatus": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```
**Status:** 200 OK

#### 2. Admin Accessing Any User
```bash
# Admin accessing user 456
GET /users/456
```
**Expected Response:** 200 OK with user data

#### 3. Valid ID Format
```bash
GET /users/1
```
**Expected Response:** 200 OK or 404 Not Found

### ❌ Invalid Test Cases

#### 1. Unauthorized Access
```bash
# User 123 trying to access user 456
GET /users/456
```
**Expected Response:**
```json
{
  "error": "Access denied",
  "message": "User can only access their own profile or ADMIN role required"
}
```
**Status:** 403 Forbidden

#### 2. Invalid ID Format
```bash
GET /users/0
GET /users/-1
GET /users/abc
```
**Expected Response:**
```json
{
  "error": "Invalid user ID",
  "message": "User ID must be a positive number",
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 400 Bad Request

#### 3. Non-existent User
```bash
GET /users/99999
```
**Expected Response:** 404 Not Found

#### 4. Inactive User
```bash
GET /users/123  # where user status = INACTIVE
```
**Expected Response:** 404 Not Found

#### 5. Rate Limit Exceeded
```bash
# Make 31 requests within 1 minute
GET /users/123
```
**Expected Response:**
```json
{
  "error": "Rate limit exceeded",
  "message": "Too many requests. Please try again later.",
  "limit": 30,
  "timeWindow": "MINUTES"
}
```
**Status:** 429 Too Many Requests

---

## ➕ POST /users - Create User

**Endpoint:** `POST /users`  
**Access:** Public  
**Rate Limit:** None  
**Description:** Create a new user account with comprehensive validation

### ✅ Valid Test Cases

#### 1. Complete Valid User
```bash
POST /users
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "username": "johndoe",
  "password": "SecurePass123!",
  "phoneNumber": "+1234567890",
  "dob": "1990-01-15",
  "gender": "MALE",
  "activityLevel": "MODERATE",
  "role": "USER",
  "dailyCalorieIntakeTarget": 2000,
  "dailyCalorieBurnTarget": 500,
  "weight": 70.5,
  "height": {
    "value": 175.0,
    "unit": "cm"
  }
}
```
**Expected Response:**
```json
{
  "id": 124,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "username": "johndoe",
  "phoneNumber": "+1234567890",
  "dob": "1990-01-15",
  "gender": "MALE",
  "activityLevel": "MODERATE",
  "dailyCalorieIntakeTarget": 2000,
  "dailyCalorieBurnTarget": 500,
  "weight": 70.5,
  "height": 175.0,
  "heightCm": {
    "value": 175.0,
    "unit": "cm"
  },
  "heightFeet": {
    "value": 5.74,
    "unit": "feet"
  },
  "role": "USER",
  "accountStatus": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```
**Status:** 200 OK

#### 2. Minimal Required Fields
```bash
POST /users
Content-Type: application/json

{
  "firstName": "Jane",
  "email": "jane@example.com",
  "username": "jane",
  "password": "SecurePass123!",
  "dob": "1995-05-20",
  "gender": "FEMALE",
  "activityLevel": "LIGHT",
  "role": "USER"
}
```
**Expected Response:** 200 OK with created user

#### 3. Height in Feet
```bash
POST /users
Content-Type: application/json

{
  "firstName": "Bob",
  "email": "bob@example.com",
  "username": "bob",
  "password": "SecurePass123!",
  "dob": "1985-12-10",
  "gender": "MALE",
  "activityLevel": "ACTIVE",
  "role": "USER",
  "height": {
    "value": 6.0,
    "unit": "feet"
  }
}
```
**Expected Response:** 200 OK with height converted to cm

### ❌ Invalid Test Cases

#### 1. Missing Required Fields
```bash
POST /users
Content-Type: application/json

{
  "firstName": "John",
  "email": "john@example.com"
  // Missing username, password, dob, gender, activityLevel, role
}
```
**Expected Response:**
```json
{
  "error": "Validation failed",
  "message": "Please fix the following validation errors",
  "totalErrors": 6,
  "fieldErrors": {
    "username": "Username is required",
    "password": "Password is required",
    "dob": "Date of birth is required",
    "gender": "Gender is required",
    "activityLevel": "Activity level is required",
    "role": "Role is required"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 400 Bad Request

#### 2. Invalid Email Format
```bash
POST /users
Content-Type: application/json

{
  "firstName": "John",
  "email": "invalid-email",
  "username": "john",
  "password": "SecurePass123!",
  "dob": "1990-01-15",
  "gender": "MALE",
  "activityLevel": "MODERATE",
  "role": "USER"
}
```
**Expected Response:**
```json
{
  "error": "Validation failed",
  "message": "Please fix the following validation errors",
  "totalErrors": 1,
  "fieldErrors": {
    "email": "Email must be a valid email address"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 400 Bad Request

#### 3. Weak Password
```bash
POST /users
Content-Type: application/json

{
  "firstName": "John",
  "email": "john@example.com",
  "username": "john",
  "password": "weak",
  "dob": "1990-01-15",
  "gender": "MALE",
  "activityLevel": "MODERATE",
  "role": "USER"
}
```
**Expected Response:**
```json
{
  "error": "Validation failed",
  "message": "Please fix the following validation errors",
  "totalErrors": 1,
  "fieldErrors": {
    "password": "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 400 Bad Request

#### 4. Duplicate Email
```bash
POST /users
Content-Type: application/json

{
  "firstName": "John",
  "email": "existing@example.com",  // Already exists
  "username": "john",
  "password": "SecurePass123!",
  "dob": "1990-01-15",
  "gender": "MALE",
  "activityLevel": "MODERATE",
  "role": "USER"
}
```
**Expected Response:**
```json
{
  "error": "Validation failed",
  "message": "Please fix the following validation errors",
  "totalErrors": 1,
  "fieldErrors": {
    "email": "Email already exists"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 400 Bad Request

#### 5. Duplicate Username
```bash
POST /users
Content-Type: application/json

{
  "firstName": "John",
  "email": "john@example.com",
  "username": "existing",  // Already exists
  "password": "SecurePass123!",
  "dob": "1990-01-15",
  "gender": "MALE",
  "activityLevel": "MODERATE",
  "role": "USER"
}
```
**Expected Response:**
```json
{
  "error": "Validation failed",
  "message": "Please fix the following validation errors",
  "totalErrors": 1,
  "fieldErrors": {
    "username": "Username already exists"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 400 Bad Request

#### 6. Invalid Height
```bash
POST /users
Content-Type: application/json

{
  "firstName": "John",
  "email": "john@example.com",
  "username": "john",
  "password": "SecurePass123!",
  "dob": "1990-01-15",
  "gender": "MALE",
  "activityLevel": "MODERATE",
  "role": "USER",
  "height": {
    "value": 50.0,  // Too short
    "unit": "cm"
  }
}
```
**Expected Response:**
```json
{
  "error": "Validation failed",
  "message": "Please fix the following validation errors",
  "totalErrors": 1,
  "fieldErrors": {
    "height": "Height must be between 100 and 250 cm"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 400 Bad Request

#### 7. Underage User
```bash
POST /users
Content-Type: application/json

{
  "firstName": "Child",
  "email": "child@example.com",
  "username": "child",
  "password": "SecurePass123!",
  "dob": "2020-01-15",  // 4 years old
  "gender": "MALE",
  "activityLevel": "LIGHT",
  "role": "USER"
}
```
**Expected Response:**
```json
{
  "error": "Validation failed",
  "message": "Please fix the following validation errors",
  "totalErrors": 1,
  "fieldErrors": {
    "dob": "User must be at least 13 years old"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 400 Bad Request

#### 8. Invalid Weight
```bash
POST /users
Content-Type: application/json

{
  "firstName": "John",
  "email": "john@example.com",
  "username": "john",
  "password": "SecurePass123!",
  "dob": "1990-01-15",
  "gender": "MALE",
  "activityLevel": "MODERATE",
  "role": "USER",
  "weight": 500.0  // Too heavy
}
```
**Expected Response:**
```json
{
  "error": "Validation failed",
  "message": "Please fix the following validation errors",
  "totalErrors": 1,
  "fieldErrors": {
    "weight": "Weight must be between 30 and 300 kg"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 400 Bad Request

---

## 🔧 PATCH /users/{id} - Update User

**Endpoint:** `PATCH /users/{id}`  
**Access:** Self or Admin  
**Rate Limit:** None  
**Description:** Partially update an existing user's information

### ✅ Valid Test Cases

#### 1. Update Single Field
```bash
PATCH /users/123
Content-Type: application/json

{
  "firstName": "Jonathan"
}
```
**Expected Response:** 200 OK with updated user

#### 2. Update Multiple Fields
```bash
PATCH /users/123
Content-Type: application/json

{
  "firstName": "Jonathan",
  "lastName": "Smith",
  "phoneNumber": "+1987654321",
  "weight": 75.0
}
```
**Expected Response:** 200 OK with updated user

#### 3. Update Height in CM
```bash
PATCH /users/123
Content-Type: application/json

{
  "height": {
    "value": 180.0,
    "unit": "cm"
  }
}
```
**Expected Response:** 200 OK with height updated

#### 4. Update Height in Feet
```bash
PATCH /users/123
Content-Type: application/json

{
  "height": {
    "value": 6.2,
    "unit": "feet"
  }
}
```
**Expected Response:** 200 OK with height converted and updated

#### 5. Update Password
```bash
PATCH /users/123
Content-Type: application/json

{
  "password": "NewSecurePass456!"
}
```
**Expected Response:** 200 OK with password updated (hashed)

#### 6. Update Account Status
```bash
PATCH /users/123
Content-Type: application/json

{
  "accountStatus": "INACTIVE"
}
```
**Expected Response:** 200 OK with status updated

#### 7. Update Calorie Targets
```bash
PATCH /users/123
Content-Type: application/json

{
  "dailyCalorieIntakeTarget": 2200,
  "dailyCalorieBurnTarget": 600
}
```
**Expected Response:** 200 OK with targets updated

### ❌ Invalid Test Cases

#### 1. Unauthorized Access
```bash
PATCH /users/456  # User 123 trying to update user 456
Content-Type: application/json

{
  "firstName": "Hacked"
}
```
**Expected Response:**
```json
{
  "error": "Access denied",
  "message": "User can only update their own profile or ADMIN role required"
}
```
**Status:** 403 Forbidden

#### 2. Invalid Email (Duplicate)
```bash
PATCH /users/123
Content-Type: application/json

{
  "email": "existing@example.com"  // Already exists
}
```
**Expected Response:**
```json
{
  "error": "Validation failed",
  "message": "Please fix the following validation errors",
  "totalErrors": 1,
  "fieldErrors": {
    "email": "Email already exists"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 400 Bad Request

#### 3. Invalid Weight
```bash
PATCH /users/123
Content-Type: application/json

{
  "weight": 500.0  // Too heavy
}
```
**Expected Response:**
```json
{
  "error": "Validation failed",
  "message": "Please fix the following validation errors",
  "totalErrors": 1,
  "fieldErrors": {
    "weight": "Weight must be between 30 and 300 kg"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 400 Bad Request

#### 4. Invalid Account Status
```bash
PATCH /users/123
Content-Type: application/json

{
  "accountStatus": "DELETED"  // Not allowed via PATCH
}
```
**Expected Response:**
```json
{
  "error": "Validation failed",
  "message": "Please fix the following validation errors",
  "totalErrors": 1,
  "fieldErrors": {
    "accountStatus": "Cannot set account status to DELETED via PATCH"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 400 Bad Request

#### 5. Non-existent User
```bash
PATCH /users/99999
Content-Type: application/json

{
  "firstName": "Test"
}
```
**Expected Response:**
```json
{
  "error": "User patch failed",
  "message": "User not found",
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 400 Bad Request

#### 6. Invalid Height
```bash
PATCH /users/123
Content-Type: application/json

{
  "height": {
    "value": 50.0,  // Too short
    "unit": "cm"
  }
}
```
**Expected Response:**
```json
{
  "error": "Validation failed",
  "message": "Please fix the following validation errors",
  "totalErrors": 1,
  "fieldErrors": {
    "height": "Height must be between 100 and 250 cm"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 400 Bad Request

---

## 🗑️ DELETE /users/{id} - Delete User

**Endpoint:** `DELETE /users/{id}`  
**Access:** Self or Admin  
**Rate Limit:** 5 requests per minute  
**Description:** Soft delete a user account (marks as DELETED, preserves data)

### ✅ Valid Test Cases

#### 1. User Deleting Own Account
```bash
DELETE /users/123  # User 123 deleting their own account
```
**Expected Response:**
```json
{
  "message": "User account deleted successfully",
  "userId": 123,
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 200 OK

#### 2. Admin Deleting Any User
```bash
DELETE /users/456  # Admin deleting user 456
```
**Expected Response:**
```json
{
  "message": "User account deleted successfully",
  "userId": 456,
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 200 OK

#### 3. Valid ID Format
```bash
DELETE /users/1
```
**Expected Response:** 200 OK or 400 Bad Request

### ❌ Invalid Test Cases

#### 1. Unauthorized Access
```bash
DELETE /users/456  # User 123 trying to delete user 456
```
**Expected Response:**
```json
{
  "error": "Access denied",
  "message": "User can only delete their own account or ADMIN role required"
}
```
**Status:** 403 Forbidden

#### 2. Invalid ID Format
```bash
DELETE /users/0
DELETE /users/-1
DELETE /users/abc
```
**Expected Response:**
```json
{
  "error": "Invalid user ID",
  "message": "User ID must be a positive number",
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 400 Bad Request

#### 3. Non-existent User
```bash
DELETE /users/99999
```
**Expected Response:**
```json
{
  "error": "Deletion failed",
  "message": "User not found",
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 400 Bad Request

#### 4. Already Deleted User
```bash
DELETE /users/123  # User already has status = DELETED
```
**Expected Response:**
```json
{
  "error": "User already deleted",
  "message": "This user account has already been deleted",
  "timestamp": "2024-01-15T10:30:00"
}
```
**Status:** 400 Bad Request

#### 5. Rate Limit Exceeded
```bash
# Make 6 requests within 1 minute
DELETE /users/123
```
**Expected Response:**
```json
{
  "error": "Rate limit exceeded",
  "message": "Too many requests. Please try again later.",
  "limit": 5,
  "timeWindow": "MINUTES"
}
```
**Status:** 429 Too Many Requests

---

## ✅ Testing Checklist

### 🔐 Security Testing
- [ ] **GET /users**: Test unauthorized access (non-admin)
- [ ] **GET /users/{id}**: Test unauthorized access (wrong user)
- [ ] **PATCH /users/{id}**: Test unauthorized access (wrong user)
- [ ] **DELETE /users/{id}**: Test unauthorized access (wrong user)
- [ ] **Rate Limiting**: Test all rate-limited endpoints
- [ ] **Input Validation**: Test all endpoints with invalid IDs

### 📊 Functional Testing
- [ ] **GET /users**: Test pagination, sorting, filtering, search
- [ ] **GET /users/{id}**: Test valid and invalid user IDs
- [ ] **POST /users**: Test all validation scenarios
- [ ] **PATCH /users/{id}**: Test partial updates
- [ ] **DELETE /users/{id}**: Test soft delete functionality
- [ ] **Height Conversion**: Test CM ↔ Feet conversion

### 🚨 Error Handling Testing
- [ ] **400 Bad Request**: Test all validation errors
- [ ] **403 Forbidden**: Test unauthorized access
- [ ] **404 Not Found**: Test non-existent resources
- [ ] **429 Too Many Requests**: Test rate limiting
- [ ] **500 Internal Server Error**: Test server errors

### 🔄 Data Integrity Testing
- [ ] **Soft Delete**: Verify data preservation after deletion
- [ ] **Account Status**: Test status filtering and updates
- [ ] **Password Hashing**: Verify password encryption
- [ ] **Email Uniqueness**: Test duplicate email prevention
- [ ] **Username Uniqueness**: Test duplicate username prevention

### 📝 Audit & Logging Testing
- [ ] **Access Logs**: Verify all access attempts are logged
- [ ] **Error Logs**: Verify error scenarios are logged
- [ ] **Security Logs**: Verify unauthorized attempts are logged
- [ ] **Timestamp Tracking**: Verify all responses include timestamps

### 🌐 Integration Testing
- [ ] **Cross-Endpoint**: Test interactions between endpoints
- [ ] **Data Consistency**: Verify data consistency across operations
- [ ] **State Management**: Test account status transitions
- [ ] **Recovery Scenarios**: Test data recovery after soft delete

---

## 🎯 Test Environment Setup

### Prerequisites
- [ ] Database with test data
- [ ] Admin user account
- [ ] Regular user account
- [ ] API testing tool (Postman, curl, etc.)
- [ ] Rate limiting monitoring

### Test Data Requirements
- [ ] Multiple users with different roles
- [ ] Users with different account statuses
- [ ] Users with various data combinations
- [ ] Duplicate email/username scenarios
- [ ] Edge case data (min/max values)

### Monitoring
- [ ] Application logs
- [ ] Database state
- [ ] Rate limiting counters
- [ ] Error tracking
- [ ] Performance metrics

---

## 📚 Additional Resources

- **API Documentation**: Swagger UI at `/swagger-ui.html`
- **Database Schema**: Check entity classes for field definitions
- **Validation Rules**: See `ValidationService.java` for detailed rules
- **Security Configuration**: See `SecurityConfig.java` for access control
- **Rate Limiting**: See `RateLimitService.java` for rate limiting logic

---

*Last Updated: January 2024*  
*Version: 1.0*  
*Status: Production Ready* ✅ 