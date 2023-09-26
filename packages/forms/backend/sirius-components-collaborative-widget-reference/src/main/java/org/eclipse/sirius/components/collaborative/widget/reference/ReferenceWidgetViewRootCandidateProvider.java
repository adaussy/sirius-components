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
package org.eclipse.sirius.components.collaborative.widget.reference;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.sirius.components.core.URLParser;
import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.emf.ResourceMetadataAdapter;
import org.eclipse.sirius.components.emf.services.EditingContext;
import org.eclipse.sirius.components.interpreter.AQLInterpreter;
import org.eclipse.sirius.components.view.View;
import org.eclipse.sirius.components.view.emf.IJavaServiceProvider;
import org.eclipse.sirius.components.view.emf.IViewRepresentationDescriptionSearchService;
import org.eclipse.sirius.components.view.emf.form.IFormIdProvider;
import org.eclipse.sirius.components.view.form.FormElementDescription;
import org.eclipse.sirius.components.widget.reference.IReferenceWidgetRootCandidateSeachProvider;
import org.eclipse.sirius.components.widgets.reference.ReferenceWidgetDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * TODO.
 *
 * @author Arthur Daussy
 */
@Service
public class ReferenceWidgetViewRootCandidateProvider implements IReferenceWidgetRootCandidateSeachProvider {
    private final Logger logger = LoggerFactory.getLogger(ReferenceWidgetViewRootCandidateProvider.class);

    private final IViewRepresentationDescriptionSearchService viewRepresentationDescriptionSearchService;

    private List<IJavaServiceProvider> serviceProviders;

    private final ApplicationContext applicationContext;

    public ReferenceWidgetViewRootCandidateProvider(IViewRepresentationDescriptionSearchService viewRepresentationDescriptionSearchService, List<IJavaServiceProvider> serviceProviders,
            ApplicationContext applicationContext) {
        super();
        this.viewRepresentationDescriptionSearchService = viewRepresentationDescriptionSearchService;
        this.serviceProviders = serviceProviders;
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean canHandle(String descriptionId) {
        if (descriptionId != null && descriptionId.startsWith(IFormIdProvider.FORM_ELEMENT_DESCRIPTION_KIND)) {
            Map<String, List<String>> widgetParameters = new URLParser().getParameterValues(descriptionId);
            return "view".equals(widgetParameters.get("sourceKind").get(0));
        } else {
            return false;
        }
    }

    private AQLInterpreter createInterpreter(View view, IEditingContext editingContext) {
        List<EPackage> visibleEPackages = this.getAccessibleEPackages(editingContext);
        AutowireCapableBeanFactory beanFactory = this.applicationContext.getAutowireCapableBeanFactory();
        List<Object> serviceInstances = this.serviceProviders.stream().flatMap(provider -> provider.getServiceClasses(view).stream()).map(serviceClass -> {
            try {
                return beanFactory.createBean(serviceClass);
            } catch (BeansException beansException) {
                this.logger.warn("Error while trying to instantiate Java service class " + serviceClass.getName(), beansException);
                return null;
            }
        }).filter(Objects::nonNull).map(Object.class::cast).toList();
        return new AQLInterpreter(List.of(), serviceInstances, visibleEPackages);
    }

    private List<EPackage> getAccessibleEPackages(IEditingContext editingContext) {
        if (editingContext instanceof EditingContext) {
            EPackage.Registry packageRegistry = ((EditingContext) editingContext).getDomain().getResourceSet().getPackageRegistry();
            return packageRegistry.values().stream().filter(EPackage.class::isInstance).map(EPackage.class::cast).toList();
        } else {
            return List.of();
        }
    }

    @Override
    public List<? extends Object> getRootElementsForReference(Object targetElement, String descriptionId, IEditingContext editingContext) {
        return this.viewRepresentationDescriptionSearchService.findViewFormElementDescriptionById(descriptionId).filter(ReferenceWidgetDescription.class::isInstance)
                .map(ReferenceWidgetDescription.class::cast).map(ref -> this.getSearchRootElements(ref, editingContext, targetElement)).orElse(List.of());
    }

    private List<? extends Object> getSearchRootElements(ReferenceWidgetDescription description, IEditingContext editingContext, Object self) {



//        String expression = description.getLabelExpression();
//        String expression = "aql:self.eContainer()";
//
//        AQLInterpreter interpreter = this.createInterpreter(this.getView(description), editingContext);
//        VariableManager varMan = new VariableManager();
//        varMan.put("self", self);
//
//        Result result = interpreter.evaluateExpression(varMan.getVariables(), expression);
//        if (result.getStatus().compareTo(Status.WARNING) <= 0) {
//            return result.asObjects().orElse(List.of());
//        }
//        return List.of();

        var optionalResourceSet = Optional.of(editingContext)
                .filter(EditingContext.class::isInstance)
                .map(EditingContext.class::cast)
                .map(EditingContext::getDomain)
                .map(EditingDomain::getResourceSet);

        if (optionalResourceSet.isPresent()) {
            var resourceSet = optionalResourceSet.get();
            return resourceSet.getResources().stream()
                    .filter(res -> res.getURI() != null && EditingContext.RESOURCE_SCHEME.equals(res.getURI().scheme()))
                    .sorted(Comparator.nullsLast(Comparator.comparing(this::getResourceLabel, String.CASE_INSENSITIVE_ORDER)))
                    .toList();
        }

        return List.of();
    }

    private String getResourceLabel(Resource resource) {
        return resource.eAdapters().stream()
                .filter(ResourceMetadataAdapter.class::isInstance)
                .map(ResourceMetadataAdapter.class::cast)
                .findFirst()
                .map(ResourceMetadataAdapter::getName)
                .orElse(resource.getURI().lastSegment());
    }

    private View getView(FormElementDescription desc) {
        EObject container = desc;
        while (container != null && !(container instanceof View)) {
            container = container.eContainer();
        }
        return (View) container;
    }

    // @OVERRIDE
    // PUBLIC LIST<OBJECT> GETROOTELEMENTS(OBJECT TARGETELEMENT, OBJECT FORMELEMENTDESCRIPTION, FORMDESCRIPTION
    // DIAGRAMDESCRIPTION, IEDITINGCONTEXT EDITINGCONTEXT) {
    // RETURN NULL;
    // }

    // @Override
    // List<Object> getRootElements(Object targetElement, String descriptionId, IEditingContext editingContext);
    //
    // Optional<Form> optionalForm = this.representationSearchService.findById(optionalEditingContext.get(), formId,
    // Form.class);
    //
    // Map<String, List<String>> widgetParameters = new URLParser().getParameterValues(descriptionId);
    //
    // String sourceId = this.widgetParameters.get("sourceId").get(0);
    //
    // String sourceElementId = this.widgetParameters.get("sourceElementId").get(0);
    //
    // String kind = this.widgetParameters.get("kind").get(0);
    //
    // return List.of();
    // }

}
