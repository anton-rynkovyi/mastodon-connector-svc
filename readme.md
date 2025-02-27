# Application Setup

## Quick Start with Docker Compose

If you want to run the application quickly, follow these steps:

1. Run the following command to start the application with Docker Compose:

    ```bash
    docker-compose up
    ```

2. Once the services are up, you can start making API calls to:

    ```
    http://localhost:8081/posts/stream
    ```

## Running Locally from Your IDE

If you prefer to run the application locally from your IDE, follow these steps:

1. Set up a 'local' profile in your IDE.
2. Remove the `mastodon-connector-svc` and `post-gateway-svc` services from the `docker-compose.yml` file.
3. Run the following command to start the application with Docker Compose:

    ```bash
    docker-compose up
    ```
4. Start these services manually from your local machine.
5. After setting up the services locally, you can run the application and make API calls as needed.
    ```
    http://localhost:8081/posts/stream
    ```