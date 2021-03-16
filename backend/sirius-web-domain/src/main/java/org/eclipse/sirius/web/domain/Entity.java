/**
 * Copyright (c) 2021 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 */
package org.eclipse.sirius.web.domain;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc --> A representation of the model object '<em><b>Entity</b></em>'. <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 * <li>{@link org.eclipse.sirius.web.domain.Entity#getName <em>Name</em>}</li>
 * <li>{@link org.eclipse.sirius.web.domain.Entity#getAttributes <em>Attributes</em>}</li>
 * <li>{@link org.eclipse.sirius.web.domain.Entity#getRelations <em>Relations</em>}</li>
 * </ul>
 *
 * @see org.eclipse.sirius.web.domain.DomainPackage#getEntity()
 * @model
 * @generated
 */
public interface Entity extends EObject {
    /**
     * Returns the value of the '<em><b>Name</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
     *
     * @return the value of the '<em>Name</em>' attribute.
     * @see #setName(String)
     * @see org.eclipse.sirius.web.domain.DomainPackage#getEntity_Name()
     * @model
     * @generated
     */
    String getName();

    /**
     * Sets the value of the '{@link org.eclipse.sirius.web.domain.Entity#getName <em>Name</em>}' attribute. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     *
     * @param value
     *            the new value of the '<em>Name</em>' attribute.
     * @see #getName()
     * @generated
     */
    void setName(String value);

    /**
     * Returns the value of the '<em><b>Attributes</b></em>' containment reference list. The list contents are of type
     * {@link org.eclipse.sirius.web.domain.Attribute}. <!-- begin-user-doc --> <!-- end-user-doc -->
     *
     * @return the value of the '<em>Attributes</em>' containment reference list.
     * @see org.eclipse.sirius.web.domain.DomainPackage#getEntity_Attributes()
     * @model containment="true"
     * @generated
     */
    EList<Attribute> getAttributes();

    /**
     * Returns the value of the '<em><b>Relations</b></em>' containment reference list. The list contents are of type
     * {@link org.eclipse.sirius.web.domain.Relation}. <!-- begin-user-doc --> <!-- end-user-doc -->
     *
     * @return the value of the '<em>Relations</em>' containment reference list.
     * @see org.eclipse.sirius.web.domain.DomainPackage#getEntity_Relations()
     * @model containment="true"
     * @generated
     */
    EList<Relation> getRelations();

} // Entity
