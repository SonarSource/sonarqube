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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.scm.BlameCommand;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;

import static java.util.Optional.ofNullable;

public class CompositeBlameCommand extends BlameCommand {
  private static final Logger LOG = Loggers.get(CompositeBlameCommand.class);

  private final AnalysisWarnings analysisWarnings;
  private final PathResolver pathResolver;
  private final JGitBlameCommand jgitCmd;
  private final GitBlameCommand nativeCmd;
  private boolean nativeGitEnabled = false;

  public CompositeBlameCommand(AnalysisWarnings analysisWarnings, PathResolver pathResolver, JGitBlameCommand jgitCmd, GitBlameCommand nativeCmd) {
    this.analysisWarnings = analysisWarnings;
    this.pathResolver = pathResolver;
    this.jgitCmd = jgitCmd;
    this.nativeCmd = nativeCmd;
  }

  @Override
  public void blame(BlameInput input, BlameOutput output) {
    File basedir = input.fileSystem().baseDir();
    try (Repository repo = JGitUtils.buildRepository(basedir.toPath()); Git git = Git.wrap(repo)) {
      File gitBaseDir = repo.getWorkTree();
      if (cloneIsInvalid(gitBaseDir)) {
        return;
      }
      Profiler profiler = Profiler.create(LOG);
      profiler.startDebug("Collecting committed files");
      Set<String> committedFiles = collectAllCommittedFiles(repo);
      profiler.stopDebug();
      nativeGitEnabled = nativeCmd.checkIfEnabled();
      ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new GitThreadFactory());

      for (InputFile inputFile : input.filesToBlame()) {
        String filename = pathResolver.relativePath(gitBaseDir, inputFile.file());
        if (filename == null || !committedFiles.contains(filename)) {
          continue;
        }
        // exceptions thrown by the blame method will be ignored
        executorService.submit(() -> blame(output, git, gitBaseDir, inputFile, filename));
      }

      executorService.shutdown();
      try {
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        LOG.info("Git blame interrupted", e);
        Thread.currentThread().interrupt();
      }
    }
  }

  private static Set<String> collectAllCommittedFiles(Repository repo) {
    try {
      Set<String> files = new HashSet<>();
      Optional<ObjectId> headCommit = ofNullable(repo.resolve(Constants.HEAD));

      if (headCommit.isEmpty()) {
        LOG.warn("Could not find HEAD commit");
        return files;
      }

      try (RevWalk revWalk = new RevWalk(repo)) {
        RevCommit head = revWalk.parseCommit(headCommit.get());
        try (TreeWalk treeWalk = new TreeWalk(repo)) {
          treeWalk.addTree(head.getTree());
          treeWalk.setRecursive(true);

          while (treeWalk.next()) {
            String path = treeWalk.getPathString();
            files.add(path);
          }
        }
      }
      return files;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to find all committed files", e);
    }
  }

  private void blame(BlameOutput output, Git git, File gitBaseDir, InputFile inputFile, String filename) {
    List<BlameLine> blame = null;
    if (nativeGitEnabled) {
      try {
        LOG.debug("Blame file (native) {}", filename);
        blame = nativeCmd.blame(gitBaseDir.toPath(), filename);
      } catch (Exception e) {
        LOG.debug("Native git blame failed. Falling back to jgit: " + filename, e);
        nativeGitEnabled = false;
      }
    }

    if (blame == null) {
      LOG.debug("Blame file (JGit) {}", filename);
      blame = jgitCmd.blame(git, filename);
    }

    if (!blame.isEmpty()) {
      if (blame.size() == inputFile.lines() - 1) {
        // SONARPLUGINS-3097 Git does not report blame on last empty line
        blame.add(blame.get(blame.size() - 1));
      }
      output.blameResult(inputFile, blame);
    }
  }

  private boolean cloneIsInvalid(File gitBaseDir) {
    if (Files.isRegularFile(gitBaseDir.toPath().resolve(".git/objects/info/alternates"))) {
      LOG.info("This git repository references another local repository which is not well supported. SCM information might be missing for some files. "
        + "You can avoid borrow objects from another local repository by not using --reference or --shared when cloning it.");
    }

    if (Files.isRegularFile(gitBaseDir.toPath().resolve(".git/shallow"))) {
      LOG.warn("Shallow clone detected, no blame information will be provided. " + "You can convert to non-shallow with 'git fetch --unshallow'.");
      analysisWarnings.addUnique("Shallow clone detected during the analysis. " + "Some files will miss SCM information. This will affect features like auto-assignment of issues. "
        + "Please configure your build to disable shallow clone.");
      return true;
    }

    return false;
  }
}
