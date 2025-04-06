## Task description
Here is [a glitchy HTTP server written in Python](/server.py). The server sends some randomized data on every GET request, but often it doesn’t send the whole data, but just a part of the data. Luckily, the server supports the HTTP header “Range”.

Run the server in a terminal.

You need to write a client application that downloads the binary data from the glitchy server. To ensure that the downloaded data is correct, check the SHA-256 hash of the downloaded data. The hash must be the same as the HTTP server writes into the terminal.

Write the client app either in Kotlin+JVM or in Rust.

If possible, avoid using external libraries.

You may use an LLM for writing code if you wish, but keep in mind that it’s you who are responsible for the code you deliver, not the LLM. Bugs made by the LLM are your bugs.


## Components

### Client (`GlitchyClient.kt`)

A streamlined Kotlin client that:
- Determines file size using Content-Length headers
- Downloads data in manageable 64KB chunks
- Dynamically reduces chunk size when failures occur
- Tracks download progress
- Verifies data integrity with SHA-256 hash comparison

### Server (`server.py`)

The Python HTTP server provided with this project:
- Generates random binary data on startup
- Displays SHA-256 hash of the generated data
- Supports Range requests
- Intentionally introduces unpredictable behavior

## Usage

### Running the Server

```bash
python server.py
```

### Running the Client

```bash
# Compile the Kotlin code
kotlinc GlitchyClient.kt -include-runtime -d GlitchyClient.jar

# Run the client
java -jar GlitchyClient.jar
```

The client will display:
- Download progress percentage
- Total bytes downloaded
- Final SHA-256 hash of the downloaded data

### Verification

To verify successful download, compare the SHA-256 hash displayed by:
1. The server (when started)
2. The client (upon download completion)

These hashes should match, confirming that despite the server's unreliable behavior, the client has successfully downloaded the complete, uncorrupted data.

## Requirements

- Kotlin & JVM 1.8+
- Python 3.6+ (for the server)
- No external dependencies required
