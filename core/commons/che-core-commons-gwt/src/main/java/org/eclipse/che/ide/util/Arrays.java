/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors:
 * Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.util;

import com.google.common.annotations.Beta;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Utility methods to operate with arrays.
 *
 * @author Vlad Zhukovskiy
 * @since 4.2.0
 */
@Beta
public class Arrays {

    /**
     * Checks where given {@code array} is {@code null} or empty.
     *
     * @param array
     *         the array to check
     * @param <T>
     *         any type of the given array
     * @return {@code true} if given array is null or empty, otherwise {@code false}
     * @since 4.2.0
     */
    public static <T> boolean isNullOrEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    /**
     * Adds the given {@code element} to the tail of given {@code array}.
     *
     * @param array
     *         the array to which {@code element} should be inserted
     * @param element
     *         {@code element} to insert
     * @param <T>
     *         type of given {@code array}
     * @return the copy of given {@code array} with added {@code element}
     * @throws IllegalArgumentException
     *         in case if given {@code arrays} is null
     * @since 4.2.0
     */
    public static <T> T[] add(T[] array, T element) {
        checkArgument(array != null, "Input array is null");

        final int index = array.length;

        array = java.util.Arrays.copyOf(array, index + 1);
        array[index] = element;

        return array;
    }

}
