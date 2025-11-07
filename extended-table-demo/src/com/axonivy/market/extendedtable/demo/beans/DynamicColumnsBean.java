package com.axonivy.market.extendedtable.demo.beans;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.faces.context.FacesContext;
import com.axonivy.market.extendedtable.demo.entities.Customer;

public class DynamicColumnsBean extends GenericDemoBean {

	public static class ColumnDef {
		public String header;
		public String property;

		public ColumnDef(String header, String property) {
			this.header = header;
			this.property = property;
		}

		public String getHeader() {
			return header;
		}

		public String getProperty() {
			return property;
		}
	}

	private List<ColumnDef> dynamicColumns = new ArrayList<>();

	/**
	 * Currently visible column property names as set by the column toggler (order:
	 * CSV from client)
	 */
	private List<String> visibleColumns;

	public List<ColumnDef> getDynamicColumns() {
		if (dynamicColumns.isEmpty()) {
			// Populate columns based on Customer entity declared fields
			Field[] fields = Customer.class.getDeclaredFields();
			for (Field f : fields) {
				// skip static fields like serialVersionUID
				if (Modifier.isStatic(f.getModifiers()))
					continue;
				String prop = f.getName();
				if ("serialVersionUID".equals(prop))
					continue;
				// build a human readable header from camelCase property name
				String header = toHeader(prop);
				dynamicColumns.add(new ColumnDef(header, prop));
			}
			// default: preselect first 5 columns
			if (visibleColumns == null) {
				visibleColumns = new ArrayList<>();
				for (int i = 0; i < dynamicColumns.size() && i < 5; i++) {
					visibleColumns.add(dynamicColumns.get(i).getProperty());
				}
			}
		}
		return dynamicColumns;
	}

	private String toHeader(String prop) {
		// split camelCase and underscores into words and capitalize
		StringBuilder sb = new StringBuilder();
		char[] cs = prop.toCharArray();
		boolean prevUpper = false;
		for (int i = 0; i < cs.length; i++) {
			char c = cs[i];
			if (i == 0) {
				sb.append(Character.toUpperCase(c));
				prevUpper = Character.isUpperCase(c);
				continue;
			}
			if (c == '_' || (Character.isUpperCase(c) && !prevUpper)) {
				sb.append(' ');
			}
			sb.append(c);
			prevUpper = Character.isUpperCase(c);
		}
		// special-case some property names
		return sb.toString().replace("CustomerRank", "Rank");
	}

	/**
	 * Called by the remoteCommand on column toggler changes. Reads the request
	 * parameter named "visibleColumns" (CSV of property names) and stores it.
	 */
	public void updateVisibleColumns() {
		String csv = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
				.get("visibleColumns");
		if (csv == null) {
			// no param -> clear to default (show all)
			visibleColumns = null;
			return;
		}
		csv = csv.trim();
		if (csv.isEmpty()) {
			visibleColumns = new ArrayList<>();
			return;
		}
		visibleColumns = Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
	}

	public List<String> getVisibleColumns() {
		return visibleColumns;
	}

	/** Return a comma separated, JS-quoted list of headers for initial visible columns.
	 * Example: 'Name','Company','Country'
	 */
	public String getVisibleHeadersJs() {
		// ensure columns populated
		getDynamicColumns();
		if(visibleColumns == null) return "";
		List<String> headers = new ArrayList<>();
		for(ColumnDef c : dynamicColumns) {
			if(visibleColumns.contains(c.getProperty())) {
				// escape single quotes in header
				String h = c.getHeader().replace("'", "\\'");
				headers.add("'" + h + "'");
			}
		}
		return String.join(",", headers);
	}

	/** Return a JS object literal mapping header text -> property name
	 * Example: 'Name':'name','Company':'company'
	 */
	public String getHeaderToPropJs() {
		getDynamicColumns();
		List<String> entries = new ArrayList<>();
		for(ColumnDef c : dynamicColumns) {
			String h = c.getHeader().replace("'", "\\'");
			String p = c.getProperty();
			entries.add("'" + h + "':'" + p + "'");
		}
		return String.join(",", entries);
	}

	/** Return visible header CSV (comma-separated). Commas inside headers are replaced by space. */
	public String getVisibleHeadersCsv() {
		getDynamicColumns();
		if(visibleColumns == null) return "";
		List<String> headers = new ArrayList<>();
		for(ColumnDef c : dynamicColumns) {
			if(visibleColumns.contains(c.getProperty())) {
				headers.add(c.getHeader().replace(',', ' '));
			}
		}
		return String.join(",", headers);
	}

	/** Return JSON object string mapping header -> property. */
	public String getHeaderToPropJson() {
		getDynamicColumns();
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		boolean first = true;
		for(ColumnDef c : dynamicColumns) {
			if(!first) sb.append(',');
			first = false;
			String h = c.getHeader().replace("\"", "\\\"").replace("\\","\\\\");
			String p = c.getProperty().replace("\"", "\\\"").replace("\\","\\\\");
			sb.append('"').append(h).append('"').append(':').append('"').append(p).append('"');
		}
		sb.append('}');
		return sb.toString();
	}

	/** Helper used by UI to decide if a column property should be rendered. */
	public boolean isColumnVisible(String property) {
		if (visibleColumns == null)
			return true; // default: all visible
		return visibleColumns.contains(property);
	}
}
