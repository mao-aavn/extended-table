package com.axonivy.market.extendedtable.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class CustomDataTableStateTest {

  private CustomDataTableState state;

  @BeforeEach
  void setUp() {
    state = new CustomDataTableState();
  }

  @Test
  void testDateFormats() {
    assertNull(state.getDateFormats());

    Map<String, String> dateFormats = new HashMap<>();
    dateFormats.put("col1", "dd.MM.yyyy");
    dateFormats.put("col2", "MM/dd/yyyy");

    state.setDateFormats(dateFormats);
    assertEquals(dateFormats, state.getDateFormats());
    assertEquals("dd.MM.yyyy", state.getDateFormats().get("col1"));
  }

  @Test
  void testRenderedColumns() {
    assertNull(state.getRenderedColumns());

    List<String> renderedColumns = Arrays.asList("col1", "col2", "col3");

    state.setRenderedColumns(renderedColumns);
    assertEquals(renderedColumns, state.getRenderedColumns());
    assertEquals(3, state.getRenderedColumns().size());
    assertEquals("col1", state.getRenderedColumns().get(0));
  }
}
