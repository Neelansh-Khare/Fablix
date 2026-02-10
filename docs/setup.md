# Fabflix Local Setup Guide (Windows)

This guide will walk you through setting up and running the Fabflix project on your local Windows machine.

## 1. Prerequisites

You'll need to install the following software:

*   **Java Development Kit (JDK):** The project is configured to use Java 23, but we recommend using **JDK 17 (LTS)** for better compatibility with Tomcat. You can download it from [Oracle](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) or use an open-source distribution like [Eclipse Temurin](https://adoptium.net/).
*   **Apache Maven:** This project uses Maven for dependency management and building. Download it from the [Maven website](https://maven.apache.org/download.cgi) and follow their installation instructions.
*   **MySQL Server:** The application uses a MySQL database. We recommend **MySQL 8.0+**. You can install it using the [MySQL Installer for Windows](https://dev.mysql.com/downloads/installer/).
*   **Apache Tomcat:** This will act as our web server and servlet container. We recommend **Tomcat 9.x**. Download it from the [Tomcat 9 website](https://tomcat.apache.org/download-90.cgi).

## 2. Database Setup

1.  **Create the Database:**
    *   Open the MySQL Command Line Client or your preferred MySQL GUI (like MySQL Workbench).
    *   Create a new database for the project.

    ```sql
    CREATE DATABASE moviedb;
    ```

2.  **Run the SQL Scripts:**
    *   Connect to your newly created `moviedb` database.
    *   Execute the SQL scripts located in the root of the project to create the schema and populate the database with initial data. The execution order might be important. A common order would be schema first, then data.
        *   `movie-data.sql`
        *   `sample_data.sql`

## 3. Project Configuration

1.  **Database Connection:**
    *   Open the file `src/main/resources/db.properties`.
    *   Update the placeholder values with your MySQL database information.

    ```properties
    # Database Configuration
    db.driver=com.mysql.cj.jdbc.Driver
    db.url=jdbc:mysql://localhost:3306/moviedb?autoReconnect=true&useSSL=false
    db.username=your_mysql_username
    db.password=your_mysql_password
    db.min_connections=5
    db.max_connections=10
    ```

## 4. Build the Project

Open a terminal or command prompt in the root directory of the Fabflix project and run the following Maven command. This will download all the necessary dependencies and package the application into a `.war` file.

```bash
mvn clean install
```

This will create a `fabflix.war` file in a new `target` directory.

## 5. IDE Setup (IntelliJ IDEA)

1.  **Add Tomcat Server:**
    *   Go to `File` -> `Settings` -> `Build, Execution, Deployment` -> `Application Servers`.
    *   Click the `+` icon and select `Tomcat Server`.
    *   Set the `Tomcat Home` to the directory where you extracted Tomcat.

2.  **Create Run Configuration:**
    *   Go to `Run` -> `Edit Configurations...`.
    *   Click the `+` icon and select `Tomcat Server` -> `Local`.
    *   Give the configuration a name (e.g., "Fablix on Tomcat").
    *   On the `Server` tab, ensure the Application Server is set to the Tomcat server you just configured.
    *   Click the `Deployment` tab.
    *   Click the `+` icon, select `Artifact...`, and choose `fabflix:war exploded`.
    *   In the `Application context` field, set the path to `/fabflix`. This will make your application accessible at `http://localhost:8080/fabflix`.

## 6. Run the Application

1.  **Start the Server:**
    *   Select your "Fablix on Tomcat" run configuration from the dropdown in the toolbar.
    *   Click the green 'Run' button (or press `Shift` + `F10`).
    *   IntelliJ will build the project, start Tomcat, and deploy the application.

2.  **Access the Application:**
    *   Open your web browser and navigate to: `http://localhost:8080/fabflix`

You should now see the Fabflix application running.
