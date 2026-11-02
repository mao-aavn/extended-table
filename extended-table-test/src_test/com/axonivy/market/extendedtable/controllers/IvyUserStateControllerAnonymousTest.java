package com.axonivy.market.extendedtable.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;

/**
 * Anonymous session (no user logged in): the controller must not throw a
 * NullPointerException but report itself as not available.
 */
@IvyTest
class IvyUserStateControllerAnonymousTest {

	private static final String KEY = "TABLE_STATE_form_table_myState";

	private IvyUserStateController controller;

	@BeforeEach
	void setUp() {
		assertNull(Ivy.session().getSessionUser(), "precondition: anonymous session");
		controller = new IvyUserStateController();
	}

	@Test
	void isAvailable_WithoutUser_ShouldBeFalse() {
		assertFalse(controller.isAvailable());
	}

	@Test
	void load_WithoutUser_ShouldReturnNull() {
		assertNull(controller.load(KEY));
	}

	@Test
	void listKeys_WithoutUser_ShouldReturnEmptyList() {
		assertTrue(controller.listKeys("TABLE_STATE_").isEmpty());
	}

	@Test
	void delete_WithoutUser_ShouldReturnFalse() {
		assertFalse(controller.delete(KEY));
	}

	@Test
	void save_WithoutUser_ShouldFailWithClearMessage() {
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> controller.save(KEY, "{}"));
		assertEquals("Cannot save table state '" + KEY + "': no Ivy user is logged in.", e.getMessage());
	}
}
