-- Make date_of_birth and gender nullable to support Google Sign-In
-- Users can sign in with Google, then complete profile before accessing app features
-- Frontend will require profile completion based on profileComplete flag in login response

ALTER TABLE users 
MODIFY COLUMN date_of_birth DATE NULL,
MODIFY COLUMN gender ENUM('FEMALE', 'MALE', 'NON_BINARY', 'OTHER') NULL;

