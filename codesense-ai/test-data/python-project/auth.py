"""
Sample Python project for CodeSense AI parser tests.
User authentication module.
"""

import hashlib
import secrets
from datetime import datetime


class User:
    """Represents an authenticated user."""

    def __init__(self, user_id: int, username: str, email: str):
        self.user_id = user_id
        self.username = username
        self.email = email
        self.created_at = datetime.now()

    def to_dict(self):
        return {
            "id": self.user_id,
            "username": self.username,
            "email": self.email
        }


class AuthService:
    """Handles user authentication and session management."""

    def __init__(self, user_repository):
        self.user_repository = user_repository
        self._sessions = {}

    def login(self, username: str, password: str) -> str:
        """Authenticate user and return session token."""
        user = self.user_repository.find_by_username(username)
        if not user:
            raise ValueError("User not found")

        if not self._verify_password(password, user.password_hash):
            raise ValueError("Invalid credentials")

        token = secrets.token_hex(32)
        self._sessions[token] = user.user_id
        return token

    def logout(self, token: str) -> None:
        """Invalidate session token."""
        self._sessions.pop(token, None)

    def get_current_user(self, token: str) -> User:
        """Get user from session token."""
        user_id = self._sessions.get(token)
        if not user_id:
            raise ValueError("Invalid or expired session")
        return self.user_repository.find_by_id(user_id)

    @staticmethod
    def _verify_password(password: str, password_hash: str) -> bool:
        return hashlib.sha256(password.encode()).hexdigest() == password_hash

    @staticmethod
    def hash_password(password: str) -> str:
        return hashlib.sha256(password.encode()).hexdigest()
