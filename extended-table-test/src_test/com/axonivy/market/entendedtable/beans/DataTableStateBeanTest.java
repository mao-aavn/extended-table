//package com.axonivy.market.entendedtable.beans;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertNull;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import com.axonivy.market.extendedtable.beans.DataTableStateBean;
//
//import ch.ivyteam.ivy.environment.IvyTest;
//
//@IvyTest
//class DataTableStateBeanTest {
//
//  private DataTableStateBean bean;
//
//  @BeforeEach
//  void setUp() {
//    bean = new DataTableStateBean();
//    // ...setup mock DataTable and session property map if needed...
//  }
//
//  @Test
//  void testSaveTableState() {
//    // ...existing code...
//    bean.saveTableState();
//    // Assert that the state is saved in the session property map
//    assertNotNull(bean.getTableStateFromIvyUser());
//  }
//
//  @Test
//  void testRestoreTableState() {
//    // ...existing code...
//    bean.saveTableState();
//    bean.restoreTableState();
//    // Assert that the restored state matches the saved state
//    assertEquals(bean.getTableStateFromIvyUser(), bean.restoreTableState());
//  }
//
//  @Test
//  void testDeleteTableState() {
//    // ...existing code...
//    bean.saveTableState();
//    bean.deleteTableState();
//    // Assert that the state is removed from the session property map
//    assertNull(bean.getTableStateFromIvyUser());
//  }
//
//  @Test
//  void testResetTableState() {
//    // ...existing code...
//    bean.saveTableState();
//    bean.resetTableState();
//    // Assert that the table state is reset to default
//    assertTrue(bean.isTableStateDefault());
//  }
//}
