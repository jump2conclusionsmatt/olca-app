package org.openlca.app;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.InformationPopup;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchText extends WorkbenchWindowControlContribution {

	private Logger log = LoggerFactory.getLogger(getClass());
	private Text text;
	private DropDownAction action;

	@Override
	protected Control createControl(Composite parent) {
		parent.setLayout(new FillLayout());
		log.trace("create search text control");
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.horizontalSpacing = 5;
		layout.marginHeight = 0;
		layout.marginWidth = 5;
		layout.verticalSpacing = 0;
		composite.setLayout(layout);
		text = new Text(composite, SWT.BORDER | SWT.SEARCH);
		text.addTraverseListener(e -> {
			if (e.detail == SWT.TRAVERSE_RETURN) {
				doSearch(action.typeFilter);
			}
		});
		UI.gridData(text, true, false).minimumWidth = 180;
		createActionMenu(composite);
		return composite;
	}

	private void createActionMenu(Composite composite) {
		ToolBar toolBar = new ToolBar(composite, SWT.NONE);
		ToolBarManager manager = new ToolBarManager(toolBar);
		action = new DropDownAction();
		manager.add(action);
		manager.update(true);
		toolBar.pack();
	}

	private void doSearch(ModelType typeFilter) {
		if (Database.get() == null) {
			InformationPopup.show(M.NeedOpenDatabase);
			return;
		}
		String term = text.getText();
		Search search = new Search(Database.get(), text.getText());
		search.typeFilter = typeFilter;
		App.run(M.Searching, search,
				() -> SearchResultView.show(term, search.getResult()));
	}

	@SuppressWarnings("unused")
	private class SearchAction extends Action {
		public SearchAction() {
			setText(M.Search);
			setImageDescriptor(Icon.SEARCH.descriptor());
		}

		@Override
		public void run() {
			doSearch(null);
		}
	}

	/**
	 * A drop down menu for optionally selecting a model type as a search
	 * filter.
	 */
	private class DropDownAction extends Action implements IMenuCreator {

		private Menu menu;
		private ModelType typeFilter;

		public DropDownAction() {
			setText(M.Search);
			setImageDescriptor(Icon.SEARCH.descriptor());
			setMenuCreator(this);
		}

		@Override
		public void dispose() {
			if (menu == null)
				return;
			if (!menu.isDisposed())
				menu.dispose();
			menu = null;
		}

		@Override
		public Menu getMenu(Menu parent) {
			return null;
		}

		@Override
		public Menu getMenu(Control parent) {
			if (menu != null)
				menu.dispose();
			menu = new Menu(parent);

			String[] texts = { "#Search all types",
					"#Search in projects",
					"#Search in product systems",
					"#Search in LCIA methods",
					"#Search in processes",
					"#Search in flows",
					"#Search in social indicators",
					"#Search in parameters",
					"#Search in flow properties",
					"#Search in unit groups",
					"#Search in actors",
					"#Search in sources",
					"#Search in locations" ,					
					"#Search in data quality systems",
			};

			ModelType[] types = { null,
					ModelType.PROJECT,
					ModelType.PRODUCT_SYSTEM,
					ModelType.IMPACT_METHOD,
					ModelType.PROCESS,
					ModelType.FLOW,
					ModelType.SOCIAL_INDICATOR,
					ModelType.PARAMETER,
					ModelType.FLOW_PROPERTY,
					ModelType.UNIT_GROUP,
					ModelType.CURRENCY,
					ModelType.ACTOR,
					ModelType.SOURCE,
					ModelType.LOCATION,
					ModelType.DQ_SYSTEM
			};

			for (int i = 0; i < texts.length; i++) {
				createItem(menu, texts[i], types[i]);
			}
			return menu;
		}

		private void createItem(Menu menu, final String text,
				final ModelType type) {
			Image image = null;
			if (type == null)
				image = Icon.SEARCH.get();
			else
				image = Images.get(type);
			MenuItem item = new MenuItem(menu, SWT.NONE);
			item.setText(text);
			item.setImage(image);
			Controls.onSelect(item, e -> updateSelection(text, type));
		}

		private void updateSelection(String text, ModelType type) {
			setText(text);
			ImageDescriptor imageDescriptor = null;
			if (type == null)
				imageDescriptor = Icon.SEARCH.descriptor();
			else
				imageDescriptor = Images.descriptor(type);
			setImageDescriptor(imageDescriptor);
			typeFilter = type;
		}

		public void run() {
			doSearch(typeFilter);
		}
	}
}
