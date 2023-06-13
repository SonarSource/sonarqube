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

import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.sonar.api.batch.scm.BlameCommand;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.documentation.DocumentationLinkGenerator;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.eclipse.jgit.diff.DiffEntry.ChangeType.ADD;
import static org.eclipse.jgit.diff.DiffEntry.ChangeType.MODIFY;
import static org.eclipse.jgit.diff.DiffEntry.ChangeType.RENAME;

public class GitScmProvider extends ScmProvider {

  private static final Logger LOG = LoggerFactory.getLogger(GitScmProvider.class);
  private static final String COULD_NOT_FIND_REF = "Could not find ref '%s' in refs/heads, refs/remotes, refs/remotes/upstream or refs/remotes/origin";
  private static final String NO_MERGE_BASE_FOUND_MESSAGE = "No merge base found between HEAD and %s";
  @VisibleForTesting
  static final String SCM_INTEGRATION_DOCUMENTATION_SUFFIX = "/analyzing-source-code/scm-integration/";
  private final BlameCommand blameCommand;
  private final AnalysisWarnings analysisWarnings;
  private final GitIgnoreCommand gitIgnoreCommand;
  private final System2 system2;
  private final DocumentationLinkGenerator documentationLinkGenerator;

  public GitScmProvider(CompositeBlameCommand blameCommand, AnalysisWarnings analysisWarnings, GitIgnoreCommand gitIgnoreCommand, System2 system2,
    DocumentationLinkGenerator documentationLinkGenerator) {

    this.blameCommand = blameCommand;
    this.analysisWarnings = analysisWarnings;
    this.gitIgnoreCommand = gitIgnoreCommand;
    this.system2 = system2;
    this.documentationLinkGenerator = documentationLinkGenerator;
  }

  @Override
  public GitIgnoreCommand ignoreCommand() {
    return gitIgnoreCommand;
  }

  @Override
  public String key() {
    return "git";
  }

  @Override
  public boolean supports(File baseDir) {
    RepositoryBuilder builder = new RepositoryBuilder().findGitDir(baseDir);
    return builder.getGitDir() != null;
  }

  @Override
  public BlameCommand blameCommand() {
    return this.blameCommand;
  }

  @CheckForNull
  @Override
  public Set<Path> branchChangedFiles(String targetBranchName, Path rootBaseDir) {
    return Optional.ofNullable((branchChangedFilesWithFileMovementDetection(targetBranchName, rootBaseDir)))
      .map(GitScmProvider::extractAbsoluteFilePaths)
      .orElse(null);
  }

  @CheckForNull
  public Set<ChangedFile> branchChangedFilesWithFileMovementDetection(String targetBranchName, Path rootBaseDir) {
    try (Repository repo = buildRepo(rootBaseDir)) {
      Ref targetRef = resolveTargetRef(targetBranchName, repo);
      if (targetRef == null) {
        addWarningTargetNotFound(targetBranchName);
        return null;
      }

      if (isDiffAlgoInvalid(repo.getConfig())) {
        LOG.warn("The diff algorithm configured in git is not supported. "
          + "No information regarding changes in the branch will be collected, which can lead to unexpected results.");
        return null;
      }

      Optional<RevCommit> mergeBaseCommit = findMergeBase(repo, targetRef);
      if (mergeBaseCommit.isEmpty()) {
        LOG.warn(composeNoMergeBaseFoundWarning(targetRef.getName()));
        return null;
      }
      AbstractTreeIterator mergeBaseTree = prepareTreeParser(repo, mergeBaseCommit.get());

      // we compare a commit with HEAD, so no point ignoring line endings (it will be whatever is committed)
      try (Git git = newGit(repo)) {
        List<DiffEntry> diffEntries = git.diff()
          .setShowNameAndStatusOnly(true)
          .setOldTree(mergeBaseTree)
          .setNewTree(prepareNewTree(repo))
          .call();

        return computeChangedFiles(repo, diffEntries);
      }
    } catch (IOException | GitAPIException e) {
      LOG.warn(e.getMessage(), e);
    }
    return null;
  }

  private static Set<ChangedFile> computeChangedFiles(Repository repository, List<DiffEntry> diffEntries) throws IOException {
    Path workingDirectory = repository.getWorkTree().toPath();

    Map<String, String> renamedFilePaths = computeRenamedFilePaths(repository, diffEntries);
    Set<String> changedFilePaths = computeChangedFilePaths(diffEntries);

    return collectChangedFiles(workingDirectory, renamedFilePaths, changedFilePaths);
  }

  private static Set<ChangedFile> collectChangedFiles(Path workingDirectory, Map<String, String> renamedFilePaths, Set<String> changedFilePaths) {
    Set<ChangedFile> changedFiles = new HashSet<>();
    changedFilePaths.forEach(filePath -> changedFiles.add(ChangedFile.of(workingDirectory.resolve(filePath), renamedFilePaths.get(filePath))));
    return changedFiles;
  }

  private static Map<String, String> computeRenamedFilePaths(Repository repository, List<DiffEntry> diffEntries) throws IOException {
    RenameDetector renameDetector = new RenameDetector(repository);
    renameDetector.addAll(diffEntries);

    return renameDetector
      .compute()
      .stream()
      .filter(entry -> RENAME.equals(entry.getChangeType()))
      .collect(toUnmodifiableMap(DiffEntry::getNewPath, DiffEntry::getOldPath));
  }

  private static Set<String> computeChangedFilePaths(List<DiffEntry> diffEntries) {
    return diffEntries
      .stream()
      .filter(isAllowedChangeType(ADD, MODIFY))
      .map(DiffEntry::getNewPath)
      .collect(toSet());
  }

  private static Predicate<DiffEntry> isAllowedChangeType(ChangeType... changeTypes) {
    Function<ChangeType, Predicate<DiffEntry>> isChangeType = type -> entry -> type.equals(entry.getChangeType());

    return Arrays
      .stream(changeTypes)
      .map(isChangeType)
      .reduce(x -> false, Predicate::or);
  }

  @CheckForNull
  @Override
  public Map<Path, Set<Integer>> branchChangedLines(String targetBranchName, Path projectBaseDir, Set<Path> changedFiles) {
    return branchChangedLinesWithFileMovementDetection(targetBranchName, projectBaseDir, toChangedFileByPathsMap(changedFiles));
  }

  @CheckForNull
  public Map<Path, Set<Integer>> branchChangedLinesWithFileMovementDetection(String targetBranchName, Path projectBaseDir, Map<Path, ChangedFile> changedFiles) {
    try (Repository repo = buildRepo(projectBaseDir)) {
      Ref targetRef = resolveTargetRef(targetBranchName, repo);
      if (targetRef == null) {
        addWarningTargetNotFound(targetBranchName);
        return null;
      }

      if (isDiffAlgoInvalid(repo.getConfig())) {
        // we already print a warning when branchChangedFiles is called
        return null;
      }

      // force ignore different line endings when comparing a commit with the workspace
      repo.getConfig().setBoolean("core", null, "autocrlf", true);

      Optional<RevCommit> mergeBaseCommit = findMergeBase(repo, targetRef);

      if (mergeBaseCommit.isEmpty()) {
        LOG.warn(composeNoMergeBaseFoundWarning(targetRef.getName()));
        return null;
      }

      Map<Path, Set<Integer>> changedLines = new HashMap<>();

      for (Map.Entry<Path, ChangedFile> entry : changedFiles.entrySet()) {
        collectChangedLines(repo, mergeBaseCommit.get(), changedLines, entry.getKey(), entry.getValue());
      }

      return changedLines;
    } catch (Exception e) {
      LOG.warn("Failed to get changed lines from git", e);
    }

    return null;
  }

  private static String composeNoMergeBaseFoundWarning(String targetRef) {
    return format(NO_MERGE_BASE_FOUND_MESSAGE, targetRef);
  }

  private void addWarningTargetNotFound(String targetBranchName) {
    String url = documentationLinkGenerator.getDocumentationLink(SCM_INTEGRATION_DOCUMENTATION_SUFFIX);
    analysisWarnings.addUnique(format(COULD_NOT_FIND_REF
      + ". You may see unexpected issues and changes. "
      + "Please make sure to fetch this ref before pull request analysis and refer to"
      + " <a href=\"%s\" rel=\"noopener noreferrer\" target=\"_blank\">the documentation</a>.", targetBranchName, url));
  }

  private void collectChangedLines(Repository repo, RevCommit mergeBaseCommit, Map<Path, Set<Integer>> changedLines, Path changedFilePath, ChangedFile changedFile) {
    ChangedLinesComputer computer = new ChangedLinesComputer();

    try (DiffFormatter diffFmt = new DiffFormatter(new BufferedOutputStream(computer.receiver()))) {
      diffFmt.setRepository(repo);
      diffFmt.setProgressMonitor(NullProgressMonitor.INSTANCE);
      diffFmt.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);

      diffFmt.setDetectRenames(changedFile.isMovedFile());

      Path workTree = repo.getWorkTree().toPath();
      TreeFilter treeFilter = getTreeFilter(changedFile, workTree);
      diffFmt.setPathFilter(treeFilter);

      AbstractTreeIterator mergeBaseTree = prepareTreeParser(repo, mergeBaseCommit);
      List<DiffEntry> diffEntries = diffFmt.scan(mergeBaseTree, new FileTreeIterator(repo));

      diffFmt.format(diffEntries);
      diffFmt.flush();

      diffEntries.stream()
        .filter(isAllowedChangeType(ADD, MODIFY, RENAME))
        .findAny()
        .ifPresent(diffEntry -> changedLines.put(changedFilePath, computer.changedLines()));
    } catch (Exception e) {
      LOG.warn("Failed to get changed lines from git for file " + changedFilePath, e);
    }
  }

  @Override
  @CheckForNull
  public Instant forkDate(String referenceBranchName, Path projectBaseDir) {
    return null;
  }

  private static String toGitPath(String path) {
    return path.replaceAll(Pattern.quote(File.separator), "/");
  }

  private static TreeFilter getTreeFilter(ChangedFile changedFile, Path baseDir) {
    String path = toGitPath(relativizeFilePath(baseDir, changedFile.getAbsolutFilePath()));
    String oldRelativePath = changedFile.getOldRelativeFilePathReference();

    if (oldRelativePath != null) {
      return PathFilterGroup.createFromStrings(path, toGitPath(oldRelativePath));
    }

    return PathFilter.create(path);
  }

  private static Set<Path> extractAbsoluteFilePaths(Collection<ChangedFile> changedFiles) {
    return changedFiles
      .stream()
      .map(ChangedFile::getAbsolutFilePath)
      .collect(toSet());
  }

  @CheckForNull
  private Ref resolveTargetRef(String targetBranchName, Repository repo) throws IOException {
    String localRef = "refs/heads/" + targetBranchName;
    String remotesRef = "refs/remotes/" + targetBranchName;
    String originRef = "refs/remotes/origin/" + targetBranchName;
    String upstreamRef = "refs/remotes/upstream/" + targetBranchName;

    Ref targetRef;
    // Because circle ci destroys the local reference to master, try to load remote ref first.
    // https://discuss.circleci.com/t/git-checkout-of-a-branch-destroys-local-reference-to-master/23781
    if (runningOnCircleCI()) {
      targetRef = getFirstExistingRef(repo, originRef, localRef, upstreamRef, remotesRef);
    } else {
      targetRef = getFirstExistingRef(repo, localRef, originRef, upstreamRef, remotesRef);
    }

    if (targetRef == null) {
      LOG.warn(String.format(COULD_NOT_FIND_REF, targetBranchName));
    }

    return targetRef;
  }

  @CheckForNull
  private static Ref getFirstExistingRef(Repository repo, String... refs) throws IOException {
    Ref targetRef = null;
    for (String ref : refs) {
      targetRef = repo.exactRef(ref);
      if (targetRef != null) {
        break;
      }
    }
    return targetRef;
  }

  private boolean runningOnCircleCI() {
    return "true".equals(system2.envVariable("CIRCLECI"));
  }

  @Override
  public Path relativePathFromScmRoot(Path path) {
    RepositoryBuilder builder = getVerifiedRepositoryBuilder(path);
    return builder.getGitDir().toPath().getParent().relativize(path);
  }

  @Override
  @CheckForNull
  public String revisionId(Path path) {
    RepositoryBuilder builder = getVerifiedRepositoryBuilder(path);
    try {
      return Optional.ofNullable(getHead(builder.build()))
        .map(Ref::getObjectId)
        .map(ObjectId::getName)
        .orElse(null);
    } catch (IOException e) {
      throw new IllegalStateException("I/O error while getting revision ID for path: " + path, e);
    }
  }

  private static boolean isDiffAlgoInvalid(Config cfg) {
    try {
      DiffAlgorithm.getAlgorithm(cfg.getEnum(
        ConfigConstants.CONFIG_DIFF_SECTION, null,
        ConfigConstants.CONFIG_KEY_ALGORITHM,
        DiffAlgorithm.SupportedAlgorithm.HISTOGRAM));
      return false;
    } catch (IllegalArgumentException e) {
      return true;
    }
  }

  private static AbstractTreeIterator prepareNewTree(Repository repo) throws IOException {
    CanonicalTreeParser treeParser = new CanonicalTreeParser();
    try (ObjectReader objectReader = repo.newObjectReader()) {
      Ref head = getHead(repo);
      if (head == null) {
        throw new IOException("HEAD reference not found");
      }
      treeParser.reset(objectReader, repo.parseCommit(head.getObjectId()).getTree());
    }
    return treeParser;
  }

  @CheckForNull
  private static Ref getHead(Repository repo) throws IOException {
    return repo.exactRef("HEAD");
  }

  private static Optional<RevCommit> findMergeBase(Repository repo, Ref targetRef) throws IOException {
    try (RevWalk walk = new RevWalk(repo)) {
      Ref head = getHead(repo);
      if (head == null) {
        throw new IOException("HEAD reference not found");
      }

      walk.markStart(walk.parseCommit(targetRef.getObjectId()));
      walk.markStart(walk.parseCommit(head.getObjectId()));
      walk.setRevFilter(RevFilter.MERGE_BASE);
      RevCommit next = walk.next();
      if (next == null) {
        return Optional.empty();
      }
      RevCommit base = walk.parseCommit(next);
      walk.dispose();
      LOG.info("Merge base sha1: {}", base.getName());
      return Optional.of(base);
    }
  }

  private static Map<Path, ChangedFile> toChangedFileByPathsMap(Set<Path> changedFiles) {
    return changedFiles
      .stream()
      .collect(toMap(identity(), ChangedFile::of, (x, y) -> y, LinkedHashMap::new));
  }

  private static String relativizeFilePath(Path baseDirectory, Path filePath) {
    return baseDirectory.relativize(filePath).toString();
  }

  AbstractTreeIterator prepareTreeParser(Repository repo, RevCommit commit) throws IOException {
    CanonicalTreeParser treeParser = new CanonicalTreeParser();
    try (ObjectReader objectReader = repo.newObjectReader()) {
      treeParser.reset(objectReader, commit.getTree());
    }
    return treeParser;
  }

  Git newGit(Repository repo) {
    return new Git(repo);
  }

  Repository buildRepo(Path basedir) throws IOException {
    return getVerifiedRepositoryBuilder(basedir).build();
  }

  static RepositoryBuilder getVerifiedRepositoryBuilder(Path basedir) {
    RepositoryBuilder builder = new RepositoryBuilder()
      .findGitDir(basedir.toFile())
      .setMustExist(true);

    if (builder.getGitDir() == null) {
      throw MessageException.of("Not inside a Git work tree: " + basedir);
    }
    return builder;
  }
}
