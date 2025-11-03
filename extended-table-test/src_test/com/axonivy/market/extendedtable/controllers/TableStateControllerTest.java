package com.axonivy.market.extendedtable.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.environment.IvyTest;

/**
 * Unit tests for TableStateController interface following Ivy standard testing practices.
 * Tests use a simple in-memory implementation to verify the contract.
 */
@IvyTest
class TableStateControllerTest {

    private TableStateController controller;

    @BeforeEach
    void setUp() {
        // Use a simple in-memory implementation for testing
        controller = new InMemoryTableStateController();
    }

    @Test
    void testSaveAndLoad_ShouldPersistAndRetrieveState() {
        // Given
        String key = "TEST_TABLE_STATE";
        String state = "{\n" +
                "            \"filterBy\": {},\n" +
                "            \"sortBy\": [],\n" +
                "            \"first\": 0,\n" +
                "            \"rows\": 10\n" +
                "        }";

        // When
        controller.save(key, state);
        String retrievedState = controller.load(key);

        // Then
        assertEquals(state, retrievedState);
    }

    @Test
    void testLoad_NonExistentKey_ShouldReturnNull() {
        // Given
        String nonExistentKey = "NON_EXISTENT_KEY";

        // When
        String result = controller.load(nonExistentKey);

        // Then
        assertNull(result);
    }

    @Test
    void testDelete_ExistingKey_ShouldReturnTrueAndRemoveState() {
        // Given
        String key = "TEST_DELETE_KEY";
        String state = "{\"test\": \"data\"}";
        
        controller.save(key, state);

        // When
        boolean result = controller.delete(key);

        // Then
        assertTrue(result);
        assertNull(controller.load(key));
    }

    @Test
    void testDelete_NonExistentKey_ShouldReturnFalse() {
        // Given
        String nonExistentKey = "NON_EXISTENT_DELETE_KEY";

        // When
        boolean result = controller.delete(nonExistentKey);

        // Then
        assertFalse(result);
    }

    @Test
    void testListKeys_WithPrefix_ShouldReturnMatchingKeys() {
        // Given
        String prefix = "TABLE_STATE_";
        controller.save(prefix + "table1", "{\"table\": \"1\"}");
        controller.save(prefix + "table2", "{\"table\": \"2\"}");
        controller.save("OTHER_PREFIX_table3", "{\"table\": \"3\"}");

        // When
        List<String> result = controller.listKeys(prefix);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(prefix + "table1", prefix + "table2");
        assertThat(result).doesNotContain("OTHER_PREFIX_table3");
    }

    @Test
    void testListKeys_WithEmptyPrefix_ShouldReturnAllKeys() {
        // Given
        controller.save("key1", "{\"data\": \"1\"}");
        controller.save("key2", "{\"data\": \"2\"}");

        // When
        List<String> result = controller.listKeys("");

        // Then
        assertThat(result).contains("key1", "key2");
    }

    @Test
    void testSave_OverwriteExistingKey_ShouldUpdateValue() {
        // Given
        String key = "UPDATE_TEST_KEY";
        String initialState = "{\"initial\": \"state\"}";
        String updatedState = "{\"updated\": \"state\"}";

        controller.save(key, initialState);

        // When
        controller.save(key, updatedState);
        String result = controller.load(key);

        // Then
        assertEquals(updatedState, result);
    }

    @Test
    void testSave_WithNullState_ShouldStoreNull() {
        // Given
        String key = "NULL_STATE_KEY";

        // When
        controller.save(key, null);
        String result = controller.load(key);

        // Then
        assertNull(result);
    }

    /**
     * Simple in-memory implementation of TableStateController for testing purposes.
     */
    private static class InMemoryTableStateController implements TableStateController {
        
        private final Map<String, String> storage = new HashMap<>();

        @Override
        public void save(String key, String stateAsJSON) {
            storage.put(key, stateAsJSON);
        }

        @Override
        public String load(String key) {
            return storage.get(key);
        }

        @Override
        public boolean delete(String key) {
            return storage.remove(key) != null;
        }

        @Override
        public List<String> listKeys(String prefix) {
            return storage.keySet().stream()
                    .filter(key -> key.startsWith(prefix))
                    .toList();
        }
    }
}