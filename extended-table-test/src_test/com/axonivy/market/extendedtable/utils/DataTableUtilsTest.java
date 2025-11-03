package com.axonivy.market.extendedtable.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.convert.DateTimeConverter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.primefaces.PrimeFaces;
import org.primefaces.component.column.Column;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.datatable.DataTableState;
import org.primefaces.component.datepicker.DatePicker;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
@MockitoSettings(strictness = Strictness.LENIENT)
class DataTableUtilsTest {

    @Mock
    private DataTable mockDataTable;
    
    @Mock
    private DataTableState mockDataTableState;
    
    @Mock
    private FacesContext mockFacesContext;
    
    @Mock
    private ExternalContext mockExternalContext;
    
    @Mock
    private ELContext mockELContext;
    
    @Mock
    private ValueExpression mockSelectionValueExpression;
    
    @Mock
    private ValueExpression mockRowKeyValueExpression;
    
    @Mock
    private Column mockColumn;
    
    @Mock
    private DatePicker mockDatePicker;
    
    @Mock
    private DateTimeConverter mockDateTimeConverter;
    
    @Mock
    private PrimeFaces mockPrimeFaces;

    @BeforeEach
    void setUp() {
        when(mockFacesContext.getELContext()).thenReturn(mockELContext);
        when(mockFacesContext.getExternalContext()).thenReturn(mockExternalContext);
        
        Map<String, Object> requestMap = new HashMap<>();
        when(mockExternalContext.getRequestMap()).thenReturn(requestMap);
    }

    @Test
    void testClearSelection_WithMultipleSelectionMode_ShouldClearList() {
        // Given
        List<Object> selectionList = new ArrayList<Object>(Arrays.asList("item1", "item2"));
        Set<String> selectedRowKeys = new HashSet<String>(Arrays.asList("1", "2"));
        
        when(mockDataTable.getValueExpression("selection")).thenReturn(mockSelectionValueExpression);
        when(mockSelectionValueExpression.getValue(mockELContext)).thenReturn(selectionList);
        when(mockDataTable.getMultiViewState(false)).thenReturn(mockDataTableState);
        when(mockDataTableState.getSelectedRowKeys()).thenReturn(selectedRowKeys);
        
        try (MockedStatic<JSFUtils> jsfUtilsMock = Mockito.mockStatic(JSFUtils.class);
             MockedStatic<PrimeFaces> primeFactesMock = Mockito.mockStatic(PrimeFaces.class)) {
            
            jsfUtilsMock.when(JSFUtils::currentContext).thenReturn(mockFacesContext);
            primeFactesMock.when(PrimeFaces::current).thenReturn(mockPrimeFaces);
            when(mockPrimeFaces.ajax()).thenReturn(mock(PrimeFaces.Ajax.class));
            when(mockDataTable.getClientId()).thenReturn("form:table");
            
            // When
            DataTableUtils.clearSelection(mockDataTable);
            
            // Then
            assertThat(selectionList).isEmpty();
            verify(mockDataTable).setSelection(null);
            // selectedRowKeys is a real Set instance, not a mock — assert its state
            assertThat(selectedRowKeys).isEmpty();
        }
    }

    @Test
    void testClearSelection_WithSingleSelectionMode_ShouldSetToNull() {
        // Given
        String singleSelection = "selectedItem";
        
        when(mockDataTable.getValueExpression("selection")).thenReturn(mockSelectionValueExpression);
        when(mockSelectionValueExpression.getValue(mockELContext)).thenReturn(singleSelection);
        when(mockDataTable.getMultiViewState(false)).thenReturn(null);
        
        try (MockedStatic<JSFUtils> jsfUtilsMock = Mockito.mockStatic(JSFUtils.class);
             MockedStatic<PrimeFaces> primeFactesMock = Mockito.mockStatic(PrimeFaces.class)) {
            
            jsfUtilsMock.when(JSFUtils::currentContext).thenReturn(mockFacesContext);
            primeFactesMock.when(PrimeFaces::current).thenReturn(mockPrimeFaces);
            when(mockPrimeFaces.ajax()).thenReturn(mock(PrimeFaces.Ajax.class));
            when(mockDataTable.getClientId()).thenReturn("form:table");
            
            // When
            DataTableUtils.clearSelection(mockDataTable);
            
            // Then
            verify(mockSelectionValueExpression).setValue(mockELContext, null);
            verify(mockDataTable).setSelection(null);
        }
    }

    @Test
    void testClearSelection_WithNoSelectionValueExpression_ShouldOnlyClearTable() {
        // Given
        when(mockDataTable.getValueExpression("selection")).thenReturn(null);
        when(mockDataTable.getMultiViewState(false)).thenReturn(null);
        
        try (MockedStatic<JSFUtils> jsfUtilsMock = Mockito.mockStatic(JSFUtils.class);
             MockedStatic<PrimeFaces> primeFactesMock = Mockito.mockStatic(PrimeFaces.class)) {
            
            jsfUtilsMock.when(JSFUtils::currentContext).thenReturn(mockFacesContext);
            primeFactesMock.when(PrimeFaces::current).thenReturn(mockPrimeFaces);
            when(mockPrimeFaces.ajax()).thenReturn(mock(PrimeFaces.Ajax.class));
            when(mockDataTable.getClientId()).thenReturn("form:table");
            
            // When
            DataTableUtils.clearSelection(mockDataTable);
            
            // Then
            verify(mockDataTable).setSelection(null);
        }
    }

    @Test
    void testRestoreSelection_WithValidRowKeys_ShouldRestoreSelection() {
        // Given
        Set<String> selectedRowKeys = new HashSet<>(Arrays.asList("1", "2"));
        List tableData = Arrays.asList(
            createMockItem("1", "Item1"),
            createMockItem("2", "Item2"),
            createMockItem("3", "Item3")
        );
        List<Object> selectionList = new ArrayList<>();
        
        doReturn(tableData).when(mockDataTable).getFilteredValue();
        when(mockDataTable.getValueExpression("rowKey")).thenReturn(mockRowKeyValueExpression);
        when(mockDataTable.getValueExpression("selection")).thenReturn(mockSelectionValueExpression);
        when(mockSelectionValueExpression.getValue(mockELContext)).thenReturn(selectionList);
        when(mockDataTable.getVar()).thenReturn("item");
        when(mockDataTable.getMultiViewState(true)).thenReturn(mockDataTableState);
        when(mockDataTableState.getSelectedRowKeys()).thenReturn(new HashSet<>());
        
        try (MockedStatic<JSFUtils> jsfUtilsMock = Mockito.mockStatic(JSFUtils.class);
             MockedStatic<PrimeFaces> primeFactesMock = Mockito.mockStatic(PrimeFaces.class)) {
            
            jsfUtilsMock.when(JSFUtils::currentContext).thenReturn(mockFacesContext);
            primeFactesMock.when(PrimeFaces::current).thenReturn(mockPrimeFaces);
            when(mockPrimeFaces.ajax()).thenReturn(mock(PrimeFaces.Ajax.class));
            when(mockDataTable.getClientId()).thenReturn("form:table");
            
            // Mock row key extraction
            when(mockRowKeyValueExpression.getValue(mockELContext))
                .thenReturn("1") // First item
                .thenReturn("2") // Second item  
                .thenReturn("3"); // Third item
            
            // When
            DataTableUtils.restoreSelection(mockDataTable, selectedRowKeys, "tableWidget");
            
            // Then
            assertThat(selectionList).hasSize(2);
            verify(mockDataTable).setSelection(selectionList);
        }
    }

    @Test
    void testRestoreSelection_WithEmptyRowKeys_ShouldNotRestore() {
        // Given
        Set<String> emptyRowKeys = new HashSet<>();
        
        // When
        DataTableUtils.restoreSelection(mockDataTable, emptyRowKeys, "tableWidget");
        
        // Then
        verify(mockDataTable, never()).setSelection(any());
    }

    @Test
    void testRestoreSelection_WithNullRowKeys_ShouldNotRestore() {
        // When
        DataTableUtils.restoreSelection(mockDataTable, null, "tableWidget");
        
        // Then
        verify(mockDataTable, never()).setSelection(any());
    }

    @Test
    void testRestoreSelection_WithSingleSelectionMode_ShouldSetFirstMatchedItem() {
        // Given
        Set<String> selectedRowKeys = new HashSet<>(Arrays.asList("2"));
        List<Map<String, String>> tableData = Arrays.asList(
            createMockItem("1", "Item1"),
            createMockItem("2", "Item2"),
            createMockItem("3", "Item3")
        );
        String singleSelection = null;
        
        doReturn(tableData).when(mockDataTable).getFilteredValue();
        when(mockDataTable.getValueExpression("rowKey")).thenReturn(mockRowKeyValueExpression);
        when(mockDataTable.getValueExpression("selection")).thenReturn(mockSelectionValueExpression);
        when(mockSelectionValueExpression.getValue(mockELContext)).thenReturn(singleSelection);
        when(mockDataTable.getVar()).thenReturn("item");
        when(mockDataTable.getMultiViewState(true)).thenReturn(mockDataTableState);
        when(mockDataTableState.getSelectedRowKeys()).thenReturn(new HashSet<>());
        
        try (MockedStatic<JSFUtils> jsfUtilsMock = Mockito.mockStatic(JSFUtils.class);
             MockedStatic<PrimeFaces> primeFactesMock = Mockito.mockStatic(PrimeFaces.class)) {
            
            jsfUtilsMock.when(JSFUtils::currentContext).thenReturn(mockFacesContext);
            primeFactesMock.when(PrimeFaces::current).thenReturn(mockPrimeFaces);
            when(mockPrimeFaces.ajax()).thenReturn(mock(PrimeFaces.Ajax.class));
            when(mockDataTable.getClientId()).thenReturn("form:table");
            
            // Mock row key extraction
            when(mockRowKeyValueExpression.getValue(mockELContext))
                .thenReturn("1") // First item
                .thenReturn("2") // Second item (should match)
                .thenReturn("3"); // Third item
            
            // When
            DataTableUtils.restoreSelection(mockDataTable, selectedRowKeys, "tableWidget");
            
            // Then
            verify(mockSelectionValueExpression).setValue(eq(mockELContext), any());
            verify(mockDataTable).setSelection(any());
        }
    }

    @Test
    void testUpdateTable_ShouldCallAjaxUpdate() {
        // Given
        when(mockDataTable.getClientId()).thenReturn("form:table");
        
        try (MockedStatic<PrimeFaces> primeFactesMock = Mockito.mockStatic(PrimeFaces.class)) {
            PrimeFaces.Ajax mockAjax = mock(PrimeFaces.Ajax.class);
            primeFactesMock.when(PrimeFaces::current).thenReturn(mockPrimeFaces);
            when(mockPrimeFaces.ajax()).thenReturn(mockAjax);
            
            // When
            DataTableUtils.updateTable(mockDataTable);
            
            // Then
            verify(mockAjax).update("form:table");
        }
    }

    @Test
    void testFindDatePickerPattern_WithDatePickerInFilterFacet_ShouldReturnPattern() {
        // Given
        String expectedPattern = "dd/MM/yyyy";
        
        when(mockColumn.getFacet("filter")).thenReturn(mockDatePicker);
        when(mockDatePicker.getPattern()).thenReturn(expectedPattern);
        
        // When
        String result = DataTableUtils.findDatePickerPattern(mockColumn);
        
        // Then
        assertEquals(expectedPattern, result);
    }

    @Test
    void testFindDatePickerPattern_WithNoFilterFacet_ShouldReturnNull() {
        // Given
        when(mockColumn.getFacet("filter")).thenReturn(null);
        
        // When
        String result = DataTableUtils.findDatePickerPattern(mockColumn);
        
        // Then
        assertNull(result);
    }

    @Test
    void testFindDatePickerPatternRecursive_WithDatePickerComponent_ShouldReturnPattern() {
        // Given
        String expectedPattern = "MM/dd/yyyy";
        when(mockDatePicker.getPattern()).thenReturn(expectedPattern);
        
        // When
        String result = DataTableUtils.findDatePickerPatternRecursive(mockDatePicker);
        
        // Then
        assertEquals(expectedPattern, result);
    }

    @Test
    void testFindDatePickerPatternRecursive_WithNestedDatePicker_ShouldFindPattern() {
        // Given
        String expectedPattern = "yyyy-MM-dd";
        UIComponent parentComponent = mock(UIComponent.class);
        List<UIComponent> children = Arrays.asList(mockDatePicker);
        
        when(parentComponent.getChildren()).thenReturn(children);
        when(mockDatePicker.getPattern()).thenReturn(expectedPattern);
        
        // When
        String result = DataTableUtils.findDatePickerPatternRecursive(parentComponent);
        
        // Then
        assertEquals(expectedPattern, result);
    }

    @Test
    void testRestoreSelectionOnClient_WithValidRowKeys_ShouldExecuteScript() {
        // Given
        Set<String> rowKeys = new HashSet<>(Arrays.asList("key1", "key2"));
        String widgetVar = "myTableWidget";
        
        try (MockedStatic<PrimeFaces> primeFactesMock = Mockito.mockStatic(PrimeFaces.class)) {
            primeFactesMock.when(PrimeFaces::current).thenReturn(mockPrimeFaces);
            
            // When
            DataTableUtils.restoreSelectionOnClient(widgetVar, rowKeys);
            
            // Then
            verify(mockPrimeFaces).executeScript(anyString());
        }
    }

    @Test
    void testRestoreSelectionOnClient_WithEmptyRowKeys_ShouldNotExecuteScript() {
        // Given
        Set<String> emptyRowKeys = new HashSet<>();
        String widgetVar = "myTableWidget";
        
        try (MockedStatic<PrimeFaces> primeFactesMock = Mockito.mockStatic(PrimeFaces.class)) {
            primeFactesMock.when(PrimeFaces::current).thenReturn(mockPrimeFaces);
            
            // When
            DataTableUtils.restoreSelectionOnClient(widgetVar, emptyRowKeys);
            
            // Then
            verify(mockPrimeFaces, never()).executeScript(anyString());
        }
    }

    private Map<String, String> createMockItem(String id, String name) {
        Map<String, String> item = new HashMap<>();
        item.put("id", id);
        item.put("name", name);
        return item;
    }
}
