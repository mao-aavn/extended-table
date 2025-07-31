package com.axonivy.market.extendedtable.utils;

import java.lang.reflect.Field;

public class ObjectUtil {

	/**
	 * Gets the value of a property from the given object using reflection.
	 * 
	 * @param obj          the object to read from
	 * @param propertyName the name of the property/field
	 * @return the value of the property, or null if not found or inaccessible
	 */
	public static Object getPropertyValueViaReflection(Object obj, String propertyName) {
		if (obj == null || propertyName == null) {
			return null;
		}
		Class<?> clazz = obj.getClass();
		while (clazz != null) {
			try {
				Field field = clazz.getDeclaredField(propertyName);
				field.setAccessible(true);
				return field.get(obj);
			} catch (NoSuchFieldException e) {
				clazz = clazz.getSuperclass();
			} catch (IllegalAccessException e) {
				return null;
			}
		}
		return null;
	}
}
