/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.scm.BlameCommand;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

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
      nativeGitEnabled = nativeCmd.isEnabled(basedir.toPath());
      Stream<InputFile> stream = StreamSupport.stream(input.filesToBlame().spliterator(), true);
      ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors(), new GitThreadFactory(), null, false);
      // exceptions thrown by the blame method will be ignored
      forkJoinPool.submit(() -> stream.forEach(inputFile -> blame(output, git, gitBaseDir, inputFile)));
      try {
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        LOG.info("Git blame interrupted");
      }
    }
  }

  private void blame(BlameOutput output, Git git, File gitBaseDir, InputFile inputFile) {
    String filename = pathResolver.relativePath(gitBaseDir, inputFile.file());
    LOG.debug("Blame file {}", filename);
    List<BlameLine> blame = null;
    if (nativeGitEnabled) {
      try {
        blame = nativeCmd.blame(gitBaseDir.toPath(), filename);
      } catch (Exception e) {
        // fallback to jgit
      }
    }

    if (blame == null) {
      blame = jgitCmd.blame(git, filename);
    }

    if (!blame.isEmpty()) {
      if (blame.size() == inputFile.lines() - 1) {
        // SONARPLUGINS-3097 Git do not report blame on last empty line
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
      LOG.warn("Shallow clone detected, no blame information will be provided. "
        + "You can convert to non-shallow with 'git fetch --unshallow'.");
      analysisWarnings.addUnique("Shallow clone detected during the analysis. "
        + "Some files will miss SCM information. This will affect features like auto-assignment of issues. "
        + "Please configure your build to disable shallow clone.");
      return true;
    }

    return false;
  }
}
