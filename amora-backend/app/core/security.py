import base64
import hashlib
import logging
import os
from cryptography.fernet import Fernet

logger = logging.getLogger("amora.security")

_DEFAULT_KEY = "amora_default_secret_key_32_bytes_len!"

# Derive a consistent 32-byte Fernet key using SHA-256 (safe for any input length)
_SECRET_KEY = os.getenv("ENCRYPTION_KEY", _DEFAULT_KEY)
if _SECRET_KEY == _DEFAULT_KEY:
    # This default is published in the source code, so anyone with the repo can
    # decrypt anything encrypted under it. Previously this failed silently —
    # set ENCRYPTION_KEY in your .env to a real random value.
    logger.warning(
        "ENCRYPTION_KEY is not set — falling back to the default key baked into the "
        "source code. Anything encrypted right now is NOT actually secure. Set a real "
        "ENCRYPTION_KEY in your .env (e.g. `python -c \"import secrets; print(secrets.token_urlsafe(32))\"`)."
    )
_KEY_BYTES = base64.urlsafe_b64encode(hashlib.sha256(_SECRET_KEY.encode()).digest())
_fernet = Fernet(_KEY_BYTES)

def encrypt_data(data: str) -> str:
    """Encrypts plaintext string into base64 ciphertext."""
    if not data:
        return ""
    return _fernet.encrypt(data.encode()).decode()

def decrypt_data(token: str) -> str:
    """Decrypts base64 ciphertext into plaintext string."""
    if not token:
        return ""
    try:
        return _fernet.decrypt(token.encode()).decode()
    except Exception:
        return token
