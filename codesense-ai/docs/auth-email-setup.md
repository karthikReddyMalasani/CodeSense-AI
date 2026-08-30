# Authentication & Email Configuration Guide

## Issues Fixed

### Issue 1: "Account created but session not established. Please log in again."

**Root Cause:** When migrating legacy accounts to Supabase, the `signUp()` API with `autoConfirm: true` doesn't always return a session immediately, causing login failures.

**Solution Implemented:**
- After creating a Supabase account, we now immediately attempt to sign in if no session is returned
- Handles email verification flows gracefully
- Provides clear error messages to users

**Changes Made:**
### Issue 3: "Multiple accounts with the same email address in the same linking domain detected"

**Root Cause:** When a user logs in with an OAuth provider (Google/GitHub) using an email address that already has an existing account in Supabase, Supabase rejects the login if automatic identity linking is not enabled.

**Solution Implemented:**
- Created `authUtils.js` to parse URL query/hash error parameters on redirect.
- `LoginPage.jsx` automatically catches the error and displays a clear message: `"An account with this email address already exists using a different sign-in method. Please sign in using your original email & password or social login provider."`
- Automatically cleans error query parameters (`?error=...#error=...`) from the browser address bar.

**Supabase Dashboard Setting (Optional):**
If you wish to allow automatic identity linking in Supabase:
1. Go to Supabase Dashboard $\rightarrow$ Authentication $\rightarrow$ Providers.
2. Under general settings / identity linking, configure manual vs. automatic identity linking behavior per project security requirements.

## Supabase Email Configuration Steps

**Root Cause:** Email service configuration in Supabase needs proper setup.

**Solution:**
Supabase Email Authentication requires either:
1. **Supabase Email Service** (default but requires email templates)
2. **Third-party SMTP** (SendGrid, Mailgun, AWS SES)

## Supabase Email Configuration Steps

### Method 1: Using Supabase Built-in Email (Simple)

1. Go to Supabase Dashboard → Your Project → Email Templates
2. Enable Email Authentication
3. Customize email templates for:
   - Confirm signup email
   - Magic link email
   - Password recovery email

**Limitation:** Supabase has rate limits on free tier (~3 emails/day)

### Method 2: Using External SMTP Provider (Recommended)

1. **Choose a provider:**
   - SendGrid (Free tier: 100 emails/day)
   - Mailgun (Free tier: 10k emails/month)
   - AWS SES
   - Custom SMTP server

2. **Configure in Supabase:**
   - Go to Supabase Dashboard → Project Settings → Auth
   - Scroll to "Email Configuration"
   - Select "Custom SMTP"
   - Enter SMTP credentials:
     ```
     SMTP Host: [provider's host]
     SMTP Port: [provider's port]
     SMTP User: [your email/username]
     SMTP Password: [your password/API key]
     From Email: [sender email address]
     From Name: [sender name, e.g., "CodeSense AI"]
     ```

3. **Test the configuration:**
   - Use Supabase's test feature to send test email
   - Check email delivery

### Method 3: Using Environment Variables (Advanced)

For backend-controlled email, add to backend `.env`:
```env
# Email Configuration
MAIL_HOST=smtp.provider.com
MAIL_PORT=587
MAIL_USERNAME=your-email@example.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=noreply@codesense-ai.com
```

Then implement custom email service in Spring Boot with JavaMailSender.

## Testing the Authentication Flow

### Test 1: Legacy Account Migration
1. Use old email/password (exists only in backend DB)
2. Expected flow:
   - Supabase sign-in fails
   - Backend legacy verification succeeds
   - Supabase account created
   - User is logged in

### Test 2: Magic Link Email
1. Click "Email me a verification link"
2. Enter email address
3. Check inbox for email
4. Click link to verify and login

**Troubleshooting:**
- Check spam/junk folder
- Verify email address is correct
- Wait 1-2 minutes for email delivery
- Check Supabase logs in dashboard for delivery errors

### Test 3: New User Registration
1. Create new account with email/password
2. Expected: Email verification required (or auto-confirmed based on config)
3. Login should work after verification

## Common Error Messages & Solutions

| Error | Cause | Solution |
|-------|-------|----------|
| "Error sending magic link email" | Email service misconfigured | Set up SMTP in Supabase dashboard |
| "Over request rate limit" | Too many requests | Wait a few minutes before retrying |
| "Account created but session not established" | Email confirmation required | Check email and click verification link |
| "Invalid credentials" | Wrong email/password | Verify credentials and retry |
| "User not found" | Account doesn't exist | Create account or check email |

## Environment Setup

### Frontend `.env.local`
```env
VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_ANON_KEY=your-anon-key
```

### Backend `.env` or `application.yml`
```properties
# For Spring Boot
spring.mail.host=smtp.provider.com
spring.mail.port=587
spring.mail.username=your-email@example.com
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

## Recommended Next Steps

1. ✅ **Fix deployed** - Session establishment improved
2. 📧 **Configure Email** - Set up SMTP in Supabase
3. 🧪 **Test** - Verify email flow works
4. 📝 **Document** - Keep email provider credentials secure

## Production Checklist

- [ ] Email provider credentials configured in Supabase
- [ ] Email templates customized with branding
- [ ] Test emails sent successfully
- [ ] Rate limits monitored
- [ ] Error handling in place
- [ ] User support docs created
- [ ] Email sender address verified with provider

## Resources

- [Supabase Email Configuration](https://supabase.com/docs/guides/auth/auth-email)
- [SendGrid Setup](https://sendgrid.com/)
- [Mailgun Setup](https://www.mailgun.com/)
- [AWS SES Setup](https://aws.amazon.com/ses/)
