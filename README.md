# Remitly Internship Project (2025)

## Project Overview
This project is a solution for the Remitly Internship home exercise, focusing on creating a REST API for parsing, storing, and managing SWIFT codes.

## Exercise Description
The goal is to develop an application that:
- Parses SWIFT codes from a provided spreadsheet
- Stores SWIFT code data in a database
- Provides RESTful endpoints for:
  1. Retrieving details of a single SWIFT code
  2. Fetching all SWIFT codes for a specific country
  3. Adding new SWIFT code entries
  4. Deleting SWIFT code entries

## Features
- REST API for JSON/Excel file processing
- Multi-stage Docker build with JDK 21
- Isolated environments: production & testing
- PostgreSQL database cluster with:
  - Main database for application
  - Dedicated test database with automatic cleanup
- Integration with pgAdmin4 for database management
- Apache POI for Excel file validation and processing
- Comprehensive test suite covering:
  - Edge cases handling
  - Integration testing with dedicated test database
  - Input validation scenarios
- Custom Exception handling 
- RESTful endpoints as specified in the exercise:
  - Retrieve single SWIFT code details (headquarter with branches/branch)
  - Fetch all SWIFT codes by country
  - Add new SWIFT code entries
  - Delete existing SWIFT codes


## Data Initialisation: 
The application automatically processes: "src/main/resources/data/Interns_2025_SWIFT_CODES.xlsx" 
during application start, because of @PostConstruct annotation. 

Ensure your Excel file follows the format:
* It must contain the following columns:
  * COUNTRY ISO2 CODE
  * SWIFT CODE
  * NAME 
  * COUNTRY NAME
* Columns can appear in any order.
* SWIFT CODE must be exactly 11 characters long (codes ending in "XXX" indicate headquarters).
* COUNTRY ISO2 CODE must be exactly 2 characters long.

### Important!! 
* SwiftCode model is representing a BANK


## Prerequisites

### For Docker Usage (Recommended):
- **Docker (Docker Desktop)**
  - [Download Docker Desktop](https://www.docker.com/products/docker-desktop)
- Git

### For Local Development
- Java 21
- Maven 3.9+
- Spring Boot 3.4.3
- PostgreSQL 15+

### Optional
- **PGAdmin 4**  
  [Download pgAdmin](https://www.pgadmin.org/download/)


## Setup Instructions 

### Clone Repository 
* git clone https://github.com/adamzulu123/RemitlyInternship
* cd RemitlyInternship

### Build and Start Application
* (Ensure Docker Desktop is running)
* docker-compose up --build app postgres pgadmin -d

#### Additional Info: 
Application is prepared for docker not local testing,
so if you want to test it and develop locally you need to change
application.yml file:

* Replace postgres with localhost in datasource URL
* Ensure local PostgreSQL is running with matching credentials


### Run Tests
* docker-compose up --build test postgres-test -d

#### Or use Postman or Curl to test endpoints. 

#### Key Test Features: 
* Average execution time: 4-5 minutes (initial build)
* Separate test database container
* Test container self-destructs after completion

### Stop all services: 
* docker-compose down -v


## Services 

#### Application: 
* http://localhost:8080

#### Database Management: (PgAdmin)
* URL: http://localhost:5050
* Email: admin@example.com
* Password: admin

#### Database Ports:
* Main Database (PostgreSQL): 5432
* Test Database (PostgreSQL for integration tests): 5433


## Troubleshooting
* Ensure Docker Desktop is running
* Check that ports 8080, 5432, and 5050 are available
* Verify network connectivity
* Review Docker logs for specific error messages




