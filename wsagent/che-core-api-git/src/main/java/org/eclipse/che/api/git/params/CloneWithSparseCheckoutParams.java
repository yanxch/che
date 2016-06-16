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
package org.eclipse.che.api.git.params;

import javax.validation.constraints.NotNull;

/**
 * Arguments holder for {@link org.eclipse.che.api.git.GitConnection#cloneWithSparseCheckout(CloneWithSparseCheckoutParams)}.
 *
 * @author Igor Vinokur
 */
public class CloneWithSparseCheckoutParams {

    private String directory;
    private String remoteUrl;
    private String branch;

    private CloneWithSparseCheckoutParams() {
    }

    /**
     * Create new {@link CloneWithSparseCheckoutParams} instance
     *
     * @param directory
     *         path to keep in working tree
     * @param remoteUrl
     *         url to clone
     * @param branch
     *         branch to checkout
     */
    public static CloneWithSparseCheckoutParams create(@NotNull String directory, @NotNull String remoteUrl, @NotNull String branch) {
        return new CloneWithSparseCheckoutParams().withDirectory(directory)
                                                  .withRemoteUrl(remoteUrl)
                                                  .withBranch(branch);
    }

    /** @return path to keep in working tree */
    public String getDirectory() {
        return directory;
    }

    public CloneWithSparseCheckoutParams withDirectory(@NotNull String directory) {
        this.directory = directory;
        return this;
    }

    /** @return url to clone */
    public String getRemoteUrl() {
        return remoteUrl;
    }

    public CloneWithSparseCheckoutParams withRemoteUrl(@NotNull String remoteUrl) {
        this.remoteUrl = remoteUrl;
        return this;
    }

    /** @return branch to checkout */
    public String getBranch() {
        return branch;
    }

    public CloneWithSparseCheckoutParams withBranch(@NotNull String branch) {
        this.branch = branch;
        return this;
    }
}
