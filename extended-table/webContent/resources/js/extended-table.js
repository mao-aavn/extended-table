/**
 * Extended Table JavaScript utilities
 * Provides client-side helper functions for the ExtendedTable component
 */

/**
 * Restores selection for a DataTable widget by selecting rows based on provided keys.
 * This function is designed to work with checkbox selection mode.
 * Compatible with PrimeFaces 11+
 * 
 * @param {string} widgetVar - The widgetVar of the PrimeFaces DataTable
 * @param {Array<string>} rowKeys - Array of row keys to select
 */
function restoreTableSelection(widgetVar, rowKeys) {
	if (!widgetVar || !rowKeys) {
		console.warn('ExtendedTable: widgetVar and rowKeys are required');
		return;
	}
	
	var widget = PF(widgetVar);
	if (!widget) {
		console.warn('ExtendedTable: Widget not found: ' + widgetVar);
		return;
	}
	
	console.log('ExtendedTable: Restoring selection for ' + rowKeys.length + ' rows');
	
	// Clear all existing selections
	widget.unselectAllRows();
	
	// For PrimeFaces 11, we need to find rows by data-rk attribute
	var tbody = widget.tbody;
	if (!tbody || tbody.length === 0) {
		console.warn('ExtendedTable: Table body not found');
		return;
	}
	
	// Select each row by its key
	rowKeys.forEach(function(key) {
		// Find row by data-rk attribute (PF 11 compatible)
		var row = tbody.find('tr[data-rk="' + key + '"]');
		
		if (row && row.length > 0) {
			// Check if this is checkbox selection mode
			if (widget.cfg.selectionMode === undefined || widget.cfg.selectionMode === null) {
				// Checkbox selection mode
				if (typeof widget.selectRowWithCheckbox === 'function') {
					widget.selectRowWithCheckbox(row);
					console.log('ExtendedTable: Selection restored successfully11');
				} else {
					// Fallback: manually trigger checkbox selection
					var checkbox = row.find('div.ui-chkbox-box');
					if (checkbox.length > 0) {
						widget.selectRow(row, false);
						console.log('ExtendedTable: Selection restored successfully22');
					}
				}
			} else {
				// Regular row selection mode
				widget.selectRow(row, false);
				console.log('ExtendedTable: Selection restored successfully33');
			}
		} else {
			console.warn('ExtendedTable: Row not found for key: ' + key);
		}
	});
	
	console.log('ExtendedTable: Selection restored successfully');
}
