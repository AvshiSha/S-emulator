# S-Server (web-demo)

REST API server for S-Emulator. Runs on **Tomcat 10.1.26**.

## 📦 Structure

```
web-demo/ (s-server)
├── pom.xml                    # Maven WAR config
├── src/
│   └── com/semulator/server/
│       ├── api/               # REST endpoints (Servlets)
│       │   └── EngineServlet.java
│       └── service/           # Business logic layer
│           └── EngineService.java
└── web/
    ├── index.html             # Welcome page
    └── WEB-INF/
        └── web.xml            # Jakarta EE 10 config
```

## 🚀 Running

### IntelliJ IDEA + Tomcat

1. **Configure Tomcat:**
   - **Run** → **Edit Configurations** → **+** → **Tomcat Server** → **Local**
   - **Application Server:** Select Tomcat 10.1.26
   - **Deployment tab:**
     - **+** → **Artifact** → Select `s-server:war exploded`
     - **Application context:** `/s-emulator`
2. **Run:**
   - Click **▶ Run** (or Debug)
   - Server starts at: `http://localhost:8080/s-emulator`

### Build WAR

```bash
cd web-demo
mvn clean package
# Creates: target/s-emulator.war
```

Deploy `s-emulator.war` to Tomcat `webapps/` folder.

## 🌐 API Endpoints

Base URL: `http://localhost:8080/s-emulator/api`

### Engine Operations

- `GET /engine` - Health check
- `GET /engine/catalog` - List programs & functions
- `POST /engine/load` - Load XML program
- `POST /engine/run` - Execute program/function

### Future Endpoints (TODO)

- `GET /users/{username}` - User info & credits
- `POST /debug/step` - Debug step
- `GET /runs/{runId}/status` - Run status

## 🔧 Dependencies

- **s-engine-core** - Core S-emulator logic
- **Jakarta Servlet API 6.0** - For Tomcat 10
- **Gson 2.10.1** - JSON serialization

## ✅ Step 1 Complete

The server module is ready with:

- ✅ Maven WAR build configuration
- ✅ Basic servlet structure
- ✅ Engine service wrapper
- ✅ Jakarta EE 10 (Tomcat 10 compatible)
- ✅ REST endpoint stubs

**Next:** Implement full REST API methods and create JavaFX client!
