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
package org.sonar.scm.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IncludedFilesRepository {

  private static final Logger LOG = LoggerFactory.getLogger(IncludedFilesRepository.class);
  private final Set<Path> includedFiles = new HashSet<>();

  public IncludedFilesRepository(Path baseDir) throws IOException {
    indexFiles(baseDir);
    LOG.debug("{} non excluded files in this Git repository", includedFiles.size());
  }

  public boolean contains(Path absolutePath) {
    return includedFiles.contains(absolutePath);
  }

  private void indexFiles(Path baseDir) throws IOException {
    try (Repository repo = JGitUtils.buildRepository(baseDir)) {
      collectFilesIterative(repo, baseDir);
    }
  }

  private void collectFiles(Repository repo, Path baseDir) throws IOException {
    Path workTreeRoot = repo.getWorkTree().toPath();
    FileTreeIterator workingTreeIt = new FileTreeIterator(repo);
    try (TreeWalk treeWalk = new TreeWalk(repo)) {
      treeWalk.setRecursive(true);
      // with submodules, the baseDir may be the parent of the workTreeRoot. In that case, we don't want to set a filter.
      if (!workTreeRoot.equals(baseDir) && baseDir.startsWith(workTreeRoot)) {
        Path relativeBaseDir = workTreeRoot.relativize(baseDir);
        treeWalk.setFilter(PathFilterGroup.createFromStrings(relativeBaseDir.toString().replace('\\', '/')));
      }
      treeWalk.addTree(workingTreeIt);
      while (treeWalk.next()) {

        WorkingTreeIterator workingTreeIterator = treeWalk
          .getTree(0, WorkingTreeIterator.class);

        if (!workingTreeIterator.isEntryIgnored()) {
          includedFiles.add(workTreeRoot.resolve(treeWalk.getPathString()));
        }
      }
    }
  }

  private void collectFilesIterative(Repository repo, Path baseDir) throws IOException {
    collectFiles(repo, baseDir);

    try (SubmoduleWalk submoduleWalk = SubmoduleWalk.forIndex(repo)) {
      while (submoduleWalk.next()) {
        try (Repository submoduleRepo = submoduleWalk.getRepository()) {
          if (submoduleRepo == null) {
            LOG.debug("Git submodule [{}] found, but has not been cloned, skipping.", submoduleWalk.getPath());
            continue;
          }
          collectFilesIterative(submoduleRepo, baseDir);
        }
      }
    }
  }

}
