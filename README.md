<div align="center">
  <img src="https://github.com/user-attachments/assets/9dd6e7ac-f5b7-4e1c-a4d7-ffd452402254" alt="SafeShare Logo" width="120" style="margin-bottom: 20px;">

  # SafeShare — Secure File Sharing Platform

  <p>
    <strong>A highly secure, robust, and beautiful file-sharing application built with Spring Boot and Vanilla JS.</strong>
  </p>

  <p>
    <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java" />
    <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot" />
    <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL" />
    <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis" />
    <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
  </p>
</div>

---

## 📖 About the Project

**SafeShare** is a self-hosted, enterprise-grade file sharing platform. It empowers users to upload files and generate highly secure share links with absolute control over access. Unlike standard cloud storage, SafeShare focuses on temporary, highly-tracked, and restricted data transmission. 

Whether you need to share a confidential PDF with a client or send a secure ZIP file that self-destructs after a single download, SafeShare handles it seamlessly with a beautiful, responsive user interface.

---

## ✨ Key Features

- 🔐 **Advanced Security:** Stateless JWT authentication and Google OAuth2 integration.
- 🔗 **Secure Share Links:** Generate links with custom passwords, maximum download limits, and exact expiration dates.
- 💧 **Dynamic PDF Watermarking:** Optionally overlay an un-removable, transparent watermark containing the downloader's IP address and timestamp directly onto shared PDFs.
- 📊 **Real-Time Analytics:** Track exactly when, where (IP address, Browser, Device), and how a file was accessed via the Access Logs dashboard.
- ⚡ **Atomic Rate Limiting:** Built with Redis and Bucket4j to prevent race conditions during concurrent downloads and to block brute-force password guessing.
- 🤖 **Bot Detection:** Intelligent User-Agent filtering allows sharing on WhatsApp/Telegram/Slack (to render link previews) without wasting your maximum download limits.
- 📱 **Responsive UI:** A premium, desktop-first and mobile-friendly vanilla HTML/CSS/JS frontend with drag-and-drop uploading and inline file previews.

---

## 📸 Screenshots

### 1. Landing Page & Authentication
<img src="https://github.com/user-attachments/assets/a4591767-40b3-4988-a2a2-51f15e500779" width="700" alt="Landing Page" style="border-radius: 8px; margin-bottom: 10px; border: 1px solid #ccc;">
<br>
<img src="https://github.com/user-attachments/assets/e37502fe-625c-4c28-8bd6-88d5acfa3e29" width="700" alt="Authentication" style="border-radius: 8px; border: 1px solid #ccc;">

### 2. User Dashboard & File Management
<img src="https://github.com/user-attachments/assets/7b186dfa-96d4-4ff0-8c5e-ca5f3fcf2a9d" width="700" alt="Dashboard" style="border-radius: 8px; border: 1px solid #ccc;">

### 3. Share Link Generation Modal
<img src="https://github.com/user-attachments/assets/a420b7f1-8437-48c0-882a-2c291e2af121" width="700" alt="Share Modal" style="border-radius: 8px; border: 1px solid #ccc;">

### 4. Public File Access (Password Protected)
<img src="https://github.com/user-attachments/assets/eb9bae72-d8d5-4377-9ee6-f61a979a76af" width="700" alt="Public Access Page" style="border-radius: 8px; border: 1px solid #ccc;">

### 5. Preview Page
<img src="https://github.com/user-attachments/assets/16dbb1c3-925b-479b-9930-1632f4a59dd4" width="700" alt="Preview Page" style="border-radius: 8px; border: 1px solid #ccc;">

---

## 🛠️ Technology Stack

### Backend
- **Java 17** & **Spring Boot 3**
- **Spring Security** (JWT + OAuth2 Client)
- **Spring Data JPA** (Hibernate)
- **Redis** (Data caching & Distributed Atomic Counters)
- **Bucket4j** (API Rate Limiting)
- **Apache PDFBox** (Document manipulation & Watermarking)

### Frontend
- **Vanilla HTML5 & CSS3** (Custom design system, No CSS Frameworks)
- **Vanilla JavaScript (ES6)** (Modular structure, Custom API wrapper)

### Infrastructure
- **MySQL 8** (Primary Relational Database)
- **Docker & Docker Compose** (Containerization)

---

## 🚀 Getting Started (Local Development)

To run SafeShare locally on your machine, follow these steps:

### Prerequisites
- [Java 17](https://adoptium.net/) installed
- [Maven](https://maven.apache.org/) installed
- [Docker & Docker Compose](https://www.docker.com/) installed

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/SafeShare.git
cd SafeShare
```

### 2. Start the Database and Redis
We have provided a `docker-compose.yml` to instantly spin up MySQL and Redis locally.
```bash
docker-compose up -d
```

### 3. Configure Local Credentials
Create a `src/main/resources/application-local.properties` file to store your real passwords safely (this file is ignored by git).
```properties
spring.datasource.password=your_mysql_password
app.jwt.secret=YourSuperSecretKeyGoesHere!!
spring.security.oauth2.client.registration.google.client-id=your_client_id
spring.security.oauth2.client.registration.google.client-secret=your_client_secret
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
```

### 4. Run the Application
Run the Spring Boot application using the Maven wrapper:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### 5. Access the App
- **Web Application:** `http://localhost:8080`
- **Swagger API Docs:** `http://localhost:8080/swagger-ui.html`

---

## ☁️ Deployment (Railway)

SafeShare is heavily optimized for zero-config deployments on **Railway**. 

1. Push your repository to GitHub.
2. Connect your GitHub repository to a new Railway project.
3. Add a **MySQL** and **Redis** plugin in your Railway environment.
4. Set the `SPRING_PROFILES_ACTIVE` environment variable to `prod`.
5. Add your credentials (`DB_PASSWORD`, `JWT_SECRET`, etc.) to the Railway **Variables** tab.
6. **Important:** Add a Railway **Volume** mounted to `/app/uploads` and set `APP_UPLOAD_DIR=/app/uploads` so user files persist across deployments.

Railway will automatically build and deploy the application flawlessly!

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---
<div align="center">
  <b>Built with ❤️ for secure data sharing.</b>
</div>
