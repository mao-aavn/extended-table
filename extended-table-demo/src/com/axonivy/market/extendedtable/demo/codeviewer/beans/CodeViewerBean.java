package com.axonivy.market.extendedtable.demo.codeviewer.beans;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringEscapeUtils;

import ch.ivyteam.ivy.environment.Ivy;

/**
 * Bean to safely read and display source code files from packaged resources.
 * Provides HTML-escaped content for display in code viewers with syntax highlighting.
 */
@ManagedBean(name = "codeViewerBean")
@ApplicationScoped
public class CodeViewerBean implements Serializable {

  private static final long serialVersionUID = 1L;
  
  // Whitelist: only allow reading from these resource paths
  private static final String XHTML_BASE = "/resources/source/src_hd/";
  private static final String JAVA_BASE = "/resources/source/src/";
  
  // Simple cache to avoid re-reading files
  private final Map<String, String> cache = new HashMap<>();
  
  // Maximum file size to read (100KB)
  private static final int MAX_FILE_SIZE = 100 * 1024;

  /**
   * Get source code content for XHTML file, HTML-escaped and ready for display.
   * 
   * @param relativePath relative path under src_hd folder (e.g., "com/axonivy/market/extendedtable/demo/CheckboxSelection/CheckboxSelection.xhtml")
   * @return HTML-escaped source code or error message
   */
  public String getXhtmlSource(String relativePath) {
    return getSourceEscaped(XHTML_BASE + sanitizePath(relativePath));
  }

  /**
   * Get source code content for Java file, HTML-escaped and ready for display.
   * 
   * @param relativePath relative path under src folder (e.g., "com/axonivy/market/extendedtable/demo/beans/SomeBean.java")
   * @return HTML-escaped source code or error message
   */
  public String getJavaSource(String relativePath) {
    return getSourceEscaped(JAVA_BASE + sanitizePath(relativePath));
  }

  /**
   * Auto-detect and return the current XHTML file path based on the view ID.
   * Returns the relative path suitable for getXhtmlSource().
   */
  public String getAutoDetectedXhtmlPath() {
    try {
      FacesContext context = FacesContext.getCurrentInstance();
      if (context != null && context.getViewRoot() != null) {
        String viewId = context.getViewRoot().getViewId();
        // viewId can be like: /ch.ivyteam.view.path_info/2/com.axonivy.market.extendedtable.demo.CheckboxSelection/CheckboxSelection.xhtml
        // We need: com/axonivy/market/extendedtable/demo/CheckboxSelection/CheckboxSelection.xhtml
        
        if (viewId != null) {
          // Ivy.log().info("CodeViewer: Detecting XHTML path from viewId: {0}", viewId);
          
          // Find the last occurrence of a package-like pattern followed by a filename
          // Pattern: /some/path/com.package.name.ClassName/FileName.xhtml
          // We want: com/package/name/ClassName/FileName.xhtml
          
          // Look for the part that starts with "com." or "com/"
          int comIndex = viewId.lastIndexOf("com.axonivy");
          if (comIndex == -1) {
            comIndex = viewId.lastIndexOf("com/axonivy");
          }
          
          if (comIndex != -1) {
            // Get everything from "com" onwards
            String path = viewId.substring(comIndex);
            
            // Replace dots with slashes, but preserve the .xhtml extension
            // Find the last dot (should be before "xhtml")
            int lastDot = path.lastIndexOf('.');
            if (lastDot != -1 && path.substring(lastDot).equals(".xhtml")) {
              // Replace all dots except the last one with slashes
              String pathWithoutExt = path.substring(0, lastDot).replace('.', '/');
              path = pathWithoutExt + ".xhtml";
            } else {
              // No .xhtml extension found, just replace all dots
              path = path.replace('.', '/');
            }
            
            // Ivy.log().info("CodeViewer: Detected XHTML path: {0}", path);
            return path;
          }
          
          // Fallback: remove leading slash
          if (viewId.startsWith("/")) {
            String fallback = viewId.substring(1);
            Ivy.log().info("CodeViewer: Using fallback XHTML path: {0}", fallback);
            return fallback;
          }
          
          Ivy.log().info("CodeViewer: Using viewId as-is: {0}", viewId);
          return viewId;
        }
      }
    } catch (Exception e) {
      Ivy.log().error("Failed to auto-detect XHTML path", e);
    }
    return null;
  }

  /**
   * Auto-detect the Java bean path from the data bean in request scope.
   * Returns the relative path suitable for getJavaSource().
   */
  public String getAutoDetectedJavaPath() {
    try {
      FacesContext context = FacesContext.getCurrentInstance();
      if (context != null) {
        // Get data.bean from EL context
        Object dataObj = context.getApplication().getELResolver()
            .getValue(context.getELContext(), null, "data");
        
        if (dataObj != null) {
          Object beanObj = context.getApplication().getELResolver()
              .getValue(context.getELContext(), dataObj, "bean");
          
          if (beanObj != null) {
            // Get the class and convert to path
            Class<?> beanClass = beanObj.getClass();
            String className = beanClass.getName();
            // Convert com.axonivy.market.extendedtable.demo.beans.CheckboxSelectionBean
            // to com/axonivy/market/extendedtable/demo/beans/CheckboxSelectionBean.java
            return className.replace('.', '/') + ".java";
          }
        }
      }
    } catch (Exception e) {
      Ivy.log().error("Failed to auto-detect Java bean path", e);
    }
    return null;
  }

  /**
   * Get source code from any allowed resource path.
   * 
   * @param resourcePath full resource path starting with /resources/source/
   * @return HTML-escaped source code or error message
   */
  private String getSourceEscaped(String resourcePath) {
    // Check cache first
    if (cache.containsKey(resourcePath)) {
      return cache.get(resourcePath);
    }

    // Validate path
    if (!isAllowedPath(resourcePath)) {
      String error = "Access denied: path not allowed";
      Ivy.log().warn("CodeViewer: Attempted to read disallowed path: {0}", resourcePath);
      return escapeHtml(error);
    }

    try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
      if (is == null) {
        String error = "Source file not found: " + resourcePath + "\n\nMake sure to run the build to copy sources to resources.";
        return escapeHtml(error);
      }

      // Read content with size limit
      String content = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
          .lines()
          .limit(MAX_FILE_SIZE / 50) // rough line limit
          .collect(Collectors.joining("\n"));

      if (content.length() > MAX_FILE_SIZE) {
        content = content.substring(0, MAX_FILE_SIZE) + "\n\n... (truncated)";
      }

      // Filter out the source viewer include from XHTML files
      if (resourcePath.startsWith(XHTML_BASE)) {
        content = filterSourceViewerInclude(content);
      }

      // Escape HTML and cache
      String escaped = escapeHtml(content);
      cache.put(resourcePath, escaped);
      return escaped;

    } catch (Exception e) {
      String error = "Failed to load source: " + e.getMessage();
      Ivy.log().error("CodeViewer: Error reading {0}", resourcePath, e);
      return escapeHtml(error);
    }
  }

  /**
   * Sanitize path to prevent directory traversal attacks.
   */
  private String sanitizePath(String path) {
    if (path == null) {
      return "";
    }
    // Remove any ../ or ..\\ sequences and leading slashes
    return path.replace("..", "")
               .replace("\\", "/")
               .replaceAll("^/+", "");
  }

  /**
   * Check if resource path is within allowed directories.
   */
  private boolean isAllowedPath(String resourcePath) {
    if (resourcePath == null) {
      return false;
    }
    // Must start with one of the allowed bases
    return resourcePath.startsWith(XHTML_BASE) || resourcePath.startsWith(JAVA_BASE);
  }

  /**
   * Escape HTML entities to safely display code.
   */
  private String escapeHtml(String content) {
    if (content == null) {
      return "";
    }
    return StringEscapeUtils.escapeHtml4(content);
  }

  /**
   * Clear the cache (useful for development).
   */
  public void clearCache() {
    cache.clear();
    Ivy.log().info("CodeViewer: Cache cleared");
  }

  /**
   * Show copy success message.
   * Called from client-side after successful copy to clipboard.
   */
  public void showCopySuccess() {
    FacesContext context = FacesContext.getCurrentInstance();
    if (context != null) {
      context.addMessage(null, 
        new javax.faces.application.FacesMessage(
          javax.faces.application.FacesMessage.SEVERITY_INFO,
          "Success",
          "Source code copied to clipboard!"
        )
      );
    }
  }

  /**
   * Get all related demo Java files for the current bean.
   * Returns a map of filename -> relative path for all demo package imports.
   */
  public Map<String, String> getRelatedDemoJavaFiles() {
    Map<String, String> relatedFiles = new LinkedHashMap<>();
    
    try {
      String javaPath = getAutoDetectedJavaPath();
      if (javaPath == null) {
        return relatedFiles;
      }
      
      String currentFileName = javaPath.substring(javaPath.lastIndexOf('/') + 1);
      relatedFiles.put(currentFileName, javaPath);
      
      collectRelatedFiles(javaPath, relatedFiles, 0);
      
      String xhtmlPath = getAutoDetectedXhtmlPath();
      if (xhtmlPath != null) {
        String processPath = xhtmlPath.replace(".xhtml", "Process.p.json");
        parseProcessJsonImports(processPath, relatedFiles);
      }
      
    } catch (Exception e) {
      Ivy.log().error("Failed to get related demo Java files", e);
    }
    
    return relatedFiles;
  }
  
  /**
   * Recursively collect related demo files by parsing imports.
   * 
   * @param javaPath The path to the Java file to parse
   * @param relatedFiles The map to collect results into
   * @param depth Current recursion depth (to prevent infinite loops)
   */
  private void collectRelatedFiles(String javaPath, Map<String, String> relatedFiles, int depth) {
    if (depth > 5) {
      return;
    }
    
    // Read the source file content (not escaped)
    String resourcePath = JAVA_BASE + sanitizePath(javaPath);
    String content = getSourceRaw(resourcePath);
    
    if (content != null) {
      Pattern importPattern = Pattern.compile("import\\s+(com\\.axonivy\\.market\\.extendedtable\\.demo\\.(?:entities|model|service|daos|controller|enums|beans)\\.[^;]+);");
      Matcher matcher = importPattern.matcher(content);
      
      while (matcher.find()) {
        String importClass = matcher.group(1);
        
        String filePath = importClass.replace('.', '/') + ".java";
        String fileName = importClass.substring(importClass.lastIndexOf('.') + 1) + ".java";
        
        if (relatedFiles.containsKey(fileName)) {
          continue;
        }
        
        String fullPath = JAVA_BASE + filePath;
        if (fileExists(fullPath)) {
          relatedFiles.put(fileName, filePath);
          collectRelatedFiles(filePath, relatedFiles, depth + 1);
        }
      }
      
      Pattern extendsPattern = Pattern.compile("class\\s+\\w+\\s+extends\\s+(\\w+)");
      Matcher extendsMatcher = extendsPattern.matcher(content);
      
      if (extendsMatcher.find()) {
        String parentClassName = extendsMatcher.group(1);
        String parentFilePath = null;
        
        Pattern parentImportPattern = Pattern.compile("import\\s+(com\\.axonivy\\.market\\.extendedtable\\.demo\\.[^;]*\\." + Pattern.quote(parentClassName) + ");");
        Matcher parentImportMatcher = parentImportPattern.matcher(content);
        
        if (parentImportMatcher.find()) {
          String parentClass = parentImportMatcher.group(1);
          parentFilePath = parentClass.replace('.', '/') + ".java";
        } else {
          int lastSlash = javaPath.lastIndexOf('/');
          if (lastSlash > 0) {
            String currentPackage = javaPath.substring(0, lastSlash);
            parentFilePath = currentPackage + "/" + parentClassName + ".java";
          }
        }
        
        if (parentFilePath != null) {
          String parentFileName = parentClassName + ".java";
          
          if (!relatedFiles.containsKey(parentFileName)) {
            String parentFullPath = JAVA_BASE + parentFilePath;
            if (fileExists(parentFullPath)) {
              relatedFiles.put(parentFileName, parentFilePath);
              collectRelatedFiles(parentFilePath, relatedFiles, depth + 1);
            }
          }
        }
      }
    }
  }
  
  /**
   * Parse imports from a Process.p.json file and add to related files.
   * 
   * @param processPath The path to the Process.p.json file (relative to src_hd)
   * @param relatedFiles The map to collect results into
   */
  private void parseProcessJsonImports(String processPath, Map<String, String> relatedFiles) {
    try {
      String content = getSourceRaw(XHTML_BASE + sanitizePath(processPath));
      if (content == null) {
        return;
      }
      
      Pattern importPattern = Pattern.compile("\"import\\s+(com\\.axonivy\\.market\\.extendedtable\\.demo\\.(?:entities|model|service|daos|controller|enums)\\.[^;\"]+);\"");
      Matcher matcher = importPattern.matcher(content);
      
      while (matcher.find()) {
        String importClass = matcher.group(1);
        String filePath = importClass.replace('.', '/') + ".java";
        String fileName = importClass.substring(importClass.lastIndexOf('.') + 1) + ".java";
        
        if (relatedFiles.containsKey(fileName)) {
          continue;
        }
        
        String fullPath = JAVA_BASE + filePath;
        if (fileExists(fullPath)) {
          relatedFiles.put(fileName, filePath);
          collectRelatedFiles(filePath, relatedFiles, 0);
        }
      }
      
    } catch (Exception e) {
      Ivy.log().error("CodeViewer: Error parsing Process.p.json imports", e);
    }
  }
  
  /**
   * Get list of related demo Java file info.
   * Returns a list for easier iteration in JSF, sorted by priority.
   */
  public List<RelatedFileInfo> getRelatedDemoJavaFilesList() {
    cache.clear();
    
    Map<String, String> files = getRelatedDemoJavaFiles();
    List<RelatedFileInfo> result = new ArrayList<>();
    
    for (Map.Entry<String, String> entry : files.entrySet()) {
      result.add(new RelatedFileInfo(entry.getKey(), entry.getValue()));
    }
    
    List<RelatedFileInfo> uniqueList = new ArrayList<>();
    java.util.Set<String> seenPaths = new java.util.HashSet<>();
    
    for (RelatedFileInfo info : result) {
      String normalizedPath = info.getFilePath().toLowerCase();
      if (seenPaths.add(normalizedPath)) {
        uniqueList.add(info);
      }
    }
    
    uniqueList.sort((a, b) -> {
      int priorityA = getFilePriority(a.getFilePath());
      int priorityB = getFilePriority(b.getFilePath());
      
      if (priorityA != priorityB) {
        return Integer.compare(priorityA, priorityB);
      }
      return a.getFileName().compareToIgnoreCase(b.getFileName());
    });
    
    return uniqueList;
  }
  
  /**
   * Get priority for file sorting.
   * Lower number = higher priority.
   * Priority order: Controller, XHTML, Beans, Model, DAO, Services, Entities, Enums, Others
   */
  private int getFilePriority(String filePath) {
    if (filePath.contains("/controller/")) {
      return 0; // Controllers first
    } else if (filePath.endsWith(".xhtml")) {
      return 1; // XHTML files second
    } else if (filePath.contains("/beans/")) {
      return 2; // Beans third
    } else if (filePath.contains("/model/")) {
      return 3; // Models fourth
    } else if (filePath.contains("/daos/") || filePath.contains("/dao/")) {
      return 4; // DAOs fifth
    } else if (filePath.contains("/service/") || filePath.contains("/services/")) {
      return 5; // Services sixth
    } else if (filePath.contains("/entities/") || filePath.contains("/entity/")) {
      return 6; // Entities seventh
    } else if (filePath.contains("/enums/")) {
      return 7; // Enums eighth
    } else {
      return 99; // Others last
    }
  }
  
  /**
   * Simple class to hold related file information.
   */
  public static class RelatedFileInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String fileName;
    private String filePath;
    
    public RelatedFileInfo(String fileName, String filePath) {
      this.fileName = fileName;
      this.filePath = filePath;
    }
    
    public String getFileName() {
      return fileName;
    }
    
    public String getFilePath() {
      return filePath;
    }
  }

  /**
   * Get raw source content without HTML escaping.
   */
  private String getSourceRaw(String resourcePath) {
    InputStream is = null;
    try {
      // Try to get resource stream
      is = getClass().getResourceAsStream(resourcePath);
      
      // If not found, try with Thread context classloader
      if (is == null) {
        is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath.substring(1));
        if (is != null) {
          Ivy.log().debug("CodeViewer: Reading resource using Thread classloader: {0}", resourcePath);
        }
      }
      
      if (is == null) {
        Ivy.log().warn("CodeViewer: Resource not found: {0}", resourcePath);
        return null;
      }
      
      return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
          .lines()
          .limit(MAX_FILE_SIZE / 50)
          .collect(Collectors.joining("\n"));
    } catch (Exception e) {
      Ivy.log().error("Failed to read raw source: {0}", resourcePath, e);
      return null;
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (Exception e) {
          // Ignore
        }
      }
    }
  }

  /**
   * Check if a file exists in the resources.
   */
  private boolean fileExists(String resourcePath) {
    // Try multiple approaches to find the resource
    try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
      if (is != null) {
        return true;
      }
    } catch (Exception e) {
      // Ignore, try next method
    }
    
    // Try with Thread context classloader
    try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath.substring(1))) {
      if (is != null) {
        Ivy.log().debug("CodeViewer: Found resource using Thread classloader: {0}", resourcePath);
        return true;
      }
    } catch (Exception e) {
      // Ignore
    }
    
    Ivy.log().debug("CodeViewer: Resource not found: {0}", resourcePath);
    return false;
  }

  /**
   * Filter out the source viewer include block from XHTML content.
   * Removes the <!-- Source Code Viewer --> comment and the ui:include block.
   */
  private String filterSourceViewerInclude(String content) {
    if (content == null) {
      return content;
    }
    
    // Pattern to match the source viewer block including comments and ui:include
    // Remove from "<!-- Source Code Viewer -->" to the closing </ui:include>
    String filtered = content.replaceAll(
        "(?s)\\s*<!--\\s*Source Code Viewer\\s*-->.*?</ui:include>\\s*",
        ""
    );
    
    return filtered;
  }
}
