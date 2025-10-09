# 🎨 JavaFX Client Architecture

## 📋 **Overview**

Create a **s-client** module that mirrors Exercise-2's UI but communicates **only via HTTP** with the s-server REST API.

## 🏗️ **Project Structure**

```
S-emulator/
├── s-engine-core/        # ✅ Complete (JAR)
├── s-server/            # ✅ Complete (WAR)
└── s-client/            # 🆕 JavaFX Client (JAR)
    ├── src/main/java/com/semulator/client/
    │   ├── Main.java                    # JavaFX Application entry point
    │   ├── app/                         # Application layer
    │   │   ├── AppController.java       # Main application controller
    │   │   ├── SceneManager.java        # Scene navigation
    │   │   └── AppState.java            # Global application state
    │   ├── service/                     # HTTP communication layer
    │   │   ├── ApiClient.java           # HTTP client wrapper
    │   │   ├── AuthService.java         # Authentication operations
    │   │   ├── ProgramService.java      # Program management
    │   │   ├── RunService.java          # Execution operations
    │   │   └── UserService.java         # User management
    │   ├── ui/                          # UI layer
    │   │   ├── controllers/             # FXML controllers
    │   │   │   ├── LoginController.java
    │   │   │   ├── DashboardController.java
    │   │   │   ├── ProgramEditorController.java
    │   │   │   ├── RunController.java
    │   │   │   └── DebugController.java
    │   │   ├── views/                   # FXML files
    │   │   │   ├── login.fxml
    │   │   │   ├── dashboard.fxml
    │   │   │   ├── program-editor.fxml
    │   │   │   ├── run.fxml
    │   │   │   └── debug.fxml
    │   │   ├── components/              # Reusable UI components
    │   │   │   ├── ProgramList.java
    │   │   │   ├── FunctionList.java
    │   │   │   ├── RunStatusPanel.java
    │   │   │   └── CreditDisplay.java
    │   │   └── styles/                  # CSS styling
    │   │       ├── main.css
    │   │       └── components.css
    │   ├── model/                       # Data models (mirror server API)
    │   │   ├── User.java
    │   │   ├── Program.java
    │   │   ├── RunSession.java
    │   │   └── ApiResponse.java
    │   └── util/                        # Utilities
    │       ├── JsonUtils.java
    │       ├── HttpUtils.java
    │       └── ObservableHelper.java
    ├── src/main/resources/
    │   ├── fxml/                        # FXML files
    │   ├── css/                         # CSS files
    │   └── images/                      # Icons/images
    └── pom.xml                          # Maven configuration
```

## 🔄 **Architecture Patterns**

### **1. Layered Architecture**

```
┌─────────────────────────────────────┐
│            UI Layer                 │  ← FXML Controllers, Views
├─────────────────────────────────────┤
│         Application Layer           │  ← AppController, SceneManager
├─────────────────────────────────────┤
│         Service Layer               │  ← HTTP API Communication
├─────────────────────────────────────┤
│          Model Layer                │  ← Data Models, DTOs
└─────────────────────────────────────┘
```

### **2. Service-Oriented Design**

- **AuthService**: Login, logout, token management
- **ProgramService**: Load XML, list programs/functions
- **RunService**: Execute programs, debug operations
- **UserService**: User management, credits

### **3. Reactive UI Updates**

- **ObservableCollections** for live data binding
- **Background threads** for HTTP calls
- **Polling mechanism** for real-time updates
- **Event-driven** UI updates

## 🔌 **HTTP Communication**

### **ApiClient (Singleton)**

```java
public class ApiClient {
    private static final String BASE_URL = "http://localhost:8080/s-emulator/api";
    private String authToken;

    public <T> CompletableFuture<T> get(String endpoint, Class<T> responseType);
    public <T> CompletableFuture<T> post(String endpoint, Object body, Class<T> responseType);
    public void setAuthToken(String token);
}
```

### **Service Layer Example**

```java
public class AuthService {
    public CompletableFuture<LoginResponse> login(String username) {
        LoginRequest request = new LoginRequest(username);
        return apiClient.post("/auth/login", request, LoginResponse.class);
    }
}
```

## 🎭 **UI Workflows**

### **1. Login Flow**

```
Login Screen → AuthService.login() → Dashboard
```

### **2. Program Management**

```
Dashboard → Load XML → ProgramService.upload() → Refresh Program List
```

### **3. Execution Flow**

```
Program Editor → RunService.prepare() → RunService.start() → Debug Screen
```

### **4. Debug Flow**

```
Debug Screen → RunService.step() → Update UI → Poll Status
```

## 🔄 **Real-Time Updates**

### **Polling Strategy**

```java
public class PollingManager {
    private ScheduledExecutorService scheduler;

    public void startPolling(String endpoint, Duration interval) {
        scheduler.scheduleAtFixedRate(() -> {
            // Poll for updates
            // Update UI on JavaFX thread
        }, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
    }
}
```

### **Delta Updates**

- Use `?sinceVersion=X` parameter
- Only update changed items
- Efficient bandwidth usage

## 🎨 **UI Components**

### **Reusable Components**

- **ProgramList**: Observable list of programs with selection
- **FunctionList**: Observable list of functions
- **RunStatusPanel**: Real-time run status display
- **CreditDisplay**: User credit counter with updates
- **DebugControls**: Step, step-over, stop buttons

### **Styling**

- **Modern JavaFX styling** with CSS
- **Consistent color scheme** and typography
- **Responsive layout** for different screen sizes
- **Loading indicators** for async operations

## 🔧 **Technical Stack**

### **Dependencies**

```xml
<dependencies>
    <!-- JavaFX -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>21</version>
    </dependency>

    <!-- HTTP Client -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>

    <!-- JSON Processing -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
    </dependency>
</dependencies>
```

### **Build Configuration**

- **JavaFX Maven Plugin** for packaging
- **Executable JAR** with embedded JavaFX runtime
- **Cross-platform** deployment

## 🚀 **Implementation Phases**

### **Phase 1: Foundation** (Current)

- [x] Create s-client module
- [x] Setup Maven configuration
- [x] Basic HTTP client service

### **Phase 2: Core UI**

- [ ] Main application structure
- [ ] Login screen
- [ ] Dashboard with program list

### **Phase 3: Program Management**

- [ ] XML upload functionality
- [ ] Program editor (if needed)
- [ ] Function list display

### **Phase 4: Execution**

- [ ] Run preparation and execution
- [ ] Debug interface
- [ ] Real-time status updates

### **Phase 5: Polish**

- [ ] Error handling and user feedback
- [ ] Styling and UX improvements
- [ ] Performance optimization

## 🎯 **Key Design Principles**

1. **Zero Engine Dependencies**: Client only talks HTTP
2. **Reactive UI**: Observable data binding
3. **Async Operations**: Non-blocking HTTP calls
4. **Error Resilience**: Graceful error handling
5. **Real-Time Updates**: Live data synchronization
6. **Exercise-2 Compatibility**: Same UX patterns

## 🔗 **Integration Points**

### **Server API Mapping**

- **Auth**: `/api/auth/login` → AuthService
- **Programs**: `/api/programs` → ProgramService
- **Runs**: `/api/run/*` → RunService
- **Debug**: `/api/debug/*` → DebugService
- **Users**: `/api/users/*` → UserService

**This architecture provides a clean separation between UI and server communication while maintaining the Exercise-2 user experience!** 🎉
