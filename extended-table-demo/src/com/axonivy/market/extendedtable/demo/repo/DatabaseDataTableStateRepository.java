package com.axonivy.market.extendedtable.demo.repo;

import java.util.List;

import javax.faces.bean.ManagedBean;

import com.axonivy.market.extendedtable.demo.daos.DataTableStateDAO;
import com.axonivy.market.extendedtable.demo.entities.DataTableState;
import com.axonivy.market.extendedtable.repo.DataTableStateRepository;

@ManagedBean
public class DatabaseDataTableStateRepository implements DataTableStateRepository {
	private DataTableStateDAO dao = new DataTableStateDAO();

	@Override
	public void save(String key, String stateAsJSON) {
		dao.save(DataTableState.builder().stateKey(key).stateValue(stateAsJSON).build());
	}

	@Override
	public String load(String key) {
		DataTableState state = dao.findByKey(key);
		return state != null ? state.getStateValue() : null;
	}

	@Override
	public boolean delete(String key) {
		DataTableState found = dao.findByKey(key);
		if (found == null) {
            return false;
        }
        dao.delete(found);
        return true;
	}

	@Override
	public List<String> listKeys(String prefix) {
		return dao.findAll().stream()
				.map(DataTableState::getStateKey)
				.filter(k -> prefix == null || k.startsWith(prefix))
				.toList();
	}

}
