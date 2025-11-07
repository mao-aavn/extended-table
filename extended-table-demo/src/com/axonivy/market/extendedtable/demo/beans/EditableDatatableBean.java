package com.axonivy.market.extendedtable.demo.beans;

import java.util.Objects;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.primefaces.event.CellEditEvent;
import org.primefaces.event.RowEditEvent;

import com.axonivy.market.extendedtable.demo.daos.CustomerDAO;
import com.axonivy.market.extendedtable.demo.entities.Customer;

public class EditableDatatableBean extends GenericDemoBean {

	private CustomerDAO customerDAO = new CustomerDAO();

	public void onCellEdit(CellEditEvent<?> event) {
		Object oldValue = event.getOldValue();
		Object newValue = event.getNewValue();

		if (newValue != null && !newValue.equals(oldValue)) {
			try {
				// Get the row index and retrieve the customer from the items list
				int rowIndex = event.getRowIndex();
				Customer customer = items.get(rowIndex);

				// Save the customer to database
				customerDAO.save(customer);

				FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Cell Saved",
						"Old: " + Objects.toString(oldValue, "") + " New: " + Objects.toString(newValue, ""));
				FacesContext.getCurrentInstance().addMessage(null, msg);
			} catch (Exception e) {
				FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Save Failed",
						"Failed to save changes: " + e.getMessage());
				FacesContext.getCurrentInstance().addMessage(null, msg);
			}
		}
	}

	public void onRowEdit(RowEditEvent<Customer> event) {
		Customer edited = (Customer) event.getObject();
		FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Row Edited",
				edited != null ? edited.getName() : "");
		FacesContext.getCurrentInstance().addMessage(null, msg);
	}

	public void onRowCancel(RowEditEvent<Customer> event) {
		Customer cancelled = (Customer) event.getObject();
		FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_WARN, "Edit Cancelled",
				cancelled != null ? cancelled.getName() : "");
		FacesContext.getCurrentInstance().addMessage(null, msg);
	}

}
