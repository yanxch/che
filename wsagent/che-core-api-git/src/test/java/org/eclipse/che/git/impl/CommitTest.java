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

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import org.eclipse.che.api.git.GitConnection;
import org.eclipse.che.api.git.GitConnectionFactory;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.params.AddParams;
import org.eclipse.che.api.git.params.CommitParams;
import org.eclipse.che.api.git.params.LogParams;
import org.eclipse.che.api.git.shared.AddRequest;
import org.eclipse.che.api.git.shared.Revision;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.eclipse.che.git.impl.GitTestUtil.addFile;
import static org.eclipse.che.git.impl.GitTestUtil.cleanupTestRepo;
import static org.eclipse.che.git.impl.GitTestUtil.connectToInitializedGitRepository;
import static org.testng.Assert.assertEquals;

/**
 * @author Eugene Voevodin
 */
public class
CommitTest {
    private File repository;
    private String CONTENT = "git repository content\n";

    @BeforeMethod
    public void setUp() {
        repository = Files.createTempDir();
    }

    @AfterMethod
    public void cleanUp() {
        cleanupTestRepo(repository);
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testSimpleCommit(GitConnectionFactory connectionFactory) throws GitException, IOException {
        //given
        GitConnection connection = connectToInitializedGitRepository(connectionFactory, repository);
        //add new File
        addFile(connection, "DONTREADME", "secret");
        //add changes
        connection.add(AddParams.create(AddRequest.DEFAULT_PATTERN));

        //when
        CommitParams params = CommitParams.create("Commit message").withAmend(false).withAll(false);
        Revision revision = connection.commit(params);

        //then
        assertEquals(revision.getMessage(), params.getMessage());
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testCommitWithAddAll(GitConnectionFactory connectionFactory) throws GitException, IOException {
        //given
        GitConnection connection = connectToInitializedGitRepository(connectionFactory, repository);
        addFile(connection, "README.txt", CONTENT);
        connection.add(AddParams.create(ImmutableList.of("README.txt")));
        connection.commit(CommitParams.create("Initial addd"));

        //when
        //change existing README
        addFile(connection, "README.txt", "not secret");

        //then
        CommitParams params = CommitParams.create("Other commit message").withAmend(false).withAll(true);
        Revision revision = connection.commit(params);
        assertEquals(revision.getMessage(), params.getMessage());
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testAmendCommit(GitConnectionFactory connectionFactory) throws GitException, IOException {
        //given
        GitConnection connection = connectToInitializedGitRepository(connectionFactory, repository);
        addFile(connection, "README.txt", CONTENT);
        connection.add(AddParams.create(ImmutableList.of("README.txt")));
        connection.commit(CommitParams.create("Initial addd"));
        int beforeCount = connection.log(LogParams.create()).getCommits().size();

        //when
        //change existing README
        addFile(connection, "README.txt", "some new content");
        CommitParams params = CommitParams.create("Amend commit").withAmend(true).withAll(true);

        //then
        Revision revision = connection.commit(params);
        int afterCount = connection.log(LogParams.create()).getCommits().size();
        assertEquals(revision.getMessage(), params.getMessage());
        assertEquals(beforeCount, afterCount);
    }
}
