# Image Processing Service

## Overview

The Image Processing Service is a Spring Boot application that provides functionalities for uploading, transforming, and deleting images. It leverages AWS S3 for storage and the ImageJ library for image processing tasks such as resizing, cropping, rotating, and applying filters.

## Features

- **Upload Image**: Upload images to AWS S3 and store metadata in a MySQL database.
- **Transform Image**: Apply various transformations to images such as resize, crop, rotate, and filters.
- **Delete Image**: Delete images from AWS S3 and remove metadata from the MySQL database.
- **Authentication**: Authenticate users to access the API.
- **Rate Limiting**: Limit the number of requests to prevent abuse.

## Technologies Used

- **Spring Boot**: For building the RESTful API.
- **Spring Security**: For authentication.
- **JWT**: For token-based authentication.
- **AWS S3**: For storing images.
- **MySQL**: For storing image metadata.
- **ImageJ**: For image processing.
- **Lombok**: For reducing boilerplate code.
- **Jakarta Transactions**: For managing transactions.
- **Bucket4j**: For rate limiting.

## Prerequisites

- Java 11 or higher
- Maven
- AWS account with S3 bucket
- MySQL database

## Configuration

Configure the following properties in `application.properties`:

```properties
aws.s3.bucket=your-bucket-name
aws.s3.region=your-bucket-region
spring.datasource.url=jdbc:mysql://localhost:3306/your-database
spring.datasource.username=your-username
spring.datasource.password=your-password
```

## Endpoints

### Login

**Endpoint**: `POST /login`

**Request**: `application/json`

**Headers**:

- `Content-Type`: application/json

**Body**:

```json
{
  "username": "your-username",
  "password": "your-password"
}
```

**Response**:

- `200 OK`: Returns the JWT token.

### Register

**Endpoint**: `POST /register`

**Request**: `application/json`

**Headers**:

- `Content-Type`: application/json

**Body**:

```json
{
  "username": "your-username",
  "password": "your-password"
}
```

**Response**:

- `200 OK`: Returns a success message.

### Upload Image

**Endpoint**: `POST /images`

**Request**: `multipart/form-data`

**Headers**:

- `Authorization`: Bearer token

**Parameters**:

- `file`: The image file to upload.

**Response**:

- `200 OK`: Returns the image metadata and the image S3 URL.

### Transform Image

**Endpoint**: `POST /images/{id}/transform`

**Request**: `application/json`

**Headers**:

- `Authorization`: Bearer token

**Parameters**:

- `id`: The ID of the image to transform.
- `transformations`: A JSON object specifying the transformations to apply.

#### Example JSON for Transformations

```json
{
  "transformations": {
    "resize": {
      "width": 800,
      "height": 600
    },
    "crop": {
      "x": 100,
      "y": 50,
      "width": 400,
      "height": 300
    },
    "rotate": 90,
    "filters": {
      "grayscale": true,
      "sepia": true
    },
    "format": "png"
  }
}
```

**Response**:

- `200 OK`: Returns the transformed image metadata and the image S3 URL.

### Get Image

**Endpoint**: `GET /images/{id}`

**Request**: `application/json`

**Headers**:

- `Authorization`: Bearer token

**Parameters**:

- `id`: The ID of the image to get.

**Response**:

- `200 OK`: Returns the image metadata and the image S3 URL.

### Delete Image

**Endpoint**: `DELETE /images/{id}`

**Request**: `application/json`

**Headers**:

- `Authorization`: Bearer token

**Parameters**:

- `id`: The ID of the image to delete.

**Response**:

- `200 OK`: Returns a success message.

## Rate Limiting

Rate limiting is implemented using the `Bucket4j` library to prevent abuse and ensure fair usage of the API. Each IP address is limited to 10 requests per minute.

## Running the Application

1. Clone the repository.
2. Configure the application properties.
3. Build the project using Maven:
   ```sh
   mvn clean install
   ```
4. Run the application:
   ```sh
   mvn spring-boot:run
   ```

## License

This project is licensed under the MIT License.

### Source - [Challenge](https://roadmap.sh/projects/image-processing-service)
