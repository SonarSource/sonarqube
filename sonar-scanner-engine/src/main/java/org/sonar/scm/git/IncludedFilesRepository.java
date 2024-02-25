/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class IncludedFilesRepository {

  private static final Logger LOG = Loggers.get(IncludedFilesRepository.class);
  private final Set<Path> includedFiles = new HashSet<>();

    public IncludedFilesRepository(Path baseDir, boolean analyseSubmodules) throws IOException {
        indexFiles(baseDir, analyseSubmodules);
        LOG.debug("{} non excluded files in this Git repository", includedFiles.size());
    }

  public boolean contains(Path absolutePath) {
    return includedFiles.contains(absolutePath);
  }

    private void indexFiles(Path baseDir, boolean analyseSubmodules) throws IOException {

        try (Repository repository = org.sonar.scm.git.JGitUtils.buildRepository(baseDir)) {

            LOG.debug("Indexing repository located at {}", baseDir);

            getFilesInRepo(baseDir, repository);

            Git git = new Git(repository);
            if (!git.submoduleStatus().call().isEmpty() && analyseSubmodules) {

                LOG.debug("Trying to index files in submodules");

                for (String submodule : git.submoduleStatus().call().keySet()) {

                    LOG.debug("Collecting files in {}", submodule);

                    Repository submoduleRepository = SubmoduleWalk.getSubmoduleRepository(repository, submodule);
                    if (submoduleRepository != null) {
                        getFilesInRepo(submoduleRepository.getWorkTree().toPath(), submoduleRepository);
                    } else {
                        LOG.info("Submodule {} given, failed to get submodule repository, is it not checked out?", submodule);
                    }
                }
            }
        } catch (GitAPIException e) {
            LOG.error("Failed to access repository when collecting files", e);
        }
    }

    private void getFilesInRepo(Path baseDir, Repository repository) throws IOException {

        try (TreeWalk treeWalk = new TreeWalk(repository)) {

            Path workTreeRoot = repository.getWorkTree().toPath();
            FileTreeIterator workingTreeIt = new FileTreeIterator(repository);
            treeWalk.setRecursive(true);

            if (!baseDir.equals(workTreeRoot)) {
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
}
