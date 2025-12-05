# FormaFlux 🚀

FormaFlux is a powerful and intuitive file conversion tool designed to transform XML data into structured JSON with ease. It goes beyond simple 1:1 conversion by offering advanced mapping capabilities, allowing users to restructure, rename, and nest fields dynamically.

## ✨ Features

- **Normal Conversion**: Instantly convert XML files to JSON with a direct mapping.
- **Advanced Conversion**:
    - **Custom Mapping**: Map source XML fields to custom target JSON keys.
    - **Structural Transformation**: Create nested JSON objects using dot notation (e.g., `book[].details.title`).
    - **Array Handling**: Intelligent handling of arrays and lists during transformation.
- **Modern UI**: A sleek, dark-themed dashboard built for a seamless user experience.
- **Real-time Analysis**: Automatically analyzes uploaded files to generate a schema for mapping.

## 🛠️ Tech Stack

- **Backend**: Java, Spring Boot
- **Frontend**: Thymeleaf, Vanilla JavaScript, CSS3
- **Build Tool**: Maven
- **Libraries**: Jackson (XML & JSON processing), Lombok

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- Maven

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/Avinash6643/formaflux.git
   ```
2. Navigate to the project directory:
   ```bash
   cd formaflux
   ```
3. Build the project:
   ```bash
   ./mvnw clean install
   ```

### Running the Application

1. Start the Spring Boot application:
   ```bash
   ./mvnw spring-boot:run
   ```
2. Open your browser and navigate to:
   ```
   http://localhost:8080
   ```

## 💡 How It Works

1. **Upload**: Select your XML file.
2. **Analyze**: The app parses the file to extract its structure.
3. **Map (Advanced Mode)**: Define your target structure. Use `parent.child` syntax to create nested objects.
4. **Convert**: Download your transformed JSON file.

## 🧠 Core Logic

The advanced conversion engine uses a **Flatten-Remap-Unflatten** strategy:
1. **Flatten**: The hierarchical XML structure is flattened into a map of paths (e.g., `library.book[0].title`).
2. **Remap**: User-defined mappings are applied to these paths, supporting index preservation for arrays.
3. **Unflatten**: The remapped paths are reconstructed into a new, nested JSON structure.

---

Built with ❤️ by Avinash Rapolu
