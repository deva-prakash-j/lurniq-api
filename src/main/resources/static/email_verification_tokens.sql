-- Create table for email verification tokens
CREATE TABLE email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_email_verification_token_user 
        FOREIGN KEY (user_id) REFERENCES users(id) 
        ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_email_verification_tokens_token ON email_verification_tokens(token);
CREATE INDEX idx_email_verification_tokens_user_id ON email_verification_tokens(user_id);
CREATE INDEX idx_email_verification_tokens_expiry_date ON email_verification_tokens(expiry_date);
CREATE INDEX idx_email_verification_tokens_used ON email_verification_tokens(used);

-- Create partial index for active (unused and non-expired) tokens
CREATE INDEX idx_email_verification_tokens_active 
    ON email_verification_tokens(user_id, expiry_date) 
    WHERE used = false;

-- Add comments for documentation
COMMENT ON TABLE email_verification_tokens IS 'Stores email verification tokens for user account activation';
COMMENT ON COLUMN email_verification_tokens.id IS 'Primary key, auto-generated';
COMMENT ON COLUMN email_verification_tokens.token IS 'Unique verification token sent to user email';
COMMENT ON COLUMN email_verification_tokens.user_id IS 'Foreign key reference to users table';
COMMENT ON COLUMN email_verification_tokens.expiry_date IS 'Token expiration timestamp (24 hours from creation)';
COMMENT ON COLUMN email_verification_tokens.used IS 'Flag indicating if token has been used for verification';
COMMENT ON COLUMN email_verification_tokens.created_at IS 'Timestamp when token was created';