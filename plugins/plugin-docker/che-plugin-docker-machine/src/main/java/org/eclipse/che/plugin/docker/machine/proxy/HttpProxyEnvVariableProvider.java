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
package org.eclipse.che.plugin.docker.machine.proxy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

/**
 * Add env variables to docker environment with http(s) proxy settings.
 *
 * @author Mykola Morhun
 * @author Alexander Garagatyi
 */
@Singleton
public class HttpProxyEnvVariableProvider implements Provider<Set<String>> {

    private static final String HTTP_PROXY = "http_proxy=";
    private static final String HTTPS_PROXY = "https_proxy=";

    final HashSet<String> env;

    @Inject
    public HttpProxyEnvVariableProvider(@Named("http.proxy") String httpProxy, @Named("https.proxy") String httpsProxy) {
        env = new HashSet<>(4);
        if (httpProxy != null && httpProxy.isEmpty()) {
            env.add(HTTP_PROXY + httpProxy);
        }
        if (httpsProxy != null && httpsProxy.isEmpty()) {
            env.add(HTTPS_PROXY + httpsProxy);
        }
    }

    @Override
    public Set<String> get() {
        return env;
    }

}
