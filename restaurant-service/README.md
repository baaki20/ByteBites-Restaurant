### 🔐 Input Validation

- Add validation annotations to request DTOs (`@NotNull`, `@Size`, `@Email`, `@Valid`, etc.)
- Handle validation errors gracefully using a global exception handler

---

### ⚠️ Centralized Exception Handling

- Implement a `@ControllerAdvice` class to handle:
    - Validation exceptions
    - Custom domain exceptions
    - Access control exceptions
- Return structured and consistent JSON error responses

---

### 📝 Logging and Observability

- Add structured logging using SLF4J (`LoggerFactory`)
- Log key actions like request handling, service operations, and exceptions
- Include useful metadata (e.g., `orderId`, `userId`) in logs

---

### 🧪 Unit Testing with JUnit & Mockito

- Write unit tests for your service layer
- Use Mockito to mock dependencies such as repositories and message publishers
- Cover both success and failure scenarios

---

### 🧪 Integration Testing with TestContainers

- Use TestContainers to spin up a real database instance
- Test API endpoints using `MockMvc` or `TestRestTemplate`
- Validate full request/response flows, including security enforcement