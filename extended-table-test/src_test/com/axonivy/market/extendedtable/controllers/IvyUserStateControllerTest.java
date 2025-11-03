package com.axonivy.market.extendedtable.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.security.IUser;

@IvyTest
class IvyUserStateControllerTest {

    private IvyUserStateController controller;
    private IUser currentUser;
    private String testKey;
    private String testState;
    
    @BeforeEach
    void setUp(AppFixture fixture, @Named("testuser1") IUser user) {
    	fixture.loginUser(user); 
        controller = new IvyUserStateController();
        currentUser = Ivy.session().getSessionUser();
        testKey = "TEST_TABLE_STATE_" + UUID.randomUUID().toString();
        testState = "{\n" +
                "            \"filterBy\": {},\n" +
                "            \"sortBy\": [],\n" +
                "            \"first\": 0,\n" +
                "            \"rows\": 10,\n" +
                "            \"selectedRowKeys\": [],\n" +
                "            \"columnMeta\": {},\n" +
                "            \"expandedRowKeys\": [],\n" +
                "            \"width\": \"100%\"\n" +
                "        }";
        
        // Clean up any existing test properties
        cleanupTestProperties();
    }

    @Test
    void testSave_ShouldPersistStateInUserProperties() {    	
        // When
        controller.save(testKey, testState);
        
        // Then
        String retrievedState = currentUser.getProperty(testKey);
        assertNotNull(retrievedState);
        assertEquals(testState, retrievedState);
    }

    @Test
    void testLoad_ExistingKey_ShouldReturnState() {
        // Given
        currentUser.setProperty(testKey, testState);
        
        // When
        String result = controller.load(testKey);
        
        // Then
        assertEquals(testState, result);
    }

    @Test
    void testLoad_NonExistingKey_ShouldReturnNull() {
        // Given
        String nonExistentKey = "NON_EXISTENT_KEY_" + UUID.randomUUID().toString();
        
        // When
        String result = controller.load(nonExistentKey);
        
        // Then
        assertNull(result);
    }

    @Test
    void testDelete_ExistingKey_ShouldReturnTrueAndRemoveProperty() {
        // Given
        currentUser.setProperty(testKey, testState);
        
        // When
        boolean result = controller.delete(testKey);
        
        // Then
        assertTrue(result);
        assertNull(currentUser.getProperty(testKey));
    }

    @Test
    void testDelete_NonExistingKey_ShouldReturnFalse() {
        // Given
        String nonExistentKey = "NON_EXISTENT_KEY_" + UUID.randomUUID().toString();
        
        // When
        boolean result = controller.delete(nonExistentKey);
        
        // Then
        assertFalse(result);
    }

    @Test
    void testListKeys_WithMatchingPrefix_ShouldReturnFilteredKeys() {
        // Given
        String prefix = "TEST_PREFIX_" + UUID.randomUUID().toString();
        String key1 = prefix + "_state1";
        String key2 = prefix + "_state2";
        String key3 = "OTHER_PREFIX_state3";
        
        currentUser.setProperty(key1, testState);
        currentUser.setProperty(key2, testState);
        currentUser.setProperty(key3, testState);
        
        try {
            // When
            List<String> result = controller.listKeys(prefix);
            
            // Then
            assertThat(result).hasSize(2);
            assertThat(result).contains(key1, key2);
            assertThat(result).doesNotContain(key3);
        } finally {
            // Cleanup
            currentUser.removeProperty(key1);
            currentUser.removeProperty(key2);
            currentUser.removeProperty(key3);
        }
    }

    @Test
    void testListKeys_WithNonMatchingPrefix_ShouldReturnEmptyList() {
        // Given
        String prefix = "NON_MATCHING_PREFIX_" + UUID.randomUUID().toString();
        String otherKey = "OTHER_KEY_" + UUID.randomUUID().toString();
        
        currentUser.setProperty(otherKey, testState);
        
        try {
            // When
            List<String> result = controller.listKeys(prefix);
            
            // Then
            assertThat(result).isEmpty();
        } finally {
            // Cleanup
            currentUser.removeProperty(otherKey);
        }
    }

    @Test
    void testListKeys_WithEmptyPrefix_ShouldReturnAllKeys() {
        // Given
        String key1 = "TEST_KEY1_" + UUID.randomUUID().toString();
        String key2 = "TEST_KEY2_" + UUID.randomUUID().toString();
        
        currentUser.setProperty(key1, testState);
        currentUser.setProperty(key2, testState);
        
        try {
            // When
            List<String> result = controller.listKeys("");
            
            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).contains(key1, key2);
        } finally {
            // Cleanup
            currentUser.removeProperty(key1);
            currentUser.removeProperty(key2);
        }
    }

    @Test
    void testSaveAndLoad_ComplexJsonState_ShouldPreserveData() {
        // Given
        String complexState = "{\n" +
                "            \"filterBy\": {\n" +
                "                \"name\": {\n" +
                "                    \"field\": \"name\",\n" +
                "                    \"filterValue\": \"John\",\n" +
                "                    \"matchMode\": \"contains\"\n" +
                "                },\n" +
                "                \"date\": {\n" +
                "                    \"field\": \"date\",\n" +
                "                    \"filterValue\": [\"2023-01-01\", \"2023-12-31\"],\n" +
                "                    \"matchMode\": \"range\"\n" +
                "                }\n" +
                "            },\n" +
                "            \"sortBy\": [\n" +
                "                {\n" +
                "                    \"field\": \"name\",\n" +
                "                    \"order\": 1\n" +
                "                }\n" +
                "            ],\n" +
                "            \"first\": 20,\n" +
                "            \"rows\": 25,\n" +
                "            \"selectedRowKeys\": [\"1\", \"2\", \"3\"],\n" +
                "            \"columnMeta\": {\n" +
                "                \"name\": {\"width\": 200, \"visible\": true},\n" +
                "                \"email\": {\"width\": 300, \"visible\": false}\n" +
                "            },\n" +
                "            \"expandedRowKeys\": [\"row1\", \"row2\"],\n" +
                "            \"width\": \"1200px\"\n" +
                "        }";
        
        // When
        controller.save(testKey, complexState);
        String retrievedState = controller.load(testKey);
        
        // Then
        assertEquals(complexState, retrievedState);
    }

    @Test
    void testSave_OverwriteExistingKey_ShouldUpdateValue() {
        // Given
        String initialState = "{\"initial\": \"state\"}";
        String updatedState = "{\"updated\": \"state\"}";
        
        controller.save(testKey, initialState);

        // When
        controller.save(testKey, updatedState);
        String result = controller.load(testKey);

        // Then
        assertEquals(updatedState, result);
    }


    @Test
    void testSave_WithNullState_ShouldThrowIllegalArgumentException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            controller.save(testKey, null);
        });
    }


    @Test
    void testSave_WithEmptyState_ShouldStoreEmptyString() {
        // Given
        String emptyState = "";
        
        // When
        controller.save(testKey, emptyState);
        String result = controller.load(testKey);
        
        // Then
        assertEquals(emptyState, result);
    }

    @Test
    void testMultipleOperations_ShouldMaintainIndependentStates() {
        // Given
        String key1 = testKey + "_1";
        String key2 = testKey + "_2";
        String state1 = "{\"table\": \"table1\"}";
        String state2 = "{\"table\": \"table2\"}";

        
        // When
        controller.save(key1, state1);
        controller.save(key2, state2);
        
        // Then
        assertEquals(state1, controller.load(key1));
        assertEquals(state2, controller.load(key2));
        
        // When deleting one
        boolean deleted = controller.delete(key1);
        
        // Then
        assertTrue(deleted);
        assertNull(controller.load(key1));
        assertEquals(state2, controller.load(key2)); // Other state should remain
        
        // Cleanup
        controller.delete(key2);
    }

    private void cleanupTestProperties() {
        // Clean up any properties that might have been left from previous tests
        if (currentUser != null && currentUser.getProperty(testKey) != null) {
            currentUser.removeProperty(testKey);
        }
    }
}
