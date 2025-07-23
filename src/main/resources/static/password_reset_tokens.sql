-- Create table for password reset tokens
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_password_reset_token_user 
        FOREIGN KEY (user_id) REFERENCES users(id) 
        ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_expiry_date ON password_reset_tokens(expiry_date);
CREATE INDEX idx_password_reset_tokens_used ON password_reset_tokens(used);

-- Create partial index for active (unused and non-expired) tokens
CREATE INDEX idx_password_reset_tokens_active 
    ON password_reset_tokens(user_id, expiry_date) 
    WHERE used = false;

-- Create composite index for user lookup with status filtering
CREATE INDEX idx_password_reset_tokens_user_status 
    ON password_reset_tokens(user_id, used, expiry_date);

-- Add comments for documentation
COMMENT ON TABLE password_reset_tokens IS 'Stores password reset tokens for secure password recovery';
COMMENT ON COLUMN password_reset_tokens.id IS 'Primary key, auto-generated';
COMMENT ON COLUMN password_reset_tokens.token IS 'Unique password reset token sent to user email';
COMMENT ON COLUMN password_reset_tokens.user_id IS 'Foreign key reference to users table';
COMMENT ON COLUMN password_reset_tokens.expiry_date IS 'Token expiration timestamp (1 hour from creation)';
COMMENT ON COLUMN password_reset_tokens.used IS 'Flag indicating if token has been used for password reset';
COMMENT ON COLUMN password_reset_tokens.created_at IS 'Timestamp when token was created';