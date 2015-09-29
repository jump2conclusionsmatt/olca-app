package org.openlca.app.navigation.actions;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.db.DerbyConfiguration;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Editors;
import org.openlca.app.util.UI;
import org.openlca.core.database.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseCopyAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(getClass());

	private DerbyConfiguration config;

	public DatabaseCopyAction() {
		setText(Messages.Copy);
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement dbElement = (DatabaseElement) element;
		IDatabaseConfiguration config = dbElement.getContent();
		if (!(config instanceof DerbyConfiguration))
			return false;
		else {
			this.config = (DerbyConfiguration) config;
			return true;
		}
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		if (config == null)
			return;
		InputDialog dialog = new InputDialog(UI.shell(),
				Messages.Copy,
				Messages.PleaseEnterAName,
				config.getName(), null);
		if (dialog.open() != Window.OK)
			return;
		String newName = dialog.getValue();
		if (!DbUtils.isValidName(newName) || Database.getConfigurations()
				.nameExists(newName.trim())) {
			org.openlca.app.util.Error
					.showBox(Messages.NewDatabase_InvalidName);
			return;
		}
		App.runInUI("Copy database", () -> doCopy(newName));
	}

	private void doCopy(String newName) {
		boolean isActive = Database.isActive(config);
		try {
			if (isActive) {
				Editors.closeAll();
				Database.close();
			}
			File fromFolder = DatabaseDir.getRootFolder(config.getName());
			File toFolder = DatabaseDir.getRootFolder(newName);
			FileUtils.copyDirectory(fromFolder, toFolder);
			DerbyConfiguration newConf = new DerbyConfiguration();
			newConf.setName(newName);
			Database.register(newConf);
			if (isActive)
				Database.activate(config);
			Navigator.refresh();
		} catch (Exception e) {
			log.error("failed to copy database", e);
		}
	}

}
