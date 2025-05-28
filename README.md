# Organization-Management

## Overview 
The Organization Management component is intended to streamline the onboarding and engagement of organizations (both providers and consumers) within the Tec4MaaSEs system. This component manages the complete lifecycle of an organization’s involvement in the Tec4MaaSEs platform, including initial registration, data access provisioning, service request management, and historical transaction tracking.

It is based on Java Spring Boot framework utilizing Java 17. 

## Table of Contents
1. [Installation](#installation)
2. [Usage](#usage)
3. [Deployment](#deployment)
4. [License](#license)
5. [Contributors](#contributors)

### Installation

1. Clone the repository:

    git clone git@bitbucket.org:atc-code/ilab-tec4maases-organisation-manager.git
    cd ilab-tec4maases-organisation-manager
    ```

2. Install the dependencies:

    mvn install



4. If deployed through Docker Compose file the following Environmental Variables must be defined:

    

### Usage

1. Run the application after Keycloak is deployed:

    ```sh
    mvn spring-boot:run
    ```

2. The application will start on `http://localhost:8090`.

3. Access the OpenAPI documentation at `http://localhost:8090/api/organization/swagger-ui/index.html`.

### Deployment

For local deployment Docker containers can be utilized to deploy the microservice with the following procedure:

1. Ensure Docker is installed and running.

2. Build the maven project:

    ```sh
    mvn package
    ```

3. Build the Docker container:

    ```sh
    docker build -t organization-manager .
    ```

4. Run the Docker container including the environmental variables:

    ```sh
    docker run -d -p 8090:8090 --name organization-manager organization-manager
    ```

5. To stop container run:

    ```sh
   docker stop organization-manager
    ```

## License

This project has received funding from the European Union's Horizon 2023 research and innovation program, under Grant Agreement 101138517.

For more details about the licence, see the [LICENSE](LICENSE) file.

## Contributors

- Christina Vaia (<c.vaia@atc.gr>)
