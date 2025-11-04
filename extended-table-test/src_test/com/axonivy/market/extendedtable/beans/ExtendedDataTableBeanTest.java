package com.axonivy.market.extendedtable.beans;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.Application;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.datatable.DataTableState;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;
import org.primefaces.util.ComponentTraversalUtils;

import com.axonivy.market.extendedtable.controllers.TableStateController;
import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;

/**
 * Unit tests for ExtendedDataTableBean following Ivy standard testing practices.
 * Tests cover table state management, persistence, restoration, and JSON serialization/deserialization.
 */
@IvyTest
@MockitoSettings(strictness = Strictness.LENIENT)
class ExtendedDataTableBeanTest {

    private ExtendedDataTableBean bean;
    
    @Mock
    private TableStateController mockController;
    
    @Mock
    private FacesContext mockFacesContext;
    
    @Mock
    private ExternalContext mockExternalContext;
    
    @Mock
    private Application mockApplication;
    
    @Mock
    private UIViewRoot mockViewRoot;
    
    @Mock
    private DataTable mockDataTable;
    
    @Mock
    private DataTableState mockDataTableState;
    
    private PartialViewContext mockPartialViewContext;

    @BeforeEach
    void setUp() {
        bean = new ExtendedDataTableBean();
        
        // Setup basic mock structure
        when(mockFacesContext.getExternalContext()).thenReturn(mockExternalContext);
        when(mockFacesContext.getApplication()).thenReturn(mockApplication);
        when(mockFacesContext.getViewRoot()).thenReturn(mockViewRoot);
        when(mockViewRoot.getViewId()).thenReturn("/test-view.xhtml");
        
        // Setup request parameter map
        Map<String, String> requestParameterMap = new HashMap<>();
        when(mockExternalContext.getRequestParameterMap()).thenReturn(requestParameterMap);

        // Partial View Context used by PrimeFaces.ajax update
        mockPartialViewContext = mock(PartialViewContext.class);
        when(mockFacesContext.getPartialViewContext()).thenReturn(mockPartialViewContext);
        when(mockPartialViewContext.getRenderIds()).thenReturn(new java.util.HashSet<>());
    }

    @Test
    void testSaveTableState_WithoutExplicitSave_ShouldNotSave() {
        // Given
        Map<String, String> requestParameterMap = new HashMap<>();
        // No "explicitSave" parameter
        when(mockExternalContext.getRequestParameterMap()).thenReturn(requestParameterMap);
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            // When
            bean.saveTableState();
            
            // Then - should not interact with controller
            verify(mockController, never()).save(anyString(), anyString());
        }
    }

    @Test
    void testSaveTableState_WithEmptyStateName_ShouldShowError() {
        // Given
        Map<String, String> requestParameterMap = new HashMap<>();
        requestParameterMap.put("explicitSave", "true");
        when(mockExternalContext.getRequestParameterMap()).thenReturn(requestParameterMap);
        
        bean.setStateName(""); // Empty state name
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            // Mock attribute access for error message
            when(mockApplication.evaluateExpressionGet(any(), eq("#{cc.attrs.stateNameRequiredMsg}"), eq(Object.class)))
                .thenReturn("State name is required");
            
            // When
            bean.saveTableState();
            
            // Then - should not save and should add error message
            verify(mockController, never()).save(anyString(), anyString());
        }
    }

    @Test
    void testSaveTableState_WithValidStateNameAndTable_ShouldSave() {
        // Given
        Map<String, String> requestParameterMap = new HashMap<>();
        requestParameterMap.put("explicitSave", "true");
        when(mockExternalContext.getRequestParameterMap()).thenReturn(requestParameterMap);
        
        bean.setStateName("testState");
        
        // Mock DataTable and state
        when(mockDataTable.getMultiViewState(false)).thenReturn(mockDataTableState);
        when(mockDataTable.getClientId()).thenReturn("form:testTable");
        when(mockViewRoot.findComponent("form:testTable")).thenReturn(mockDataTable);
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            // Mock attribute access
            when(mockApplication.evaluateExpressionGet(any(), eq("#{cc.attrs.tableId}"), eq(Object.class)))
                .thenReturn("testTable");
            when(mockApplication.evaluateExpressionGet(any(), eq("#{cc.attrs.tableStateController}"), eq(Object.class)))
                .thenReturn(mockController);
            when(mockApplication.evaluateExpressionGet(any(), eq("#{cc.attrs.saveSuccessMsg}"), eq(Object.class)))
                .thenReturn("State saved successfully");
            
            // Setup mock controller to return state names
            when(mockController.listKeys("TABLE_STATE_form_testTable_")).thenReturn(
                Arrays.asList("TABLE_STATE_form_testTable_testState", "TABLE_STATE_form_testTable_anotherState"));
            
            // Ensure ComponentTraversalUtils used by JSFUtils.findComponent returns our mock
            try (MockedStatic<ComponentTraversalUtils> traverseMock = Mockito.mockStatic(ComponentTraversalUtils.class)) {
                traverseMock.when(() -> ComponentTraversalUtils.firstWithId("testTable", mockViewRoot))
                    .thenReturn(mockDataTable);

                // When
                bean.saveTableState();

                // Then
                verify(mockController, times(1)).save(eq("TABLE_STATE_form_testTable_testState"), anyString());
                verify(mockController, times(1)).listKeys("TABLE_STATE_form_testTable_");
            }
        }
    }

    @Test
    void testRestoreTableState_WithEmptyStateName_ShouldShowError() throws JsonProcessingException {
        // Given
        bean.setStateName("");
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            when(mockApplication.evaluateExpressionGet(any(), eq("#{cc.attrs.stateNameRequiredMsg}"), eq(Object.class)))
                .thenReturn("State name is required");
            
            // When
            bean.restoreTableState();
            
            // Then
            verify(mockController, never()).load(anyString());
        }
    }

    @Test
    void testRestoreTableState_WithValidState_ShouldRestore() throws JsonProcessingException {
        // Given
        bean.setStateName("testState");
        
        // Setup mock table
        when(mockDataTable.getClientId()).thenReturn("form:testTable");
        when(mockDataTable.getFilteredValue()).thenReturn(new ArrayList<>());
        when(mockDataTable.getMultiViewState(true)).thenReturn(mockDataTableState);
        when(mockViewRoot.findComponent("form:testTable")).thenReturn(mockDataTable);
        
    // Setup mock state
    Map<String, FilterMeta> filterBy = new HashMap<>();
    Map<String, SortMeta> sortBy = new HashMap<>();
        Set<String> selectedRowKeys = new HashSet<>();
        selectedRowKeys.add("1");
        
    when(mockDataTableState.getFilterBy()).thenReturn(filterBy);
    when(mockDataTableState.getSortBy()).thenReturn(sortBy);
        when(mockDataTableState.getFirst()).thenReturn(0);
        when(mockDataTableState.getRows()).thenReturn(10);
        when(mockDataTableState.getSelectedRowKeys()).thenReturn(selectedRowKeys);
        when(mockDataTableState.getColumnMeta()).thenReturn(new HashMap<>());
        when(mockDataTableState.getExpandedRowKeys()).thenReturn(new HashSet<>());
        when(mockDataTableState.getWidth()).thenReturn("100%");
        
        // Mock JSON state
    String jsonState = "{\n" +
        "            \"state\": {\n" +
        "                \"filterBy\": {},\n" +
        "                \"sortBy\": {},\n" +
        "                \"first\": 0,\n" +
        "                \"rows\": 10,\n" +
        "                \"selectedRowKeys\": [\"1\"],\n" +
        "                \"columnMeta\": {},\n" +
        "                \"expandedRowKeys\": [],\n" +
        "                \"width\": \"100%\"\n" +
        "            },\n" +
        "            \"dateFormats\": {}\n" +
        "        }";
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            when(mockApplication.evaluateExpressionGet(any(), eq("#{cc.attrs.tableId}"), eq(Object.class)))
                .thenReturn("testTable");
            when(mockApplication.evaluateExpressionGet(any(), eq("#{cc.attrs.tableStateController}"), eq(Object.class)))
                .thenReturn(mockController);
            when(mockApplication.evaluateExpressionGet(any(), eq("#{cc.attrs.widgetVar}"), eq(Object.class)))
                .thenReturn("testTableWidget");
            
            when(mockController.load("TABLE_STATE_form_testTable_testState")).thenReturn(jsonState);
            
            // Ensure ComponentTraversalUtils used by JSFUtils.findComponent returns our mock
            try (MockedStatic<ComponentTraversalUtils> traverseMock = Mockito.mockStatic(ComponentTraversalUtils.class)) {
                traverseMock.when(() -> ComponentTraversalUtils.firstWithId("testTable", mockViewRoot))
                    .thenReturn(mockDataTable);

                // When
                bean.restoreTableState();

                // Then
                verify(mockDataTable).reset();
                verify(mockDataTable).filterAndSort();
                verify(mockDataTable).resetColumns();
                verify(mockDataTable).setFirst(0);
                verify(mockController).load("TABLE_STATE_form_testTable_testState");
            }
        }
    }

    @Test
    void testResetTable_ShouldClearStateAndSelection() {
        // Given
        bean.setStateName("testState");
        
        when(mockDataTable.getClientId()).thenReturn("form:testTable");
        when(mockViewRoot.findComponent("form:testTable")).thenReturn(mockDataTable);
        when(mockViewRoot.getViewId()).thenReturn("/test-view.xhtml");
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            when(mockApplication.evaluateExpressionGet(any(), eq("#{cc.attrs.tableId}"), eq(Object.class)))
                .thenReturn("testTable");
            
            // When
            try (MockedStatic<ComponentTraversalUtils> traverseMock = Mockito.mockStatic(ComponentTraversalUtils.class)) {
                traverseMock.when(() -> ComponentTraversalUtils.firstWithId("testTable", mockViewRoot))
                    .thenReturn(mockDataTable);

                // When
                bean.resetTable();

                // Then
                verify(mockDataTable).clearInitialState();
                verify(mockDataTable).resetColumns();
                assertNull(bean.getStateName());
            }
        }
    }

    @Test
    void testDeleteTableState_ShouldDeleteAndUpdateStateNames() {
        // Given
        bean.setStateName("testState");
        
        when(mockDataTable.getClientId()).thenReturn("form:testTable");
        when(mockViewRoot.findComponent("form:testTable")).thenReturn(mockDataTable);
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            when(mockApplication.evaluateExpressionGet(any(), eq("#{cc.attrs.tableId}"), eq(Object.class)))
                .thenReturn("testTable");
            when(mockApplication.evaluateExpressionGet(any(), eq("#{cc.attrs.tableStateController}"), eq(Object.class)))
                .thenReturn(mockController);
            when(mockApplication.evaluateExpressionGet(any(), eq("#{cc.attrs.deleteSuccessMsg}"), eq(Object.class)))
                .thenReturn("State deleted successfully");
            
            when(mockController.delete("TABLE_STATE_form_testTable_testState")).thenReturn(true);
            when(mockController.listKeys("TABLE_STATE_form_testTable_")).thenReturn(
                Arrays.asList("TABLE_STATE_form_testTable_anotherState"));
            try (MockedStatic<ComponentTraversalUtils> traverseMock = Mockito.mockStatic(ComponentTraversalUtils.class)) {
                traverseMock.when(() -> ComponentTraversalUtils.firstWithId("testTable", mockViewRoot))
                    .thenReturn(mockDataTable);

                // When
                bean.deleteTableState();

                // Then
                verify(mockController).delete("TABLE_STATE_form_testTable_testState");
                verify(mockController).listKeys("TABLE_STATE_form_testTable_");
                assertEquals("anotherState", bean.getStateName());
            }
        }
    }

    @Test
    void testDeleteTableState_WhenDeleteFails_ShouldShowError() {
        // Given
        bean.setStateName("testState");
        
        when(mockDataTable.getClientId()).thenReturn("form:testTable");
        when(mockViewRoot.findComponent("form:testTable")).thenReturn(mockDataTable);
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            when(mockApplication.evaluateExpressionGet(any(), eq("#{cc.attrs.tableId}"), eq(Object.class)))
                .thenReturn("testTable");
            when(mockApplication.evaluateExpressionGet(any(), eq("#{cc.attrs.tableStateController}"), eq(Object.class)))
                .thenReturn(mockController);
            when(mockApplication.evaluateExpressionGet(any(), eq("#{cc.attrs.deleteErrorMsg}"), eq(Object.class)))
                .thenReturn("Failed to delete state");
            
            when(mockController.delete("TABLE_STATE_form_testTable_testState")).thenReturn(false);
            try (MockedStatic<ComponentTraversalUtils> traverseMock = Mockito.mockStatic(ComponentTraversalUtils.class)) {
                traverseMock.when(() -> ComponentTraversalUtils.firstWithId("testTable", mockViewRoot))
                    .thenReturn(mockDataTable);

                // When
                bean.deleteTableState();

                // Then
                verify(mockController).delete("TABLE_STATE_form_testTable_testState");
            }
        }
    }

    @Test
    void testCompleteStateName_WithEmptyQuery_ShouldReturnAllStateNames() {
        // Given
        List<String> stateNames = Arrays.asList("state1", "state2", "state3");
        bean.setStateNames(stateNames);
        
        // When
        List<String> result = bean.completeStateName("");
        
        // Then
        assertEquals(stateNames, result);
    }

    @Test
    void testCompleteStateName_WithQuery_ShouldReturnFilteredStateNames() {
        // Given
        List<String> stateNames = Arrays.asList("testState1", "myState", "testState2");
        bean.setStateNames(stateNames);
        
        // When
        List<String> result = bean.completeStateName("test");
        
        // Then
        assertThat(result).containsExactly("testState1", "testState2");
    }

    @Test
    void testCompleteStateName_WithNullQuery_ShouldReturnAllStateNames() {
        // Given
        List<String> stateNames = Arrays.asList("state1", "state2");
        bean.setStateNames(stateNames);
        
        // When
        List<String> result = bean.completeStateName(null);
        
        // Then
        assertEquals(stateNames, result);
    }

    @Test
    void testCompleteStateName_WithEmptyStateNames_ShouldFetchFromController() {
        // Given
        bean.setStateNames(new ArrayList<>());
        
        when(mockDataTable.getClientId()).thenReturn("form:testTable");
        when(mockViewRoot.findComponent("form:testTable")).thenReturn(mockDataTable);
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            when(mockApplication.evaluateExpressionGet(any(), eq("#{cc.attrs.tableId}"), eq(Object.class)))
                .thenReturn("testTable");
            when(mockApplication.evaluateExpressionGet(any(), eq("#{cc.attrs.tableStateController}"), eq(Object.class)))
                .thenReturn(mockController);
            
            when(mockController.listKeys("TABLE_STATE_form_testTable_")).thenReturn(
                Arrays.asList("TABLE_STATE_form_testTable_state1", "TABLE_STATE_form_testTable_state2"));
            
            // Ensure ComponentTraversalUtils used by JSFUtils.findComponent returns our mock
            try (var traverseMock = Mockito.mockStatic(ComponentTraversalUtils.class)) {
                traverseMock.when(() -> ComponentTraversalUtils.firstWithId("testTable", mockViewRoot))
                    .thenReturn(mockDataTable);

                // When
                List<String> result = bean.completeStateName("state");

                // Then
                verify(mockController).listKeys("TABLE_STATE_form_testTable_");
                assertThat(result).containsExactly("state1", "state2");
            }
        }
    }

    @Test
    void testGetAndSetStateName() {
        // When/Then
        assertNull(bean.getStateName());
        
        bean.setStateName("testState");
        assertEquals("testState", bean.getStateName());
    }

    @Test
    void testGetAndSetStateNames() {
        // Given
        List<String> stateNames = Arrays.asList("state1", "state2");
        
        // When/Then
        assertNotNull(bean.getStateNames());
        
        bean.setStateNames(stateNames);
        assertEquals(stateNames, bean.getStateNames());
    }

}
