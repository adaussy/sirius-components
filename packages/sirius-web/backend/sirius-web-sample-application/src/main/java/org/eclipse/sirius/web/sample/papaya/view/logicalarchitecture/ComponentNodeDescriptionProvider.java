/*******************************************************************************
 * Copyright (c) 2023 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.sirius.web.sample.papaya.view.logicalarchitecture;

import org.eclipse.sirius.components.view.DiagramDescription;
import org.eclipse.sirius.components.view.NodeDescription;
import org.eclipse.sirius.components.view.ViewFactory;
import org.eclipse.sirius.web.sample.papaya.view.INodeDescriptionProvider;
import org.eclipse.sirius.web.sample.papaya.view.PapayaToolsFactory;
import org.eclipse.sirius.web.sample.papaya.view.PapayaViewCache;
import org.eclipse.sirius.web.sample.papaya.view.PapyaViewBuilder;

/**
 * Description of the component.
 *
 * @author sbegaudeau
 */
public class ComponentNodeDescriptionProvider implements INodeDescriptionProvider {

    @Override
    public NodeDescription create() {
        var nodeStyle = ViewFactory.eINSTANCE.createRectangularNodeStyleDescription();
        nodeStyle.setColor("#b0bec5");
        nodeStyle.setBorderColor("#455a64");
        nodeStyle.setLabelColor("#1212121");

        var nodeDescription = new PapyaViewBuilder().createNodeDescription("Component");
        nodeDescription.setSemanticCandidatesExpression("aql:self.components");
        nodeDescription.setLabelExpression("aql:self.name");
        nodeDescription.setChildrenLayoutStrategy(ViewFactory.eINSTANCE.createFreeFormLayoutStrategyDescription());
        nodeDescription.setStyle(nodeStyle);

        var newComponentNodeTool = new PapayaToolsFactory().createNamedElement("papaya::Component", "components", "Component");
        newComponentNodeTool.setName("New Component");
        nodeDescription.getNodeTools().add(newComponentNodeTool);
        nodeDescription.setLabelEditTool(new PapayaToolsFactory().editName());
        nodeDescription.setDeleteTool(new PapayaToolsFactory().deleteTool());

        return nodeDescription;
    }

    @Override
    public void link(DiagramDescription diagramDescription, PapayaViewCache cache) {
        var componentNodeDescription = cache.getNodeDescription("Node papaya::Component");
        var providedServiceNodeDescription = cache.getNodeDescription("Node papaya::ProvidedService");
        var requiredServiceNodeDescription = cache.getNodeDescription("Node papaya::RequiredService");
        var packageNodeDescription = cache.getNodeDescription("Node papaya::Package");

        diagramDescription.getNodeDescriptions().add(componentNodeDescription);
        componentNodeDescription.getBorderNodesDescriptions().add(providedServiceNodeDescription);
        componentNodeDescription.getBorderNodesDescriptions().add(requiredServiceNodeDescription);
        componentNodeDescription.getChildrenDescriptions().add(packageNodeDescription);
    }

}