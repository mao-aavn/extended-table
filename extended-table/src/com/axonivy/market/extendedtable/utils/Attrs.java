package com.axonivy.market.extendedtable.utils;

import javax.faces.context.FacesContext;

/**
 * <p>
 * Small utility to get {@code cc.attrs} values in HtmlDialog logic or {@code @ManagedBean}.
 */
public class Attrs {

  private Attrs(FacesContext facesContext) {
    if (facesContext == null) {
      throw new IllegalArgumentException("FacesContext must not be null to use Attrs");
    }
  }

  public static Attrs currentContext() {
    return new Attrs(FacesContext.getCurrentInstance());
  }

  /**
   * <p>
   * E.g if you have <b>cc.attrs.name</b> property:
   * </p>
   * <pre>
   * <code>
   *  Attrs attrs = Attrs.currentContext();
   *  String name = attrs.get("name");
   *  SomeClass someClass = attrs.get("name");
   * </code>
   * </pre>
   */
  public static <T> T get(String attribute) {
    String attributeExpression = String.format("#{cc.attrs.%s}", attribute);
    return getAttribute(attributeExpression);
  }

  private static <T> T getAttribute(String attributeExpression) {
    FacesContext fc = FacesContext.getCurrentInstance();
    @SuppressWarnings("unchecked")
    T attributeValue = (T) fc.getApplication().evaluateExpressionGet(fc, attributeExpression, Object.class);
    return attributeValue;
  }

}
