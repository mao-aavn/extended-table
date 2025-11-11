package com.axonivy.market.extendedtable.model;

import java.util.List;
import java.util.Map;

import org.primefaces.component.datatable.DataTableState;

/**
 * Custom extension of PrimeFaces DataTableState to support additional state management
 * for extended table features including date formatting and dynamic column rendering.
 */
public class CustomDataTableState extends DataTableState {
	private static final long serialVersionUID = -2060422079496513585L;
	
	/**
	 * Map of column identifiers to their corresponding date format patterns.
	 */
	private Map<String, String> dateFormats;
	
	/**
	 * List of rendered columns on view, this will be only used in case dynamic columns
	 */
	private List<String> renderedColumns;

	public Map<String, String> getDateFormats() {
		return dateFormats;
	}

	public void setDateFormats(Map<String, String> dateFormats) {
		this.dateFormats = dateFormats;
	}

	public List<String> getRenderedColumns() {
		return renderedColumns;
	}

	public void setRenderedColumns(List<String> renderedColumns) {
		this.renderedColumns = renderedColumns;
	}

}
