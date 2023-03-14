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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.RawTextComparator;
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
import org.sonar.scm.git.blame.BlameResult;
import org.sonar.scm.git.blame.RepositoryBlameCommand;
import org.sonar.scm.git.strategy.BlameStrategy;
import org.sonar.scm.git.strategy.DefaultBlameStrategy.BlameAlgorithmEnum;

import static java.util.Optional.ofNullable;
import static org.sonar.scm.git.strategy.DefaultBlameStrategy.BlameAlgorithmEnum.GIT_FILES_BLAME;

public class CompositeBlameCommand extends BlameCommand {
  private static final Logger LOG = Loggers.get(CompositeBlameCommand.class);

  private final AnalysisWarnings analysisWarnings;
  private final PathResolver pathResolver;
  private final JGitBlameCommand jgitCmd;
  private final NativeGitBlameCommand nativeCmd;
  private boolean nativeGitEnabled = false;

  private final BlameStrategy blameStrategy;

  public CompositeBlameCommand(AnalysisWarnings analysisWarnings, PathResolver pathResolver, JGitBlameCommand jgitCmd,
    NativeGitBlameCommand nativeCmd, BlameStrategy blameStrategy) {
    this.analysisWarnings = analysisWarnings;
    this.pathResolver = pathResolver;
    this.blameStrategy = blameStrategy;
    this.jgitCmd = jgitCmd;
    this.nativeCmd = nativeCmd;
  }

  @Override
  public void blame(BlameInput input, BlameOutput output) {
    File basedir = input.fileSystem().baseDir();
    try (Repository repo = JGitUtils.buildRepository(basedir.toPath())) {

      File gitBaseDir = repo.getWorkTree();
      if (cloneIsInvalid(gitBaseDir)) {
        return;
      }
      Profiler profiler = Profiler.create(LOG);
      profiler.startDebug("Collecting committed files");
      Map<String, InputFile> inputFileByGitRelativePath = getCommittedFilesToBlame(repo, gitBaseDir, input);
      profiler.stopDebug();

      BlameAlgorithmEnum blameAlgorithmEnum = this.blameStrategy.getBlameAlgorithm(Runtime.getRuntime().availableProcessors(), inputFileByGitRelativePath.size());
      LOG.debug("Using {} strategy to blame files", blameAlgorithmEnum);
      if (blameAlgorithmEnum == GIT_FILES_BLAME) {
        blameWithFilesGitCommand(output, repo, inputFileByGitRelativePath);
      } else {
        blameWithNativeGitCommand(output, repo, inputFileByGitRelativePath, gitBaseDir);
      }
    }
  }

  private Map<String, InputFile> getCommittedFilesToBlame(Repository repo, File gitBaseDir, BlameInput input) {
    Set<String> committedFiles = collectAllCommittedFiles(repo);
    Map<String, InputFile> inputFileByGitRelativePath = new HashMap<>();
    for (InputFile inputFile : input.filesToBlame()) {
      String relative = pathResolver.relativePath(gitBaseDir, inputFile.file());
      if (relative == null || !committedFiles.contains(relative)) {
        continue;
      }
      inputFileByGitRelativePath.put(relative, inputFile);
    }
    return inputFileByGitRelativePath;
  }

  private void blameWithNativeGitCommand(BlameOutput output, Repository repo, Map<String, InputFile> inputFileByGitRelativePath, File gitBaseDir) {
    try (Git git = Git.wrap(repo)) {
      nativeGitEnabled = nativeCmd.checkIfEnabled();
      ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new GitThreadFactory());

      for (Map.Entry<String, InputFile> e : inputFileByGitRelativePath.entrySet()) {
        InputFile inputFile = e.getValue();
        String filename = e.getKey();
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

  private static void blameWithFilesGitCommand(BlameOutput output, Repository repo, Map<String, InputFile> inputFileByGitRelativePath) {
    RepositoryBlameCommand blameCommand = new RepositoryBlameCommand(repo)
      .setTextComparator(RawTextComparator.WS_IGNORE_ALL)
      .setMultithreading(true)
      .setFilePaths(inputFileByGitRelativePath.keySet());
    try {
      BlameResult blameResult = blameCommand.call();

      for (Map.Entry<String, InputFile> e : inputFileByGitRelativePath.entrySet()) {
        BlameResult.FileBlame fileBlameResult = blameResult.getFileBlameByPath().get(e.getKey());

        if (fileBlameResult == null) {
          LOG.debug("Unable to blame file {}.", e.getValue().filename());
          continue;
        }

        saveBlameInformationForFileInTheOutput(fileBlameResult, e.getValue(), output);
      }
    } catch (GitAPIException e) {
      LOG.warn("There was an issue when interacting with git repository: " + e.getMessage(), e);
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

  private static void saveBlameInformationForFileInTheOutput(BlameResult.FileBlame fileBlame, InputFile file, BlameOutput output) {
    List<BlameLine> linesList = new ArrayList<>();
    for (int i = 0; i < fileBlame.lines(); i++) {
      if (fileBlame.getAuthorEmails()[i] == null || fileBlame.getCommitHashes()[i] == null || fileBlame.getCommitDates()[i] == null) {
        LOG.debug("Unable to blame file {}. No blame info at line {}. Is file committed? [Author: {} Source commit: {}]", file.filename());
        linesList.clear();
        break;
      }
      linesList.add(new BlameLine()
        .date(fileBlame.getCommitDates()[i])
        .revision(fileBlame.getCommitHashes()[i])
        .author(fileBlame.getAuthorEmails()[i]));
    }
    if (!linesList.isEmpty()) {
      if (linesList.size() == file.lines() - 1) {
        // SONARPLUGINS-3097 Git does not report blame on last empty line
        linesList.add(linesList.get(linesList.size() - 1));
      }
      output.blameResult(file, linesList);
    }
  }

}
