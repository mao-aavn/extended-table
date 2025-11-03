package com.axonivy.market.extendedtable.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.primefaces.util.ComponentTraversalUtils;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;

/**
 * Unit tests for JSFUtils utility class following Ivy standard testing practices.
 * Tests cover JSF message handling, component finding, and context operations.
 */
@IvyTest
@MockitoSettings(strictness = Strictness.LENIENT)
class JSFUtilsTest {

    @Mock
    private FacesContext mockFacesContext;
    
    @Mock
    private UIViewRoot mockViewRoot;
    
    @Mock
    private UIComponent mockComponent;

    @Mock
    private javax.faces.context.ExternalContext mockExternalContext;

    @BeforeEach
    void setUp() {
        when(mockFacesContext.getViewRoot()).thenReturn(mockViewRoot);
        when(mockFacesContext.getExternalContext()).thenReturn(mockExternalContext);
    }

    @Test
    void testGetRequestParam_ShouldReturnValue() {
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            java.util.Map<String, String> params = new java.util.HashMap<>();
            params.put("explicitSave", "true");
            when(mockExternalContext.getRequestParameterMap()).thenReturn(params);

            String result = JSFUtils.getRequestParam("explicitSave");
            assertEquals("true", result);
        }
    }

    @Test
    void testCurrentContext_ShouldReturnCurrentFacesContext() {
        // Given
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            // When
            FacesContext result = JSFUtils.currentContext();
            
            // Then
            assertSame(mockFacesContext, result);
        }
    }

    @Test
    void testGetViewRoot_ShouldReturnViewRootFromCurrentContext() {
        // Given
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            // When
            UIViewRoot result = JSFUtils.getViewRoot();
            
            // Then
            assertSame(mockViewRoot, result);
        }
    }

    @Test
    void testFindComponentFromClientId_ShouldFindComponentByClientId() {
        // Given
        String clientId = "form:myComponent";
        when(mockViewRoot.findComponent(clientId)).thenReturn(mockComponent);
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            // When
            UIComponent result = JSFUtils.findComponentFromClientId(clientId);
            
            // Then
            assertSame(mockComponent, result);
            verify(mockViewRoot).findComponent(clientId);
        }
    }

    @Test
    void testFindComponentFromClientId_WithNonExistentId_ShouldReturnNull() {
        // Given
        String nonExistentId = "form:nonExistent";
        when(mockViewRoot.findComponent(nonExistentId)).thenReturn(null);
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            // When
            UIComponent result = JSFUtils.findComponentFromClientId(nonExistentId);
            
            // Then
            assertNull(result);
        }
    }

    @Test
    void testFindComponent_ShouldFindComponentByLocalId() {
        // Given
        String localId = "myComponent";
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class);
             MockedStatic<ComponentTraversalUtils> traversalUtilsMock = Mockito.mockStatic(ComponentTraversalUtils.class)) {
            
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            traversalUtilsMock.when(() -> ComponentTraversalUtils.firstWithId(localId, mockViewRoot))
                             .thenReturn(mockComponent);
            
            // When
            UIComponent result = JSFUtils.findComponent(localId);
            
            // Then
            assertSame(mockComponent, result);
        }
    }

    @Test
    void testFindComponent_WithNonExistentLocalId_ShouldReturnNull() {
        // Given
        String nonExistentLocalId = "nonExistent";
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class);
             MockedStatic<ComponentTraversalUtils> traversalUtilsMock = Mockito.mockStatic(ComponentTraversalUtils.class)) {
            
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            traversalUtilsMock.when(() -> ComponentTraversalUtils.firstWithId(nonExistentLocalId, mockViewRoot))
                             .thenReturn(null);
            
            // When
            UIComponent result = JSFUtils.findComponent(nonExistentLocalId);
            
            // Then
            assertNull(result);
        }
    }

    @Test
    void testAddErrorMsg_ShouldAddErrorMessageToContext() {
        // Given
        String clientId = "form:myComponent";
        String summaryMsg = "Error occurred";
        String detailMsg = "Detailed error description";
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            ArgumentCaptor<FacesMessage> messageCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            
            // When
            JSFUtils.addErrorMsg(clientId, summaryMsg, detailMsg);
            
            // Then
            verify(mockFacesContext).addMessage(eq(clientId), messageCaptor.capture());
            
            FacesMessage capturedMessage = messageCaptor.getValue();
            assertEquals(FacesMessage.SEVERITY_ERROR, capturedMessage.getSeverity());
            assertEquals(summaryMsg, capturedMessage.getSummary());
            assertEquals(detailMsg, capturedMessage.getDetail());
        }
    }

    @Test
    void testAddErrorMsg_WithNullDetail_ShouldAddErrorMessageWithNullDetail() {
        // Given
        String clientId = "form:myComponent";
        String summaryMsg = "Error occurred";
        String detailMsg = null;
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            ArgumentCaptor<FacesMessage> messageCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            
            // When
            JSFUtils.addErrorMsg(clientId, summaryMsg, detailMsg);
            
            // Then
            verify(mockFacesContext).addMessage(eq(clientId), messageCaptor.capture());
            
            FacesMessage capturedMessage = messageCaptor.getValue();
            assertEquals(FacesMessage.SEVERITY_ERROR, capturedMessage.getSeverity());
            assertEquals(summaryMsg, capturedMessage.getSummary());
            // JSF implementations may set detail to summary when null is passed — accept both behaviors
            assertTrue(capturedMessage.getDetail() == null || capturedMessage.getDetail().equals(summaryMsg));
        }
    }

    @Test
    void testAddInfoMsg_ShouldAddInfoMessageToContext() {
        // Given
        String clientId = "form:myComponent";
        String summaryMsg = "Operation successful";
        String detailMsg = "The operation completed successfully";
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            ArgumentCaptor<FacesMessage> messageCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            
            // When
            JSFUtils.addInfoMsg(clientId, summaryMsg, detailMsg);
            
            // Then
            verify(mockFacesContext).addMessage(eq(clientId), messageCaptor.capture());
            
            FacesMessage capturedMessage = messageCaptor.getValue();
            assertEquals(FacesMessage.SEVERITY_INFO, capturedMessage.getSeverity());
            assertEquals(summaryMsg, capturedMessage.getSummary());
            assertEquals(detailMsg, capturedMessage.getDetail());
        }
    }

    @Test
    void testAddInfoMsg_WithNullClientId_ShouldAddInfoMessageWithNullClientId() {
        // Given
        String clientId = null;
        String summaryMsg = "Operation successful";
        String detailMsg = "The operation completed successfully";
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            ArgumentCaptor<FacesMessage> messageCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            
            // When
            JSFUtils.addInfoMsg(clientId, summaryMsg, detailMsg);
            
            // Then
            verify(mockFacesContext).addMessage(eq(clientId), messageCaptor.capture());
            
            FacesMessage capturedMessage = messageCaptor.getValue();
            assertEquals(FacesMessage.SEVERITY_INFO, capturedMessage.getSeverity());
            assertEquals(summaryMsg, capturedMessage.getSummary());
            assertEquals(detailMsg, capturedMessage.getDetail());
        }
    }

    @Test
    void testAddErrorMsg_WithEmptyStrings_ShouldAddErrorMessageWithEmptyStrings() {
        // Given
        String clientId = "form:myComponent";
        String summaryMsg = "";
        String detailMsg = "";
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            ArgumentCaptor<FacesMessage> messageCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            
            // When
            JSFUtils.addErrorMsg(clientId, summaryMsg, detailMsg);
            
            // Then
            verify(mockFacesContext).addMessage(eq(clientId), messageCaptor.capture());
            
            FacesMessage capturedMessage = messageCaptor.getValue();
            assertEquals(FacesMessage.SEVERITY_ERROR, capturedMessage.getSeverity());
            assertEquals("", capturedMessage.getSummary());
            assertEquals("", capturedMessage.getDetail());
        }
    }

    @Test
    void testAddInfoMsg_WithEmptyStrings_ShouldAddInfoMessageWithEmptyStrings() {
        // Given
        String clientId = "form:myComponent";
        String summaryMsg = "";
        String detailMsg = "";
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            ArgumentCaptor<FacesMessage> messageCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            
            // When
            JSFUtils.addInfoMsg(clientId, summaryMsg, detailMsg);
            
            // Then
            verify(mockFacesContext).addMessage(eq(clientId), messageCaptor.capture());
            
            FacesMessage capturedMessage = messageCaptor.getValue();
            assertEquals(FacesMessage.SEVERITY_INFO, capturedMessage.getSeverity());
            assertEquals("", capturedMessage.getSummary());
            assertEquals("", capturedMessage.getDetail());
        }
    }

    @Test
    void testMultipleMessageTypes_ShouldHandleDifferentSeverities() {
        // Given
        String clientId = "form:messages";
        String errorSummary = "Error message";
        String infoSummary = "Info message";
        
        try (MockedStatic<FacesContext> facesContextMock = Mockito.mockStatic(FacesContext.class)) {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(mockFacesContext);
            
            ArgumentCaptor<FacesMessage> messageCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            
            // When
            JSFUtils.addErrorMsg(clientId, errorSummary, null);
            JSFUtils.addInfoMsg(clientId, infoSummary, null);
            
            // Then
            verify(mockFacesContext, Mockito.times(2)).addMessage(eq(clientId), messageCaptor.capture());
            
            // Verify both messages were added with correct severities
            var capturedMessages = messageCaptor.getAllValues();
            assertEquals(2, capturedMessages.size());
            
            assertEquals(FacesMessage.SEVERITY_ERROR, capturedMessages.get(0).getSeverity());
            assertEquals(errorSummary, capturedMessages.get(0).getSummary());
            
            assertEquals(FacesMessage.SEVERITY_INFO, capturedMessages.get(1).getSeverity());
            assertEquals(infoSummary, capturedMessages.get(1).getSummary());
        }
    }

}
