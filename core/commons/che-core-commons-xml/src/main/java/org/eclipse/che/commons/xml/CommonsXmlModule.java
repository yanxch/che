/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.commons.xml;

import com.google.inject.AbstractModule;

import org.eclipse.che.inject.DynaModule;

/**
 * @author Roman Nikitenko
 */
@DynaModule
public class CommonsXmlModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(XMLTreeExceptionMapper.class);
    }
}
