# TA-Recruitment-System

## Project Overview

TA-Recruitment-System is an intelligent recruitment management system designed to streamline the hiring process for Teaching Assistants (TAs). The system leverages AI-powered resume parsing and skill matching to help administrators, managers, and TAs efficiently manage job postings, applications, and candidate evaluations.

### Key Features

- **AI-Powered Resume Analysis**: Automatic extraction of skills and experience from uploaded resumes using Alibaba Cloud Qwen models
- **Intelligent Candidate Matching**: Smart matching algorithm that evaluates candidates based on technical skills, soft skills, and experience
- **Multi-Role Access Control**: Separate interfaces for Administrators, Managers (MOs), Teaching Assistants (TAs), and applicants
- **Real-time Application Tracking**: Track application status and manage the recruitment workflow
- **Automated Profile Generation**: AI-generated candidate portraits for better evaluation

## Group Name-list

- xuanxuanzhu77733-dotcom: 2025010108 (Support TA)
- JFlame-9527: 231224376 (Lead)
- mei1234567554: 231224192 (Member)
- QhCrocus: 231224125 (Member)
- WYkkkl: 231224446 (Member)
- 477996850: 231224907 (Member)
- Xiri04: 231224491 (Member)

Software Engineering group project for `EBU6304`

## Prerequisites

Before you begin, please ensure that the following environments are installed and configured on your local machine or server:

- **JDK**: Version 21.0.0 or higher (JDK 21.+). Note: Please ensure the `JAVA_HOME` environment variable is correctly configured.
- **Tomcat**: Version 10 or higher (Tomcat 10.+)
- **Maven**: Version 3 or higher (Maven 3+)

## Configuration

Before packaging the project, you **must** configure the `apiKey` in the following files, otherwise the build may fail or the application may not run correctly:

1. Locate the following configuration files:
   
   - `src/main/resources/qwen_config.json`
   - `src/test/resources/qwen_config.json`

2. Fill in a valid apiKey in both files.
   
   - **Source**: The required key is the **Alibaba Cloud Bailian (Model Studio) Platform API Key**.
   - **Guide**: You can obtain the key from the [Bailian Console API Key Page](https://bailian.console.aliyun.com/cn-beijing?tab=model#/api-key).

### Configuration Files

The project uses two main configuration files located in src/main/resources/:

1. **config.json - Application Configuration**
   Controls the runtime environment and data management settings:
   
   ```json
   {
      "environment": "prod",
      "active": {
         "cleanData": false,
         "generateData": false
      },
      "paths": {
         "data": "data",
         "file": "upload"
      }
   }
   ```
   
   **Field Descriptions:**
   
   - `environment`: Environment mode - "prod" for production or "dev" for development
   - `cleanData`: Whether to clean existing data on startup (true/false)
   - `generateData`: Whether to generate test data on startup (true/false)
   - `paths.data`: Directory for storing application data (JSON files)
   - `paths.file`: Directory for storing uploaded files (resumes, documents, etc.)

2. **qwen_config.json - AI Model Configuration**
   Configures the Alibaba Cloud Qwen AI services for resume parsing and candidate matching:
   
   ```json
   {
     "apiKey": "your-apiKey",
     "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",      
     "qwen": {
       "model": "qwen3-max",
       "maxTokens": 512,
       "temperature": 0.0,
       "topP": 0.01,
       "topK": 1,        
       "repetitionPenalty": 1.1
     },
     "long": {
       "model": "qwen-long",
       "maxTokens": 1024,
       "temperature": 0.0,
       "topP": 0.01,
       "topK": 1,        
       "repetitionPenalty": 1.1
     },
     "vector": {
       "model": "text-embedding-v4",
       "dimension": 768
     },
     "weight": {
         "skills": 0.6,
        "softSkills": 0.25,
        "experience": 0.15
     }
   }
   ```

   **Field Descriptions:**

   - `apiKey`: Your Alibaba Cloud API Key (REQUIRED - must be replaced with actual key)
   - `baseUrl`: DashScope API endpoint URL
   - `qwen`: Standard Qwen model configuration for general tasks
     - `model`: Model name (qwen3-max)
     - `maxTokens`: Maximum token limit for responses
     - `temperature`: Randomness control (0.0 = deterministic)
     - `topP`: Nucleus sampling parameter
   - `long`: Qwen-Long model for processing long documents (resumes, cover letters)
   - `vector`: Vector embedding model for semantic search and similarity matching
   - `weight`: Candidate matching algorithm weights
     - `skills`: Technical skills weight (60%)
     - `softSkills`: Soft skills weight (25%)
     - `experience`: Work experience weight (15%)

## Building the Project

### Standard Build (Recommended)

After ensuring the `apiKey` is configured as described above, run the following Maven command in the project root directory to build the WAR package:

```bash
mvn package
```

Upon successful build, the generated WAR package will be located in the `target/` directory.

### Build Skipping Tests

If you are unable to configure the `apiKey` in `qwen_config.json` at the moment, you can skip the test phase during packaging by using the following parameter:

```bash
mvn package -Dmaven.test.skip=true
```

## Deployment & Running

### Step1: Deploy the WAR Package

Copy the generated `web.war` file into the `webapps` folder of your Tomcat installation directory:
`tomcat/webapps/`

### Step2: Start the Service

Navigate to the `bin` directory of Tomcat and run the startup script to launch the service:
   
- Windows: Double-click `startup.bat`
- Linux/macOS: Execute `./startup.sh`

### Step3: Access the Application

Once the service has started successfully, you can access your web application via a browser (the default port is usually 8080): http://localhost:8080/web/

### Step4: Runtime Configuration (Optional)

After the first startup, the WAR package will be automatically extracted to the `web` directory under `tomcat/webapps/`. You can modify the configuration files in the following location without repackaging:
`tomcat/webapps/web/config/`

This allows you to update settings such as API keys, model parameters, and other configurations directly in the deployed environment. Simply edit the JSON files and restart Tomcat for changes to take effect.
