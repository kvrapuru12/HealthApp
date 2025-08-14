# User Endpoints Test Scenarios

## Overview

This document provides comprehensive test scenarios for all user-related endpoints in the HealthApp API. These scenarios help ensure proper functionality, validation, and security of user operations.

## Base Configuration

- **Base URL**: `http://localhost:8080/api`
- **Content-Type**: `application/json`
- **Authentication**: JWT token in `Authorization` header (where required)

## Test Scenarios

### 1. User Registration (POST /users)

#### 1.1 Successful User Registration
```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "username": "johndoe",
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "phoneNumber": "+1234567890",
    "dateOfBirth": "1990-01-01",
    "gender": "MALE",
    "heightCm": 175.0,
    "weightKg": 70.0,
    "activityLevel": "MODERATE",
    "dailyCalorieIntakeTarget": 2000,
    "dailyCalorieBurnTarget": 500,
    "role": "USER"
  }'
```

**Expected Response**: 200 OK with user details
**Validation**: User created with ID, timestamps set

#### 1.2 Registration with Missing Required Fields
```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "email": "john.doe@example.com"
  }'
```

**Expected Response**: 400 Bad Request with validation errors
**Validation**: Missing required fields identified

#### 1.3 Registration with Invalid Email Format
```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "username": "johndoe",
    "email": "invalid-email",
    "password": "SecurePass123!",
    "dateOfBirth": "1990-01-01",
    "gender": "MALE",
    "role": "USER"
  }'
```

**Expected Response**: 400 Bad Request
**Validation**: Email format validation error

#### 1.4 Registration with Weak Password
```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "username": "johndoe",
    "email": "john.doe@example.com",
    "password": "weak",
    "dateOfBirth": "1990-01-01",
    "gender": "MALE",
    "role": "USER"
  }'
```

**Expected Response**: 400 Bad Request
**Validation**: Password strength validation error

#### 1.5 Registration with Duplicate Username
```bash
# First registration (should succeed)
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Smith",
    "username": "janesmith",
    "email": "jane.smith@example.com",
    "password": "SecurePass123!",
    "dateOfBirth": "1992-05-15",
    "gender": "FEMALE",
    "role": "USER"
  }'

# Second registration with same username (should fail)
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "username": "janesmith",
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "dateOfBirth": "1990-01-01",
    "gender": "MALE",
    "role": "USER"
  }'
```

**Expected Response**: 400 Bad Request
**Validation**: Username uniqueness constraint

#### 1.6 Registration with Duplicate Email
```bash
# Second registration with same email (should fail)
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "username": "johndoe",
    "email": "jane.smith@example.com",
    "password": "SecurePass123!",
    "dateOfBirth": "1990-01-01",
    "gender": "MALE",
    "role": "USER"
  }'
```

**Expected Response**: 400 Bad Request
**Validation**: Email uniqueness constraint

### 2. User Authentication (POST /auth/login)

#### 2.1 Successful Login
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePass123!"
  }'
```

**Expected Response**: 200 OK with JWT token
**Validation**: Token returned, user details included

#### 2.2 Login with Invalid Username
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "nonexistent",
    "password": "SecurePass123!"
  }'
```

**Expected Response**: 400 Bad Request
**Validation**: Authentication failed message

#### 2.3 Login with Invalid Password
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "wrongpassword"
  }'
```

**Expected Response**: 400 Bad Request
**Validation**: Authentication failed message

#### 2.4 Login with Inactive Account
```bash
# First, create a user and then deactivate them
# Then attempt login
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "inactiveuser",
    "password": "SecurePass123!"
  }'
```

**Expected Response**: 400 Bad Request
**Validation**: Account inactive message

### 3. Get User Profile (GET /users/{id})

#### 3.1 Get Own Profile (Authenticated)
```bash
# First login to get token
TOKEN=$(curl -s -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "SecurePass123!"
  }' | jq -r '.token')

# Then get profile
curl -H "Authorization: $TOKEN" \
  "http://localhost:8080/api/users/1"
```

**Expected Response**: 200 OK with user profile
**Validation**: Complete user details returned

#### 3.2 Get Another User's Profile (Unauthorized)
```bash
curl -H "Authorization: $TOKEN" \
  "http://localhost:8080/api/users/2"
```

**Expected Response**: 403 Forbidden
**Validation**: Access denied for other users

#### 3.3 Get Profile Without Authentication
```bash
curl "http://localhost:8080/api/users/1"
```

**Expected Response**: 401 Unauthorized
**Validation**: Authentication required

#### 3.4 Get Non-existent User Profile
```bash
curl -H "Authorization: $TOKEN" \
  "http://localhost:8080/api/users/999"
```

**Expected Response**: 404 Not Found
**Validation**: User not found message

### 4. Update User Profile (PATCH /users/{id})

#### 4.1 Update Own Profile (Authenticated)
```bash
curl -X PATCH "http://localhost:8080/api/users/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: $TOKEN" \
  -d '{
    "firstName": "Johnny",
    "dailyCalorieIntakeTarget": 2200
  }'
```

**Expected Response**: 200 OK with updated profile
**Validation**: Changes applied and returned

#### 4.2 Update Another User's Profile (Unauthorized)
```bash
curl -X PATCH "http://localhost:8080/api/users/2" \
  -H "Content-Type: application/json" \
  -H "Authorization: $TOKEN" \
  -d '{
    "firstName": "Modified"
  }'
```

**Expected Response**: 403 Forbidden
**Validation**: Access denied for other users

#### 4.3 Update Profile with Invalid Data
```bash
curl -X PATCH "http://localhost:8080/api/users/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: $TOKEN" \
  -d '{
    "email": "invalid-email-format"
  }'
```

**Expected Response**: 400 Bad Request
**Validation**: Validation errors returned

### 5. Delete User (DELETE /users/{id})

#### 5.1 Delete Own Account (Authenticated)
```bash
curl -X DELETE "http://localhost:8080/api/users/1" \
  -H "Authorization: $TOKEN"
```

**Expected Response**: 200 OK
**Validation**: Account marked as deleted

#### 5.2 Delete Another User's Account (Unauthorized)
```bash
curl -X DELETE "http://localhost:8080/api/users/2" \
  -H "Authorization: $TOKEN"
```

**Expected Response**: 403 Forbidden
**Validation**: Access denied for other users

#### 5.3 Delete Account Without Authentication
```bash
curl -X DELETE "http://localhost:8080/api/users/1"
```

**Expected Response**: 401 Unauthorized
**Validation**: Authentication required

### 6. List Users (GET /users) - Admin Only

#### 6.1 List Users as Admin
```bash
# Login as admin user
ADMIN_TOKEN=$(curl -s -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "AdminPass123!"
  }' | jq -r '.token')

# List all users
curl -H "Authorization: $ADMIN_TOKEN" \
  "http://localhost:8080/api/users"
```

**Expected Response**: 200 OK with paginated user list
**Validation**: All users returned (paginated)

#### 6.2 List Users as Regular User (Unauthorized)
```bash
curl -H "Authorization: $TOKEN" \
  "http://localhost:8080/api/users"
```

**Expected Response**: 403 Forbidden
**Validation**: Admin role required

#### 6.3 List Users with Pagination
```bash
curl -H "Authorization: $ADMIN_TOKEN" \
  "http://localhost:8080/api/users?page=0&size=5"
```

**Expected Response**: 200 OK with 5 users
**Validation**: Pagination working correctly

#### 6.4 List Users with Filtering
```bash
curl -H "Authorization: $ADMIN_TOKEN" \
  "http://localhost:8080/api/users?status=ACTIVE&role=USER"
```

**Expected Response**: 200 OK with filtered users
**Validation**: Filtering working correctly

### 7. Edge Cases and Error Handling

#### 7.1 Malformed JSON
```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "username": "johndoe",
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "dateOfBirth": "1990-01-01",
    "gender": "MALE",
    "role": "USER"
  '  # Missing closing brace
```

**Expected Response**: 400 Bad Request
**Validation**: JSON parsing error

#### 7.2 Invalid Date Format
```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "username": "johndoe",
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "dateOfBirth": "invalid-date",
    "gender": "MALE",
    "role": "USER"
  }'
```

**Expected Response**: 400 Bad Request
**Validation**: Date format validation error

#### 7.3 Invalid Enum Values
```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "username": "johndoe",
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "dateOfBirth": "1990-01-01",
    "gender": "INVALID_GENDER",
    "role": "USER"
  }'
```

**Expected Response**: 400 Bad Request
**Validation**: Enum validation error

#### 7.4 SQL Injection Attempt
```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John\"; DROP TABLE users; --",
    "lastName": "Doe",
    "username": "johndoe",
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "dateOfBirth": "1990-01-01",
    "gender": "MALE",
    "role": "USER"
  }'
```

**Expected Response**: 400 Bad Request
**Validation**: Input sanitization working

### 8. Performance and Load Testing

#### 8.1 Concurrent User Creation
```bash
# Create multiple users simultaneously
for i in {1..10}; do
  curl -X POST "http://localhost:8080/api/users" \
    -H "Content-Type: application/json" \
    -d "{
      \"firstName\": \"User$i\",
      \"lastName\": \"Test\",
      \"username\": \"user$i\",
      \"email\": \"user$i@example.com\",
      \"password\": \"SecurePass123!\",
      \"dateOfBirth\": \"1990-01-01\",
      \"gender\": \"MALE\",
      \"role\": \"USER\"
    }" &
done
wait
```

**Expected Response**: All requests processed successfully
**Validation**: No deadlocks or data corruption

#### 8.2 Large Data Sets
```bash
# Test with large text fields
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "username": "johndoe",
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "dateOfBirth": "1990-01-01",
    "gender": "MALE",
    "role": "USER",
    "notes": "'$(printf 'A%.0s' {1..1000})'"
  }'
```

**Expected Response**: 400 Bad Request or 200 OK
**Validation**: Large data handling

## Test Data Setup

### Pre-test Cleanup
```bash
# Clear existing test data
mysql -u root -p'your_password' -e "USE healthapp; DELETE FROM users WHERE username LIKE 'test%';"
```

### Test User Creation
```bash
# Create test users for different scenarios
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "Admin",
    "username": "testadmin",
    "email": "testadmin@example.com",
    "password": "AdminPass123!",
    "dateOfBirth": "1985-01-01",
    "gender": "MALE",
    "role": "ADMIN"
  }'
```

## Expected Test Results Summary

| Test Category | Total Tests | Expected Pass | Expected Fail |
|---------------|-------------|---------------|---------------|
| User Registration | 6 | 1 | 5 |
| User Authentication | 4 | 1 | 3 |
| Profile Management | 4 | 2 | 2 |
| User Deletion | 3 | 1 | 2 |
| Admin Operations | 4 | 2 | 2 |
| Edge Cases | 4 | 0 | 4 |
| Performance | 2 | 2 | 0 |
| **Total** | **27** | **9** | **18** |

## Notes

- All tests should be run in a clean test environment
- Database should be reset between test runs
- JWT tokens expire after 24 hours
- Rate limiting is applied to some endpoints
- Validation errors return detailed field-specific messages
