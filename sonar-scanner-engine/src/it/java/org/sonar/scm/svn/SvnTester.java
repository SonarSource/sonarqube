/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scm.svn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCopyClient;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc2.SvnList;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnRemoteMkDir;
import org.tmatesoft.svn.core.wc2.SvnTarget;

public class SvnTester {
  private final SVNClientManager manager = SVNClientManager.newInstance(new SvnOperationFactory());

  private final SVNURL localRepository;

  public SvnTester(Path root) throws SVNException, IOException {
    localRepository = SVNRepositoryFactory.createLocalRepository(root.toFile(), false, false);
    mkdir("trunk");
    mkdir("branches");
  }

  private void mkdir(String relpath) throws IOException, SVNException {
    SvnRemoteMkDir remoteMkDir = manager.getOperationFactory().createRemoteMkDir();
    remoteMkDir.addTarget(SvnTarget.fromURL(localRepository.appendPath(relpath, false)));
    remoteMkDir.run();
  }

  public void createBranch(String branchName) throws IOException, SVNException {
    SVNCopyClient copyClient = manager.getCopyClient();
    SVNCopySource source = new SVNCopySource(SVNRevision.HEAD, SVNRevision.HEAD, localRepository.appendPath("trunk", false));
    copyClient.doCopy(new SVNCopySource[] {source}, localRepository.appendPath("branches/" + branchName, false), false, false, true, "Create branch", null);
  }

  public void checkout(Path worktree, String path) throws SVNException {
    SVNUpdateClient updateClient = manager.getUpdateClient();
    updateClient.doCheckout(localRepository.appendPath(path, false),
      worktree.toFile(), null, null, SVNDepth.INFINITY, false);
  }

  public void add(Path worktree, String filename) throws SVNException {
    manager.getWCClient().doAdd(worktree.resolve(filename).toFile(), true, false, false, SVNDepth.INFINITY, false, false, true);
  }

  public void copy(Path worktree, String src, String dst) throws SVNException {
    SVNCopyClient copyClient = manager.getCopyClient();
    SVNCopySource source = new SVNCopySource(SVNRevision.HEAD, SVNRevision.HEAD, worktree.resolve(src).toFile());
    copyClient.doCopy(new SVNCopySource[]{source}, worktree.resolve(dst).toFile(), false, false, true);
  }

  public void commit(Path worktree) throws SVNException {
    manager.getCommitClient().doCommit(new File[] {worktree.toFile()}, false, "commit " + worktree, null, null, false, false, SVNDepth.INFINITY);
  }

  public void update(Path worktree) throws SVNException {
    manager.getUpdateClient().doUpdate(new File[] {worktree.toFile()}, SVNRevision.HEAD, SVNDepth.INFINITY, false, false);
  }

  public Collection<String> list(String... paths) throws SVNException {
    Set<String> results = new HashSet<>();

    SvnList list = manager.getOperationFactory().createList();
    if (paths.length == 0) {
      list.addTarget(SvnTarget.fromURL(localRepository));
    } else {
      for (String path : paths) {
        list.addTarget(SvnTarget.fromURL(localRepository.appendPath(path, false)));
      }
    }
    list.setDepth(SVNDepth.INFINITY);
    list.setReceiver((svnTarget, svnDirEntry) -> {
      String path = svnDirEntry.getRelativePath();
      if (!path.isEmpty()) {
        results.add(path);
      }
    });
    list.run();

    return results;
  }

  public void createFile(Path worktree, String filename, String content) throws IOException {
    Files.write(worktree.resolve(filename), content.getBytes());
  }

  public void createFile(Path worktree, String filename) throws IOException {
    createFile(worktree, filename, filename + "\n");
  }

  public void appendToFile(Path worktree, String filename) throws IOException {
    Files.write(worktree.resolve(filename), (filename + "\n").getBytes(), StandardOpenOption.APPEND);
  }

  public void deleteFile(Path worktree, String filename) throws SVNException {
    manager.getWCClient().doDelete(worktree.resolve(filename).toFile(), false, false);
  }
}
