package org.openlca.app.editors.graphical.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.graphical.command.CommandUtil;
import org.openlca.app.editors.graphical.command.ConnectionInput;
import org.openlca.app.editors.graphical.command.ExpansionCommand;
import org.openlca.app.editors.graphical.command.MassCreationCommand;
import org.openlca.app.editors.graphical.model.ExchangeNode;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.matrix.product.index.LinkingMethod;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ProcessType;
import org.openlca.core.model.descriptors.ProcessDescriptor;

class BuildNextTierAction extends Action implements IBuildAction {

	private final FlowDao flowDao;
	private final ProcessDao processDao;
	private List<ProcessNode> nodes;
	private ProcessType preferredType = ProcessType.UNIT_PROCESS;
	private LinkingMethod linkingMethod = LinkingMethod.ONLY_LINK_PROVIDERS;

	BuildNextTierAction() {
		setId(ActionIds.BUILD_NEXT_TIER);
		setText(M.BuildNextTier);
		flowDao = new FlowDao(Database.get());
		processDao = new ProcessDao(Database.get());
	}

	@Override
	public void setProcessNodes(List<ProcessNode> nodes) {
		this.nodes = nodes;
	}

	@Override
	public void setPreferredType(ProcessType preferredType) {
		this.preferredType = preferredType;
	}

	@Override
	public void setLinkingMethod(LinkingMethod linkingMethod) {
		this.linkingMethod = linkingMethod;
	}

	@Override
	public void run() {
		if (nodes == null || nodes.isEmpty())
			return;
		ProductSystemNode systemNode = nodes.get(0).parent();
		List<ProcessDescriptor> providers = new ArrayList<>();
		List<ConnectionInput> newConnections = new ArrayList<>();
		for (ProcessNode node : nodes)
			collectFor(node, providers, newConnections);
		Command command = MassCreationCommand.nextTier(providers, newConnections, systemNode);
		if (command == null)
			return;
		for (ProcessNode node : nodes)
			command = command.chain(ExpansionCommand.expandLeft(node));
		CommandUtil.executeCommand(command, systemNode.editor);
		systemNode.editor.setDirty(true);
	}

	private void collectFor(ProcessNode node,
			List<ProcessDescriptor> providers,
			List<ConnectionInput> newConnections) {
		long targetId = node.process.getId();
		List<ExchangeNode> toConnect = getLinkCandidates(node);
		for (ExchangeNode exchange : toConnect) {
			ProcessDescriptor provider = findProvider(exchange.exchange);
			if (provider == null)
				continue;
			if (!providers.contains(provider))
				providers.add(provider);
			long flowId = exchange.exchange.flow.getId();
			long exchangeId = exchange.exchange.getId();
			ConnectionInput connectionInput = new ConnectionInput(provider.getId(), flowId, targetId, exchangeId,
					!exchange.exchange.isInput && !exchange.exchange.isAvoided);
			if (newConnections.contains(connectionInput))
				continue;
			newConnections.add(connectionInput);
		}
	}

	private List<ExchangeNode> getLinkCandidates(ProcessNode node) {
		List<ExchangeNode> nodes = new ArrayList<>();
		for (ExchangeNode e : node.loadExchangeNodes()) {
			if (e.exchange == null)
				continue;
			if (e.parent().isConnected(e.exchange.getId()))
				continue; // already connected
			if (e.isWaste() && !e.exchange.isInput)
				nodes.add(e);
			else if (!e.isWaste() && e.exchange.isInput)
				nodes.add(e);
		}
		return nodes;
	}

	private ProcessDescriptor findProvider(Exchange exchange) {
		if (exchange.flow == null)
			return null;
		if (linkingMethod == LinkingMethod.ONLY_LINK_PROVIDERS) {
			if (exchange.defaultProviderId == 0l)
				return null;
			return processDao.getDescriptor(exchange.defaultProviderId);
		}
		if (linkingMethod == LinkingMethod.PREFER_PROVIDERS && exchange.defaultProviderId != 0l)
			return processDao.getDescriptor(exchange.defaultProviderId);
		List<ProcessDescriptor> providers = getProviders(exchange);
		ProcessDescriptor bestMatch = null;
		for (ProcessDescriptor descriptor : providers) {
			if (descriptor.getProcessType() == preferredType)
				return descriptor;
			if (bestMatch != null)
				continue;
			bestMatch = descriptor;
		}
		return bestMatch;
	}

	private List<ProcessDescriptor> getProviders(Exchange exchange) {
		Set<Long> providerIds = null;
		if (!exchange.isInput) {
			providerIds = flowDao.getWhereInput(exchange.flow.getId());
		} else {
			providerIds = flowDao.getWhereOutput(exchange.flow.getId());			
		}
		return processDao.getDescriptors(providerIds);
	}

}
