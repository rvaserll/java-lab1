#!/usr/bin/env python3
"""Local lab JWT signing only. Private keys stay outside Git and WireMock."""
import argparse
import base64
import json
from pathlib import Path
import secrets
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
LOCAL = ROOT / ".local-security"
PRIVATE_KEY = LOCAL / "private-key.pem"

def b64(value):
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode()

def local_environment():
    path = ROOT / ".env"
    if not path.exists():
        raise SystemExit("Create .env from .env.example and configure PostgreSQL first.")
    return dict(line.split("=", 1) for line in path.read_text().splitlines()
                if line and not line.startswith("#") and "=" in line)

def init():
    local_environment()
    LOCAL.mkdir(mode=0o700, exist_ok=True)
    if not PRIVATE_KEY.exists():
        subprocess.run(["openssl", "genpkey", "-algorithm", "RSA", "-pkeyopt", "rsa_keygen_bits:2048",
                        "-pkeyopt", "rsa_keygen_pubexp:65537", "-out", str(PRIVATE_KEY)],
                       check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    PRIVATE_KEY.chmod(0o600)
    modulus = subprocess.check_output(["openssl", "rsa", "-in", str(PRIVATE_KEY), "-noout", "-modulus"],
                                     stderr=subprocess.DEVNULL).decode().strip().split("=", 1)[1]
    public = {"kty": "RSA", "kid": "cosmo-cats-local", "use": "sig", "alg": "RS256",
              "n": b64(bytes.fromhex(modulus)), "e": "AQAB"}
    (LOCAL / "jwks.json").write_text(json.dumps({"keys": [public]}, indent=2) + "\n")
    path = ROOT / ".env"
    env = local_environment()
    defaults = {"MARKET_API_KEY": secrets.token_urlsafe(32), "JWKS_PORT": "8090",
                "JWT_ISSUER": "http://localhost:8090",
                "JWT_JWK_SET_URI": "http://localhost:8090/.well-known/jwks.json",
                "JWT_AUDIENCE": "cosmo-cats-api"}
    missing = {key: value for key, value in defaults.items() if not env.get(key) or env[key].startswith("replace-with-")}
    lines = [line for line in path.read_text().splitlines() if line.split("=", 1)[0] not in missing]
    lines.extend(f"{key}={value}" for key, value in missing.items())
    path.write_text("\n".join(lines) + "\n")
    path.chmod(0o600)
    print("Local RSA key, public JWKS and API key prepared. Secrets were not printed.")

def token(scope):
    env = local_environment()
    if not PRIVATE_KEY.exists():
        raise SystemExit("Run: python3 scripts/local_security.py init")
    now = int(time.time())
    header = {"alg": "RS256", "typ": "JWT", "kid": "cosmo-cats-local"}
    claims = {"iss": env.get("JWT_ISSUER", "http://localhost:8090"), "sub": "local-student",
              "aud": [env.get("JWT_AUDIENCE", "cosmo-cats-api")], "iat": now,
              "nbf": now, "exp": now + 900, "scope": scope}
    signing_input = (b64(json.dumps(header, separators=(",", ":")).encode()) + "." +
                     b64(json.dumps(claims, separators=(",", ":")).encode())).encode()
    signature = subprocess.check_output(["openssl", "dgst", "-sha256", "-sign", str(PRIVATE_KEY)],
                                        input=signing_input, stderr=subprocess.DEVNULL)
    print(signing_input.decode() + "." + b64(signature))

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["init", "token"])
    parser.add_argument("--scope", default="products:read products:write")
    args = parser.parse_args()
    if args.command == "init":
        init()
    else:
        token(args.scope)
