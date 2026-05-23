# TA-Recruitment-System
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

- **JDK**: Version 21 or higher (JDK 21+). Note: Please ensure the `JAVA_HOME` environment variable is correctly configured.
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

1. Deploy the WAR Package
Copy the generated `web.war` file into the `webapps` folder of your Tomcat installation directory:
`tomcat/webapps/`

2. Start the Service
Navigate to the `bin` directory of Tomcat and run the startup script to launch the service:
   - Windows: Double-click `startup.bat`
   - Linux/macOS: Execute `./startup.sh`

3. Access the Application
Once the service has started successfully, you can access your web application via a browser (the default port is usually 8080, url: http://localhost:8080/web/).