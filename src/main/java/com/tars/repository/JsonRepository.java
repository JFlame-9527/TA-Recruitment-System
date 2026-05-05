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
 * JSON file repository for entity persistence
 *
 * @author Jflame
 * @version 2.0.0
 * @since 2026/3/22
 */
@Slf4j
public class JsonRepository<T> {
    private final ObjectMapper objectMapper;
    private final Class<T> entityClass;
    private final String fileName;

    @Getter
    @Setter
    private static String dataDir = "data";

    public JsonRepository(Class<T> entityClass) {
        this.objectMapper = new ObjectMapper();
        this.entityClass = entityClass;
        this.fileName = entityClass.getSimpleName().toLowerCase() + ".json";
        ensureDataDirectoryExists();
    }


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

    public void saveAllEntities(List<T> entities) throws IOException {
        File file = new File(dataDir, fileName);
        objectMapper.writeValue(file, entities);
    }

    public List<T> loadAllEntities() throws IOException {
        File file = new File(dataDir, fileName);

        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }

        CollectionType collectionType = objectMapper.getTypeFactory()
                .constructCollectionType(ArrayList.class, entityClass);

        return objectMapper.readValue(file, collectionType);
    }

    public T getEntityById(String id) throws IOException {
        List<T> entities = loadAllEntities();
        return entities.stream()
                .filter(e -> getIdValue(e).equals(id))
                .findFirst()
                .orElse(null);
    }

    public boolean deleteEntity(String id) throws IOException {
        List<T> entities = loadAllEntities();
        boolean removed = entities.removeIf(e -> getIdValue(e).equals(id));

        if (removed) {
            saveAllEntities(entities);
        }

        return removed;
    }

    private String getIdValue(T entity) {
        try {
            java.lang.reflect.Method getIdMethod = entityClass.getMethod("getId");
            Object id = getIdMethod.invoke(entity);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            return System.identityHashCode(entity) + "";
        }
    }

    private boolean hasSameId(T entity1, T entity2) {
        String id1 = getIdValue(entity1);
        String id2 = getIdValue(entity2);
        return id1 != null && id1.equals(id2);
    }

    private void ensureDataDirectoryExists() {
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
            log.debug("Created data directory: {}", dataDir);
        }
    }
}
