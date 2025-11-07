package com.axonivy.market.extendedtable.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.faces.application.Application;
import javax.faces.context.FacesContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import ch.ivyteam.ivy.environment.IvyTest;


@IvyTest
class AttrsTest {

    private FacesContext mockFacesContext;
    private Application mockApplication;

    @BeforeEach
    void setUp() {
        mockFacesContext = mock(FacesContext.class);
        mockApplication = mock(Application.class);
        when(mockFacesContext.getApplication()).thenReturn(mockApplication);
    }

    @Test
    void testCurrentContext_WithValidFacesContext_ShouldReturnAttrsInstance() {
        // Given
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            // When
            Attrs result = Attrs.currentContext();
            
            // Then
            assertNotNull(result);
        }
    }

    @Test
    void testCurrentContext_WithNullFacesContext_ShouldThrowException() {
        // Given
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(null);
            
            // When/Then
            assertThrows(IllegalArgumentException.class, () -> {
                Attrs.currentContext();
            });
        }
    }

    @Test
    void testGet_WithStringAttribute_ShouldReturnStringValue() {
        // Given
        String attributeName = "title";
        String expectedValue = "My Table Title";
        String expectedExpression = "#{cc.attrs.title}";
        
        when(mockApplication.evaluateExpressionGet(
            eq(mockFacesContext), 
            eq(expectedExpression), 
            eq(Object.class)
        )).thenReturn(expectedValue);
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            // When
            String result = Attrs.get(attributeName);
            
            // Then
            assertEquals(expectedValue, result);
        }
    }

    @Test
    void testGet_WithBooleanAttribute_ShouldReturnBooleanValue() {
        // Given
        String attributeName = "required";
        Boolean expectedValue = true;
        String expectedExpression = "#{cc.attrs.required}";
        
        when(mockApplication.evaluateExpressionGet(
            eq(mockFacesContext), 
            eq(expectedExpression), 
            eq(Object.class)
        )).thenReturn(expectedValue);
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            // When
            Boolean result = Attrs.get(attributeName);
            
            // Then
            assertEquals(expectedValue, result);
        }
    }

    @Test
    void testGet_WithIntegerAttribute_ShouldReturnIntegerValue() {
        // Given
        String attributeName = "maxRows";
        Integer expectedValue = 100;
        String expectedExpression = "#{cc.attrs.maxRows}";
        
        when(mockApplication.evaluateExpressionGet(
            eq(mockFacesContext), 
            eq(expectedExpression), 
            eq(Object.class)
        )).thenReturn(expectedValue);
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            // When
            Integer result = Attrs.get(attributeName);
            
            // Then
            assertEquals(expectedValue, result);
        }
    }

    @Test
    void testGet_WithComplexObjectAttribute_ShouldReturnObjectValue() {
        // Given
        String attributeName = "tableStateController";
        Object expectedValue = mock(Object.class);
        String expectedExpression = "#{cc.attrs.tableStateController}";
        
        when(mockApplication.evaluateExpressionGet(
            eq(mockFacesContext), 
            eq(expectedExpression), 
            eq(Object.class)
        )).thenReturn(expectedValue);
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            // When
            Object result = Attrs.get(attributeName);
            
            // Then
            assertEquals(expectedValue, result);
        }
    }

    @Test
    void testGet_WithNullAttribute_ShouldReturnNull() {
        // Given
        String attributeName = "nonExistentAttribute";
        String expectedExpression = "#{cc.attrs.nonExistentAttribute}";
        
        when(mockApplication.evaluateExpressionGet(
            eq(mockFacesContext), 
            eq(expectedExpression), 
            eq(Object.class)
        )).thenReturn(null);
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            // When
            Object result = Attrs.get(attributeName);
            
            // Then
            assertNull(result);
        }
    }

    @Test
    void testGet_WithEmptyStringAttribute_ShouldReturnEmptyString() {
        // Given
        String attributeName = "emptyAttribute";
        String expectedValue = "";
        String expectedExpression = "#{cc.attrs.emptyAttribute}";
        
        when(mockApplication.evaluateExpressionGet(
            eq(mockFacesContext), 
            eq(expectedExpression), 
            eq(Object.class)
        )).thenReturn(expectedValue);
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            // When
            String result = Attrs.get(attributeName);
            
            // Then
            assertEquals("", result);
        }
    }

    @Test
    void testGet_WithSpecialCharactersInAttributeName_ShouldHandleCorrectly() {
        // Given
        String attributeName = "special_attr-name.123";
        String expectedValue = "special value";
        String expectedExpression = "#{cc.attrs.special_attr-name.123}";
        
        when(mockApplication.evaluateExpressionGet(
            eq(mockFacesContext), 
            eq(expectedExpression), 
            eq(Object.class)
        )).thenReturn(expectedValue);
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            // When
            String result = Attrs.get(attributeName);
            
            // Then
            assertEquals(expectedValue, result);
        }
    }

    @Test
    void testGet_MultipleAttributeAccess_ShouldWorkCorrectly() {
        // Given
        String attr1 = "tableId";
        String attr2 = "widgetVar";
        String attr3 = "saveSuccessMsg";
        
        String value1 = "myTable";
        String value2 = "myTableWidget";
        String value3 = "Saved successfully";
        
        when(mockApplication.evaluateExpressionGet(eq(mockFacesContext), eq("#{cc.attrs.tableId}"), eq(Object.class)))
            .thenReturn(value1);
        when(mockApplication.evaluateExpressionGet(eq(mockFacesContext), eq("#{cc.attrs.widgetVar}"), eq(Object.class)))
            .thenReturn(value2);
        when(mockApplication.evaluateExpressionGet(eq(mockFacesContext), eq("#{cc.attrs.saveSuccessMsg}"), eq(Object.class)))
            .thenReturn(value3);
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            // When
            String result1 = Attrs.get(attr1);
            String result2 = Attrs.get(attr2);
            String result3 = Attrs.get(attr3);
            
            // Then
            assertEquals(value1, result1);
            assertEquals(value2, result2);
            assertEquals(value3, result3);
        }
    }

    @Test
    void testGet_WithTypeCasting_ShouldReturnCorrectType() {
        // Given
        String attributeName = "maxValue";
        Long expectedValue = 1000L;
        String expectedExpression = "#{cc.attrs.maxValue}";
        
        when(mockApplication.evaluateExpressionGet(
            eq(mockFacesContext), 
            eq(expectedExpression), 
            eq(Object.class)
        )).thenReturn(expectedValue);
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            // When
            Long result = Attrs.get(attributeName);
            
            // Then
            assertEquals(expectedValue, result);
        }
    }

    @Test
    void testGet_WithAttributeNameFromConstants_ShouldWork() {
        // Given - simulating real usage where attribute names are defined as constants
        final String TABLE_ID_ATTR = "tableId";
        final String WIDGET_VAR_ATTR = "widgetVar";
        
        String tableIdValue = "dataTable";
        String widgetVarValue = "dtWidget";
        
        when(mockApplication.evaluateExpressionGet(eq(mockFacesContext), eq("#{cc.attrs.tableId}"), eq(Object.class)))
            .thenReturn(tableIdValue);
        when(mockApplication.evaluateExpressionGet(eq(mockFacesContext), eq("#{cc.attrs.widgetVar}"), eq(Object.class)))
            .thenReturn(widgetVarValue);
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            // When
            String tableId = Attrs.get(TABLE_ID_ATTR);
            String widgetVar = Attrs.get(WIDGET_VAR_ATTR);
            
            // Then
            assertEquals(tableIdValue, tableId);
            assertEquals(widgetVarValue, widgetVar);
        }
    }

}