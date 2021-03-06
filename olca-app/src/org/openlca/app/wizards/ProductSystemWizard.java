package org.openlca.app.wizards;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.core.matrix.ProductSystemBuilder;
import org.openlca.core.matrix.cache.MatrixCache;
import org.openlca.core.matrix.product.index.LinkingMethod;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class ProductSystemWizard extends AbstractWizard<ProductSystem> {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private Process process;

	public ProductSystemWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	/**
	 * Optionally sets the reference process for the product system that should
	 * be created.
	 */
	public void setProcess(Process process) {
		this.process = process;
	}

	@Override
	public boolean performFinish() {
		ProductSystemWizardPage page = (ProductSystemWizardPage) getPage();
		ProductSystem system = page.createModel();
		if (system == null)
			return false;
		system.setCategory(getCategory());
		system.cutoff = page.getCutoff();
		addCreationInfo(system, page);
		try {
			createDao().insert(system);
			boolean autoComplete = page.addSupplyChain();
			if (!autoComplete) {
				App.openEditor(system);
				return true;
			}
			ProcessType preferredType = page.getPreferredType();
			LinkingMethod linkingMethod = page.getLinkingMethod();
			Runner runner = new Runner(system, preferredType, linkingMethod);
			getContainer().run(true, true, runner);
			system = runner.system;
			Cache.registerNew(Descriptors.toDescriptor(system));
			App.openEditor(system);
			return true;
		} catch (Exception e) {
			log.error("Failed to create model", e);
			return false;
		}
	}

	private void addCreationInfo(ProductSystem system, ProductSystemWizardPage page) {
		if (system == null)
			return;
		String text = system.getDescription();
		if (Strings.isNullOrEmpty(text))
			text = "";
		else
			text += "\n\n~~~~~~\n\n";
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		text += "First created: " + format.format(new Date()) + "\n";
		text += "Linking approach during creation: " + getLinkingInfo(page);
		system.setDescription(text);
	}

	private String getLinkingInfo(ProductSystemWizardPage page) {
		LinkingMethod method = page.getLinkingMethod();
		if (!page.addSupplyChain() || method == null)
			return M.None;
		String suffix = "; " + M.PreferredProcessType + ": ";
		if (page.getPreferredType() == ProcessType.LCI_RESULT)
			suffix += M.SystemProcess;
		else
			suffix += M.UnitProcess;
		if (page.getCutoff() != null)
			suffix += "; cutoff = " + page.getCutoff().toString();
		switch (method) {
		case IGNORE_PROVIDERS:
			return M.IgnoreDefaultProviders + suffix;
		case ONLY_LINK_PROVIDERS:
			return M.OnlyLinkDefaultProviders + suffix;
		case PREFER_PROVIDERS:
			return M.PreferDefaultProviders + suffix;
		default:
			return "???" + suffix;
		}
	}

	@Override
	protected ModelType getModelType() {
		return ModelType.PRODUCT_SYSTEM;
	}

	private class Runner implements IRunnableWithProgress {

		private ProductSystem system;
		private ProcessType preferredType;
		private LinkingMethod linkingMethod;
		private MatrixCache cache;

		public Runner(ProductSystem system, ProcessType preferredType, LinkingMethod linkingMethod) {
			this.system = system;
			this.preferredType = preferredType;
			this.cache = Cache.getMatrixCache();
			this.linkingMethod = linkingMethod;
		}

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			try {
				monitor.beginTask(M.CreatingProductSystem,
						IProgressMonitor.UNKNOWN);
				ProductSystemBuilder builder = new ProductSystemBuilder(cache);
				builder.setPreferredType(preferredType);
				builder.setLinkingMethod(linkingMethod);
				if (system.cutoff != null)
					builder.setCutoff(system.cutoff);
				system = builder.autoComplete(system);
				monitor.done();
			} catch (Exception e) {
				log.error("Failed to auto-complete product system", e);
			}
		}
	}

	@Override
	protected String getTitle() {
		return M.NewProductSystem;
	}

	@Override
	protected AbstractWizardPage<ProductSystem> createPage() {
		ProductSystemWizardPage page = new ProductSystemWizardPage();
		page.setProcess(process);
		return page;
	}
}
