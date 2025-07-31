
## 2. Circular Reference in JSON Serialization

### Problem Description:

In JPA, bidirectional relationships between entities create circular references during JSON serialization. When the JSON serializer tries to convert an entity with bidirectional relationships, it gets stuck in an infinite loop because each entity references the other.

**Error:**

```json
{
  "taskId": 8,
  "user": {
    "id": 2,
    "tasks": [
      {
        "taskId": 8,
        "user": {
          "id": 2,
          "tasks": [
            // ... continues infinitely
          ]
        }
      }
    ]
  }
}
```

**Cause:** Bidirectional relationship without proper JSON handling:

```java
// User entity
@OneToMany(mappedBy = "user")
private List<Task> tasks; // ❌ No JSON control

// Task entity
@ManyToOne
private User user; // ❌ No JSON control
```

### Solution:

Use Jackson annotations to control the serialization direction and break the circular reference.

```java
// User.java - Parent side
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
@JsonManagedReference // ✅ Include this side in JSON
private List<Task> tasks;

// Task.java - Child side
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
@JsonBackReference // ✅ Exclude this side from JSON
private User user;
```

**Why this works:** `@JsonManagedReference` includes the relationship in JSON, while `@JsonBackReference` excludes it, breaking the cycle while maintaining the JPA relationship.

## 3. Database Schema vs JPA Entity Mismatch

### Problem Description:

A common issue occurs when the database schema uses one data type (e.g., integers for enum values) but the JPA entity is configured to use a different representation (e.g., strings). This creates runtime errors during data persistence and retrieval.

**Error:**

```
ERROR: column "task_status" is of type smallint but expression is of type character varying
[insert into tasks (date,task_desc,task_status,task_title,user_id) values (?,?,?,?,?)]
```

**Cause:** Database expects integers but JPA sends strings:

```java
// Database schema: task_status SMALLINT (0, 1, 2)
// But JPA configuration:
@Enumerated(EnumType.STRING) // ❌ Sends "TODO", "IN_PROGRESS", "DONE"
private TaskStatus taskStatus;
```

### Solution:

Align the JPA entity configuration with the database schema by using the appropriate enumeration type.

```java
// Simple enum definition
public enum TaskStatus {
    TODO,        // Will be stored as 0
    IN_PROGRESS, // Will be stored as 1
    DONE         // Will be stored as 2
}

// Task entity
@Enumerated(EnumType.ORDINAL) // ✅ Maps to database integers
private TaskStatus taskStatus;
```

**Why this works:** `EnumType.ORDINAL` stores enum values as their ordinal positions (0, 1, 2), matching the database schema, while still allowing JSON APIs to use readable string values.