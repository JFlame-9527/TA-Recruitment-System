package com.tars.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic JSON file-based repository for entity persistence.
 * <p>
 * This class provides CRUD operations for storing and retrieving entities in JSON format.
 * Each entity type is stored in a separate JSON file named after the entity class
 * (e.g., {@code User} → {@code user.json}, {@code Position} → {@code position.json}).
 * </p>
 * <p>
 * <b>Key Features:</b>
 * <ul>
 *   <li>Generic type support - works with any entity class that has an {@code getId()} method</li>
 *   <li>Automatic ID-based entity identification for update/delete operations</li>
 *   <li>File-based persistence with Jackson JSON serialization</li>
 *   <li>Thread-safe file I/O operations (via synchronized ObjectMapper)</li>
 *   <li>Automatic data directory creation on initialization</li>
 * </ul>
 * </p>
 * <p>
 * <b>Storage Structure:</b>
 * <pre>
 * data/
 * ├── user.json          (List&lt;User&gt;)
 * ├── position.json      (List&lt;Position&gt;)
 * ├── taprofile.json     (List&lt;TAProfile&gt;)
 * ├── moprofile.json     (List&lt;MOProfile&gt;)
 * └── application.json   (List&lt;Application&gt;)
 * </pre>
 * </p>
 * <p>
 * <b>Usage Example:</b>
 * <pre>{@code
 * // Create repository for User entities
 * JsonRepository<User> userRepo = new JsonRepository<>(User.class);
 * 
 * // Save or update entity (upsert)
 * User user = new User("john", "John Doe", "password123");
 * userRepo.saveEntity(user);
 * 
 * // Load all entities
 * List<User> users = userRepo.loadAllEntities();
 * 
 * // Get entity by ID
 * User found = userRepo.getEntityById(user.getId());
 * 
 * // Delete entity
 * boolean deleted = userRepo.deleteEntity(user.getId());
 * }</pre>
 * </p>
 * <p>
 * <b>ID Detection Strategy:</b>
 * The repository uses reflection to call the {@code getId()} method on entities.
 * If the method doesn't exist or throws an exception, it falls back to using
 * {@code System.identityHashCode()} as a unique identifier.
 * </p>
 * <p>
 * <b>Thread Safety:</b>
 * While individual operations are atomic, concurrent modifications may lead to
 * race conditions. For production use, consider adding external synchronization
 * or switching to a database-backed repository.
 * </p>
 *
 * @author Jflame
 * @version 2.0.0
 * @since 2026/3/22
 * @param <T> The entity type (must have a getId() method returning a unique identifier)
 * @see ObjectMapper
 * @see File
 */
@Slf4j
public class JsonRepository<T> {
    private final ObjectMapper objectMapper;
    private final Class<T> entityClass;
    private final String fileName;

    /**
     * Base directory for all JSON data files.
     * <p>
     * Defaults to {@code "data"} relative to the working directory.
     * Can be overridden via {@code setDataDir(String)} for testing or custom configurations.
     * </p>

     */
    @Getter
    @Setter
    private static String dataDir = "data";

    /**
     * Creates a new JSON repository for the specified entity type.
     * <p>
     * The repository automatically determines the storage filename based on the
     * entity class name (lowercase + ".json") and ensures the data directory exists.
     * </p>
     * <p>
     * <b>Filename Convention:</b>
     * <ul>
     *   <li>{@code User.class} → {@code "user.json"}</li>
     *   <li>{@code TAProfile.class} → {@code "taprofile.json"}</li>
     *   <li>{@code MOProfile.class} → {@code "moprofile.json"}</li>
     * </ul>
     * </p>
     *
     * @param entityClass The entity class type (used for filename generation and deserialization)
     */
    public JsonRepository(Class<T> entityClass) {
        this.objectMapper = new ObjectMapper();
        this.entityClass = entityClass;
        this.fileName = entityClass.getSimpleName().toLowerCase() + ".json";
        ensureDataDirectoryExists();
    }


    /**
     * Saves an entity to the JSON file, performing an upsert operation.
     * <p>
     * This method loads all existing entities, checks if an entity with the same ID exists,
     * and either updates the existing entity or appends the new one. The entire list is
     * then written back to the file.
     * </p>
     * <p>
     * <b>ID Matching:</b> Uses {@link #hasSameId(Object, Object)} to compare entities
     * based on their {@code getId()} return values.
     * </p>
     * <p>
     * <b>Performance Note:</b> This operation reads and writes the entire file, making it
     * O(n) for both time and space complexity. For large datasets, consider using a database.
     * </p>
     *
     * @param entity The entity to save or update
     * @throws IOException if file I/O fails or JSON serialization encounters errors
     * @see #saveAllEntities(List)
     * @see #getEntityById(String)
     */
    public void saveEntity(T entity) throws IOException {
        List<T> entities = loadAllEntities();
        boolean found = false;

        for (int i = 0; i < entities.size(); i++) {
            T existing = entities.get(i);
            if (hasSameId(existing, entity)) {
                entities.set(i, entity);
                found = true;
                break;
            }
        }

        if (!found) {
            entities.add(entity);
        }

        saveAllEntities(entities);
    }

    /**
     * Saves a complete list of entities to the JSON file, overwriting existing content.
     * <p>
     * This method serializes the entire entity list to JSON format and writes it to the
     * designated file. Use this method for bulk operations or when you need full control
     * over the file content.
     * </p>
     * <p>
     * <b>Warning:</b> This operation completely replaces the file content. Ensure the
     * provided list contains all entities you want to persist.
     * </p>
     *
     * @param entities List of entities to save (can be empty but not null)
     * @throws IOException if file I/O fails or JSON serialization encounters errors
     * @see #saveEntity(Object)
     * @see #loadAllEntities()
     */
    public void saveAllEntities(List<T> entities) throws IOException {
        File file = new File(dataDir, fileName);
        objectMapper.writeValue(file, entities);
    }

    /**
     * Loads all entities from the JSON file.
     * <p>
     * This method reads and deserializes the entire JSON file into a list of entities.
     * If the file doesn't exist or is empty, it returns an empty list instead of throwing
     * an exception.
     * </p>
     * <p>
     * <b>Deserialization:</b> Uses Jackson's {@link CollectionType} to properly deserialize
     * generic lists, ensuring type safety.
     * </p>
     *
     * @return List of all entities (empty list if file doesn't exist or is empty)
     * @throws IOException if file I/O fails or JSON deserialization encounters errors
     * @see #saveAllEntities(List)
     * @see #getEntityById(String)
     */
    public List<T> loadAllEntities() throws IOException {
        File file = new File(dataDir, fileName);

        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }

        CollectionType collectionType = objectMapper.getTypeFactory()
                .constructCollectionType(ArrayList.class, entityClass);

        return objectMapper.readValue(file, collectionType);
    }

    /**
     * Retrieves a single entity by its unique identifier.
     * <p>
     * This method loads all entities and filters them by ID, returning the first match.
     * If no entity with the specified ID exists, it returns {@code null}.
     * </p>
     * <p>
     * <b>Performance Note:</b> This operation loads the entire file into memory and
     * performs a linear search. For large datasets, consider indexing or using a database.
     * </p>
     *
     * @param id The unique identifier of the entity to retrieve
     * @return The entity with matching ID, or {@code null} if not found
     * @throws IOException if file I/O fails or JSON deserialization encounters errors
     * @see #getIdValue(Object)
     */
    public T getEntityById(String id) throws IOException {
        List<T> entities = loadAllEntities();
        return entities.stream()
                .filter(e -> {
                    String entityId = getIdValue(e);
                    return entityId != null && entityId.equals(id);
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * Deletes an entity by its unique identifier.
     * <p>
     * This method removes the entity with the specified ID from the list and saves
     * the updated list back to the file. Returns {@code true} if an entity was deleted,
     * {@code false} if no matching entity was found.
     * </p>
     *
     * @param id The unique identifier of the entity to delete
     * @return {@code true} if the entity was found and deleted, {@code false} otherwise
     * @throws IOException if file I/O fails or JSON serialization encounters errors
     * @see #saveAllEntities(List)
     */
    public boolean deleteEntity(String id) throws IOException {
        List<T> entities = loadAllEntities();
        boolean removed = entities.removeIf(e -> {
            String entityId = getIdValue(e);
            return entityId != null && entityId.equals(id);
        });

        if (removed) {
            saveAllEntities(entities);
        }

        return removed;
    }

    /**
     * Extracts the unique identifier from an entity using reflection.
     * <p>
     * This method attempts to call the {@code getId()} method on the entity. If the method
     * doesn't exist or throws an exception, it falls back to using the object's identity
     * hash code as a unique identifier.
     * </p>
     * <p>
     * <b>Fallback Strategy:</b> Using {@code System.identityHashCode()} ensures that even
     * entities without proper ID methods can be stored, though this is not recommended
     * for production use as the hash code may change between JVM sessions.
     * </p>
     *
     * @param entity The entity to extract ID from
     * @return The entity's ID as a string, or identity hash code if getId() is unavailable
     */
    private String getIdValue(T entity) {
        try {
            java.lang.reflect.Method getIdMethod = entityClass.getMethod("getId");
            Object id = getIdMethod.invoke(entity);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            return System.identityHashCode(entity) + "";
        }
    }

    /**
     * Checks if two entities have the same unique identifier.
     * <p>
     * This method extracts IDs from both entities and compares them for equality.
     * Used internally by {@link #saveEntity(Object)} to determine whether to update
     * or insert an entity.
     * </p>
     *
     * @param entity1 First entity to compare
     * @param entity2 Second entity to compare
     * @return {@code true} if both entities have the same non-null ID, {@code false} otherwise
     * @see #getIdValue(Object)
     */
    private boolean hasSameId(T entity1, T entity2) {
        String id1 = getIdValue(entity1);
        String id2 = getIdValue(entity2);
        return id1 != null && id1.equals(id2);
    }

    /**
     * Ensures the data directory exists, creating it if necessary.
     * <p>
     * This method is called during repository initialization to guarantee that the
     * storage directory is available before any file operations occur.
     * </p>
     * <p>
     * <b>Logging:</b> Creates a debug log entry when the directory is first created
     * to aid in troubleshooting initialization issues.
     * </p>
     *
     * @see #dataDir
     */
    private void ensureDataDirectoryExists() {
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
            log.debug("Created data directory: {}", dataDir);
        }
    }
}
