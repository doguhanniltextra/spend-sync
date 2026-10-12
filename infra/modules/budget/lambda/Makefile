.PHONY: build clean

# Build the Go binary for AWS Lambda (linux/arm64 — AWS Graviton)
# The binary MUST be named 'bootstrap' for the provided.al2023 custom runtime.
build:
	GOOS=linux GOARCH=arm64 CGO_ENABLED=0 \
		go build -tags lambda.norpc -ldflags="-s -w" -o bootstrap budget_handler.go
	zip -j budget_handler.zip bootstrap

clean:
	rm -f bootstrap budget_handler.zip
