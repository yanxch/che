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
package org.eclipse.che.git.impl;

import com.google.common.io.Files;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.git.GitConnection;
import org.eclipse.che.api.git.GitConnectionFactory;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.params.AddParams;
import org.eclipse.che.api.git.params.CloneParams;
import org.eclipse.che.api.git.params.CommitParams;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import static org.eclipse.che.git.impl.GitTestUtil.addFile;
import static org.eclipse.che.git.impl.GitTestUtil.cleanupTestRepo;
import static org.eclipse.che.git.impl.GitTestUtil.connectToGitRepositoryWithContent;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

/**
 * @author Igor Vinokur
 */
public class CloneTest {

    private File localRepo;
    private File remoteRepo;

    @BeforeMethod
    public void setUp() {
        localRepo = Files.createTempDir();
        remoteRepo = Files.createTempDir();
    }

    @AfterMethod
    public void cleanUp() {
        cleanupTestRepo(localRepo);
        cleanupTestRepo(remoteRepo);
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = GitConnectionFactoryProvider.class)
    public void testSimpleClone(GitConnectionFactory connectionFactory)
            throws ServerException, IOException, UnauthorizedException, URISyntaxException {
        //given
        GitConnection remoteConnection = connectToGitRepositoryWithContent(connectionFactory, remoteRepo);
        GitConnection localConnection = connectionFactory.getConnection(localRepo.getAbsolutePath());
        int filesBefore = localRepo.listFiles().length;

        //when
        localConnection.clone(CloneParams.create(remoteConnection.getWorkingDir().getAbsolutePath()));

        //then
        int filesAfter = localRepo.listFiles().length;
        assertEquals(filesAfter, filesBefore + 2);
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = GitConnectionFactoryProvider.class)
    public void testCloneWithSparceCheckout(GitConnectionFactory connectionFactory)
            throws ServerException, IOException, UnauthorizedException, URISyntaxException {
        //given
        GitConnection remoteConnection = connectToGitRepositoryWithContent(connectionFactory, remoteRepo);
        addFile(remoteConnection.getWorkingDir().toPath().resolve("subdirectory"), "newFile", "content");
        addFile(remoteConnection.getWorkingDir().toPath().resolve("otherDirectory"), "newFile", "content");
        remoteConnection.add(AddParams.create(Arrays.asList(".")));
        remoteConnection.commit(CommitParams.create("add subdirectory"));
        GitConnection localConnection = connectionFactory.getConnection(localRepo.getAbsolutePath());

        //when
        localConnection.cloneWithSparseCheckout("subdirectory", remoteConnection.getWorkingDir().getAbsolutePath());

        //then
        assertFalse(new File(localRepo.getAbsolutePath() + "/README.txt").exists());
        assertFalse(new File(localRepo.getAbsolutePath() + "/otherDirectory").exists());
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = GitConnectionFactoryProvider.class,
          expectedExceptions = GitException.class,
          expectedExceptionsMessageRegExp = "Subdirectory for sparse-checkout is not specified")
    public void testCloneWithSparceCheckoutWithoutSpecifyingDirectory(GitConnectionFactory connectionFactory)
            throws ServerException, IOException, UnauthorizedException, URISyntaxException {
        //given
        GitConnection remoteConnection = connectToGitRepositoryWithContent(connectionFactory, remoteRepo);
        GitConnection localConnection = connectionFactory.getConnection(localRepo.getAbsolutePath());

        //when
        localConnection.cloneWithSparseCheckout(null, remoteConnection.getWorkingDir().getAbsolutePath());
    }
}
