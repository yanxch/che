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
package org.eclipse.che.api.promises.client;

import elemental.util.ArrayOf;

import com.google.common.annotations.Beta;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.inject.ImplementedBy;

import org.eclipse.che.api.promises.client.js.Executor;
import org.eclipse.che.api.promises.client.js.JsPromiseProvider;

/**
 * @author Vlad Zhukovskyi
 */
@Beta
@ImplementedBy(JsPromiseProvider.class)
public interface PromiseProvider {
    <V> Promise<V> create(Executor<V> conclusion);

    Promise<JsArrayMixed> all(ArrayOf<Promise<?>> promises);

    Promise<JsArrayMixed> all(final Promise<?>... promises);

    <U> Promise<U> reject(PromiseError reason);

    <U> Promise<U> reject(String message);

    <U> Promise<U> reject(Throwable reason);

    <U> Promise<U> resolve(U value);
}
