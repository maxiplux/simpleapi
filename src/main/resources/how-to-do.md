# Generate private key
openssl genrsa -out keypair.pem 2048

# Extract public key
openssl rsa -in keypair.pem -pubout -out public.pem

# Convert private key to PKCS8 format
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in keypair.pem -out private.pem


# Base64 encode the private key (remove newlines)
cat private.pem | base64 -w 0

# Base64 encode the public key (remove newlines)
cat public.pem | base64 -w 0