# How to Login as Admin

There are **three ways** to create an admin account:

## Method 1: First User (Automatic Admin)
The **first user** to sign up automatically becomes an admin. This is the easiest method:
1. Make sure the database is empty (fresh installation)
2. Sign up with any email - you'll automatically be an admin
3. Login with that account

## Method 2: Admin Code During Signup
1. Go to the **Sign Up** page
2. Fill in all required fields
3. In the **Admin Code** field, enter: `ADMIN2024`
4. Complete the signup
5. You'll be created as an admin account
6. Login with your credentials

## Method 3: SQL Database Update (For Existing Users)
If you already have a user account and want to make it admin, you can update it directly in the database:

### Using MySQL Command Line:
```sql
USE BarangayDB;
UPDATE users SET is_admin = 1 WHERE email = 'your-email@example.com';
```

### Using MySQL Workbench or any SQL client:
1. Connect to your MySQL database
2. Select the `BarangayDB` database
3. Run this SQL:
```sql
UPDATE users 
SET is_admin = 1 
WHERE email = 'your-email@example.com';
```
Replace `'your-email@example.com'` with your actual email address.

## Verifying Admin Status
After logging in, you should see:
- An **"Admin"** badge in the top navigation bar
- **"Create Program"**, **"Post Event"**, **"Add Official"** buttons on respective pages
- Access to admin-only features

## Notes
- Admin code is case-sensitive: `ADMIN2024` (all caps)
- Only admins can create/manage Programs, Events, Officials, and view all Feedback
- Regular users can view content and submit feedback/applications
