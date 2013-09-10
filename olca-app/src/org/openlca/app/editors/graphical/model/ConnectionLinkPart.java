/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.graphical.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.requests.GroupRequest;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;
import org.openlca.app.editors.graphical.command.CommandFactory;

public class ConnectionLinkPart extends AbstractConnectionEditPart implements
		PropertyChangeListener {

	@Override
	public void activate() {
		getModel().addPropertyChangeListener(this);
		super.activate();
	}

	@Override
	public void deactivate() {
		getModel().removePropertyChangeListener(this);
		super.deactivate();
	}

	@Override
	protected IFigure createFigure() {
		ConnectionLinkFigure figure = new ConnectionLinkFigure();
		figure.setForegroundColor(ConnectionLink.COLOR);
		figure.setConnectionRouter(getConnectionRouter());
		figure.setTargetDecoration(new PolygonDecoration());
		figure.setVisible(isVisible());
		getModel().setFigure(figure);
		figure.addPropertyChangeListener(this);
		return figure;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE,
				new ConnectionEndpointEditPolicy());
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new ComponentEditPolicy() {
		});
	}

	@Override
	public ConnectionLink getModel() {
		return (ConnectionLink) super.getModel();
	}

	private ProductSystemGraphEditor getEditor() {
		return getModel().getSourceNode().getParent().getEditor();
	}

	private ConnectionRouter getConnectionRouter() {
		return getEditor().isRouted() ? TreeConnectionRouter.get()
				: ConnectionRouter.NULL;
	}

	private boolean isVisible() {
		if (!getModel().getSourceNode().getFigure().isVisible())
			return false;
		if (!getModel().getTargetNode().getFigure().isVisible())
			return false;
		return true;
	}

	@Override
	public void showSourceFeedback(Request req) {
		// TODO adjust
		// if (req instanceof ReconnectRequest) {
		// ReconnectRequest request = ((ReconnectRequest) req);
		// ConnectionLink link = (ConnectionLink) request
		// .getConnectionEditPart().getModel();
		// ExchangeNode target = link.getTargetNode().getExchangeNode(
		// link.getProcessLink().getFlowId());
		// ExchangeNode source = link.getSourceNode().getExchangeNode(
		// link.getProcessLink().getFlowId());
		//
		// ExchangeNode n1 = request.isMovingStartAnchor() ? target : source;
		// ExchangeNode n2 = request.isMovingStartAnchor() ? source : target;
		// ProductSystemNode productSystemNode = n1.getParent().getParent()
		// .getParent();
		// productSystemNode.highlightMatchingExchanges(n1);
		// n1.setHighlighted(true);
		// n2.setHighlighted(true);
		// }
		super.showSourceFeedback(req);
	}

	@Override
	public void eraseSourceFeedback(Request request) {
		// TODO adjust
		// ProcessPart source = (ProcessPart) getSource();
		// ProcessPart target = (ProcessPart) getTarget();
		// ProductSystemNode productSystemNode = source.getModel().getParent();
		// productSystemNode.removeHighlighting();
		// source.getModel().setHighlighted(false);
		// target.getModel().setHighlighted(false);
		super.eraseSourceFeedback(request);
	}

	@Override
	public Command getCommand(Request request) {
		if (request instanceof GroupRequest && request.getType() == REQ_DELETE)
			return CommandFactory.createDeleteLinkCommand(getModel());
		return super.getCommand(request);
	}

	@Override
	public boolean isSelectable() {
		return true;
	}

	@Override
	public void setSelected(int value) {
		if (getFigure().isVisible()) {
			PolylineConnection figure = (PolylineConnection) getFigure();
			if (value != EditPart.SELECTED_NONE) {
				figure.setLineWidth(2);
				figure.setForegroundColor(ConnectionLink.HIGHLIGHT_COLOR);
			} else {
				figure.setLineWidth(1);
				figure.setForegroundColor(ConnectionLink.COLOR);
			}
			super.setSelected(value);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (ConnectionLink.REFRESH_SOURCE_ANCHOR.equals(evt.getPropertyName()))
			refreshSourceAnchor();
		else if (ConnectionLink.REFRESH_TARGET_ANCHOR.equals(evt
				.getPropertyName()))
			refreshTargetAnchor();
		else if (ConnectionLink.HIGHLIGHT.equals(evt.getPropertyName()))
			setSelected((Integer) evt.getNewValue());
		else if ("SELECT".equals(evt.getPropertyName()))
			if ("false".equals(evt.getNewValue().toString()))
				setSelected(EditPart.SELECTED_NONE);

	}

}