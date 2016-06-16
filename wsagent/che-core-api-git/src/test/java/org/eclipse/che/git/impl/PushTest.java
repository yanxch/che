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
import org.eclipse.che.api.git.params.CheckoutParams;
import org.eclipse.che.api.git.params.CloneParams;
import org.eclipse.che.api.git.params.CommitParams;
import org.eclipse.che.api.git.params.PushParams;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import static org.eclipse.che.git.impl.GitTestUtil.addFile;
import static org.eclipse.che.git.impl.GitTestUtil.cleanupTestRepo;
import static org.eclipse.che.git.impl.GitTestUtil.connectToInitializedGitRepository;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Eugene Voevodin
 */
public class PushTest {

    private File repository;
    private File remoteRepo;

    @BeforeMethod
    public void setUp() {
        repository = Files.createTempDir();
        remoteRepo = Files.createTempDir();
    }

    @AfterMethod
    public void cleanUp() {
        cleanupTestRepo(repository);
        cleanupTestRepo(remoteRepo);
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testSimplePush(GitConnectionFactory connectionFactory)
            throws IOException, ServerException, URISyntaxException, UnauthorizedException {
        //given
        GitConnection connection = connectToInitializedGitRepository(connectionFactory, repository);
        GitConnection remoteConnection = connectionFactory.getConnection(remoteRepo.getAbsolutePath());
        remoteConnection.clone(CloneParams.create(connection.getWorkingDir().getAbsolutePath())
                                          .withWorkingDir(remoteConnection.getWorkingDir().getAbsolutePath()));
        addFile(remoteConnection, "newfile", "content");
        remoteConnection.add(AddParams.create(Arrays.asList(".")));
        remoteConnection.commit(CommitParams.create("Fake commit"));
        //when
        remoteConnection.push(PushParams.create("origin")
                                        .withRefSpec(Arrays.asList("refs/heads/master:refs/heads/test"))
                                        .withTimeout(-1));
        //then
        //check branches in origin repository
        assertEquals(connection.branchList(null).size(), 1);
        //checkout test branch
        connection.checkout(CheckoutParams.create("test"));
        assertTrue(new File(connection.getWorkingDir(), "newfile").exists());
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testPushRemote(GitConnectionFactory connectionFactory)
            throws GitException, IOException, URISyntaxException, UnauthorizedException {
        //given
        GitConnection connection = connectToInitializedGitRepository(connectionFactory, repository);
        GitConnection remoteConnection = connectToInitializedGitRepository(connectionFactory, remoteRepo);
        addFile(connection, "README", "README");
        connection.add(AddParams.create(Arrays.asList(".")));
        connection.commit(CommitParams.create("Init commit."));
        //make push
        int branchesBefore = remoteConnection.branchList(null).size();
        //when
        connection.push(PushParams.create(remoteRepo.getAbsolutePath()).withRefSpec(Arrays.asList("refs/heads/master:refs/heads/test"))
                                  .withTimeout(-1));
        //then
        int branchesAfter = remoteConnection.branchList(null).size();
        assertEquals(branchesAfter - 1, branchesBefore);
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = GitConnectionFactoryProvider.class,
            expectedExceptions = GitException.class, expectedExceptionsMessageRegExp = "No remote repository specified.  " +
            "Please, specify either a URL or a remote name from which new revisions should be fetched in request.")
    public void testWhenThereAreNoAnyRemotes(GitConnectionFactory connectionFactory) throws Exception {
        //given
        GitConnection connection = connectToInitializedGitRepository(connectionFactory, repository);

        //when
        connection.push(PushParams.create(null));
    }
}
