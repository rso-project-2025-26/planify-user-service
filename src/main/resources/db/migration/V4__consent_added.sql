ALTER TABLE auth.users ADD email_consent BOOLEAN DEFAULT TRUE;
ALTER TABLE auth.users ADD sms_consent BOOLEAN DEFAULT TRUE;
