package com.tars.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.tars.bean.User;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jflame
 * @version 1.0.0
 * @since 2026/3/22
 */
public class JsonRepository {
    private final ObjectMapper objectMapper;
    private final String dataDir;
    private final Map<Class<?>, String> entityToFileMap;

    public JsonRepository() {
        this.objectMapper = new ObjectMapper();
        this.dataDir = "data";
        this.entityToFileMap = new ConcurrentHashMap<>();

        initializeEntityMappings();
        ensureDataDirectoryExists();
    }

    private void initializeEntityMappings() {
        entityToFileMap.put(User.class, "user.json");
        // todo add more entity mappings as needed
    }

    public <T> void saveEntity(T entity, Class<T> entityClass) throws IOException {
        List<T> entities = loadAllEntities(entityClass);
        boolean found = false;

        for (int i = 0; i < entities.size(); i++) {
            T existing = entities.get(i);
            if (hasSameId(existing, entity, entityClass)) {
                entities.set(i, entity);
                found = true;
                break;
            }
        }

        if (!found) {
            entities.add(entity);
        }

        saveAllEntities(entities, entityClass);
    }

    public <T> void saveAllEntities(List<T> entities, Class<T> entityClass) throws IOException {
        String fileName = getFileNameForEntity(entityClass);
        File file = new File(dataDir, fileName);
        objectMapper.writeValue(file, entities);
    }

    public <T> List<T> loadAllEntities(Class<T> entityClass) throws IOException {
        String fileName = getFileNameForEntity(entityClass);
        File file = new File(dataDir, fileName);

        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }

        CollectionType collectionType = objectMapper.getTypeFactory()
                .constructCollectionType(ArrayList.class, entityClass);

        return objectMapper.readValue(file, collectionType);
    }

    public <T> T getEntityById(String id, Class<T> entityClass) throws IOException {
        List<T> entities = loadAllEntities(entityClass);
        return entities.stream()
                .filter(e -> getIdValue(e, entityClass).equals(id))
                .findFirst()
                .orElse(null);
    }

    public <T> boolean deleteEntity(String id, Class<T> entityClass) throws IOException {
        List<T> entities = loadAllEntities(entityClass);
        boolean removed = entities.removeIf(e -> getIdValue(e, entityClass).equals(id));

        if (removed) {
            saveAllEntities(entities, entityClass);
        }

        return removed;
    }

    private <T> String getFileNameForEntity(Class<T> entityClass) {
        String fileName = entityToFileMap.get(entityClass);
        if (fileName == null) {
            fileName = entityClass.getSimpleName().toLowerCase() + ".json";
        }
        return fileName;
    }

    private <T> String getIdValue(T entity, Class<T> entityClass) {
        try {
            java.lang.reflect.Method getIdMethod = entityClass.getMethod("getId");
            Object id = getIdMethod.invoke(entity);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            return System.identityHashCode(entity) + "";
        }
    }

    private <T> boolean hasSameId(T entity1, T entity2, Class<T> entityClass) {
        String id1 = getIdValue(entity1, entityClass);
        String id2 = getIdValue(entity2, entityClass);
        return id1 != null && id1.equals(id2);
    }

    private void ensureDataDirectoryExists() {
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}
