#!/usr/bin/env bash
set -e

KEYSTORE_PATH="app/geotify-upload-key.keystore"
ALIAS="geotify-key"

echo "=== Geotify Release Signing Setup ==="

# 1. Locate keytool
if command -v keytool &> /dev/null; then
    KEYTOOL_CMD="keytool"
elif [ -f "/home/arrase/android-studio/jbr/bin/keytool" ]; then
    KEYTOOL_CMD="/home/arrase/android-studio/jbr/bin/keytool"
elif [ -n "$JAVA_HOME" ] && [ -f "$JAVA_HOME/bin/keytool" ]; then
    KEYTOOL_CMD="$JAVA_HOME/bin/keytool"
else
    echo "Error: keytool command not found. Please install JDK or ensure it is in your PATH."
    exit 1
fi

# 2. Ask for password
read -s -p "Enter password for the Release Keystore: " PASSWORD
echo
read -s -p "Confirm password: " PASSWORD_CONFIRM
echo

if [ "$PASSWORD" != "$PASSWORD_CONFIRM" ]; then
    echo "Error: Passwords do not match!"
    exit 1
fi

if [ -z "$PASSWORD" ]; then
    echo "Error: Password cannot be empty!"
    exit 1
fi

# 3. Generate Keystore if it doesn't exist
if [ -f "$KEYSTORE_PATH" ]; then
    echo "Keystore already exists at $KEYSTORE_PATH."
    read -p "Do you want to overwrite it? (y/N): " OVERWRITE
    if [[ "$OVERWRITE" =~ ^[Yy]$ ]]; then
        rm "$KEYSTORE_PATH"
        GENERATE=true
    else
        GENERATE=false
    fi
else
    GENERATE=true
fi

if [ "$GENERATE" = true ]; then
    echo "Generating new keystore at $KEYSTORE_PATH..."
    "$KEYTOOL_CMD" -genkey -v \
      -keystore "$KEYSTORE_PATH" \
      -alias "$ALIAS" \
      -keyalg RSA \
      -keysize 2048 \
      -validity 10000 \
      -storetype PKCS12 \
      -storepass "$PASSWORD" \
      -keypass "$PASSWORD" \
      -dname "CN=Geotify Developer, O=Geotify, C=ES"
    echo "Keystore generated successfully!"
fi

# 4. Store passwords in KWallet via D-Bus
echo "Storing passwords in KWallet..."
HANDLE=$(gdbus call --session --dest org.kde.kwalletd6 --object-path /modules/kwalletd6 --method org.kde.KWallet.open kdewallet 0 "Geotify" | tr -d '(), ')

if [ -z "$HANDLE" ] || [ "$HANDLE" -eq 0 ]; then
    echo "Error: Failed to open KWallet."
    exit 1
fi

# Create Geotify folder if it doesn't exist
gdbus call --session --dest org.kde.kwalletd6 --object-path /modules/kwalletd6 --method org.kde.KWallet.createFolder "$HANDLE" "Geotify" "Geotify" > /dev/null

# Write keystore password
gdbus call --session --dest org.kde.kwalletd6 --object-path /modules/kwalletd6 --method org.kde.KWallet.writePassword "$HANDLE" "Geotify" "keystore_password" "$PASSWORD" "Geotify" > /dev/null

# Write key password
gdbus call --session --dest org.kde.kwalletd6 --object-path /modules/kwalletd6 --method org.kde.KWallet.writePassword "$HANDLE" "Geotify" "key_password" "$PASSWORD" "Geotify" > /dev/null

# Sync wallet to persist changes to disk
gdbus call --session --dest org.kde.kwalletd6 --object-path /modules/kwalletd6 --method org.kde.KWallet.sync "$HANDLE" "Geotify" > /dev/null

# Close wallet
gdbus call --session --dest org.kde.kwalletd6 --object-path /modules/kwalletd6 --method org.kde.KWallet.close kdewallet false > /dev/null

echo "Passwords successfully saved in KWallet folder 'Geotify'!"
echo "Configuration complete."
