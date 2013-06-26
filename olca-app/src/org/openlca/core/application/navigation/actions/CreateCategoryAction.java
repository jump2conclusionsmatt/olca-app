/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.navigation.actions;

import java.util.List;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.core.application.Messages;
import org.openlca.core.application.db.Database;
import org.openlca.core.application.navigation.CategoryElement;
import org.openlca.core.application.navigation.INavigationElement;
import org.openlca.core.application.navigation.ModelTypeElement;
import org.openlca.core.application.navigation.Navigator;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This action creates a new category and appends it to the specified parent
 * category
 */
public class CreateCategoryAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Category parent;
	private ModelType modelType;
	private INavigationElement<?> parentElement;

	public CreateCategoryAction() {
		setText(Messages.NavigationView_AddCategoryText);
		setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (element instanceof ModelTypeElement) {
			ModelType type = (ModelType) element.getContent();
			this.parent = null;
			this.modelType = type;
			this.parentElement = element;
			return true;
		}
		if (element instanceof CategoryElement) {
			Category category = (Category) element.getContent();
			parent = category;
			modelType = category.getModelType();
			this.parentElement = element;
			return true;
		}
		return false;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		if (modelType == null)
			return;
		Category category = createCategory();
		if (category == null)
			return;
		try {
			tryInsert(category);
			Navigator.refresh(parentElement);
		} catch (Exception e) {
			log.error("failed to save category", e);
		}
	}

	private void tryInsert(Category category) throws Exception {
		BaseDao<Category> dao = Database.get().createDao(Category.class);
		if (parent == null)
			dao.insert(category);
		else {
			category.setParentCategory(parent);
			parent.add(category);
			dao.update(parent);
		}
	}

	private Category createCategory() {
		String name = getDialogValue();
		if (name == null || name.trim().isEmpty())
			return null;
		name = name.trim();
		Category category = new Category();
		category.setName(name);
		category.setId(UUID.randomUUID().toString());
		category.setModelType(modelType);
		return category;
	}

	private String getDialogValue() {
		InputDialog dialog = new InputDialog(UI.shell(),
				Messages.NavigationView_NewCategoryDialogTitle,
				Messages.NavigationView_NewCategoryDialogText,
				Messages.NavigationView_NewCategoryDialogDefault, null);
		int rc = dialog.open();
		if (rc == Window.OK)
			return dialog.getValue();
		return null;
	}

}