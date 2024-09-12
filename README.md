# Image Processing Service

## Overview

The Image Processing Service is a Spring Boot application that provides functionalities for uploading, transforming, and deleting images. It leverages AWS S3 for storage and the ImageJ library for image processing tasks such as resizing, cropping, rotating, and applying filters.

## Features

- **Upload Image**: Upload images to AWS S3 and store metadata in a MySQL database.
- **Transform Image**: Apply various transformations to images such as resize, crop, rotate, and filters.
- **Delete Image**: Delete images from AWS S3 and remove metadata from the MySQL database.

## Technologies Used

- **Spring Boot**: For building the RESTful API.
- **AWS S3**: For storing images.
- **MySQL**: For storing image metadata.
- **ImageJ**: For image processing.
- **Lombok**: For reducing boilerplate code.
- **Jakarta Transactions**: For managing transactions.

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

### Upload Image

**Endpoint**: `POST /images`

**Request**: `multipart/form-data`

**Parameters**:
- `file`: The image file to upload.

**Response**:
- `200 OK`: Returns the image metadata.

### Transform Image

**Endpoint**: `POST /images/{id}/transform`

**Request**: `application/json`

**Parameters**:
- `id`: The ID of the image to transform.
- `transformations`: A JSON object specifying the transformations to apply.

**Response**:
- `200 OK`: Returns the transformed image metadata.

### Delete Image

**Endpoint**: `DELETE /images/{id}`

**Request**: `application/json`

**Parameters**:
- `id`: The ID of the image to delete.

**Response**:
- `200 OK`: Returns a success message.

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

