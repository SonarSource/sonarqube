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
package org.sonar.scanner.scan.filesystem;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.scm.IgnoreCommand;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.bootstrap.GlobalServerSettings;
import org.sonar.scanner.fs.InputModuleHierarchy;
import org.sonar.scanner.scan.ModuleConfiguration;
import org.sonar.scanner.scan.ModuleConfigurationProvider;
import org.sonar.scanner.scan.ProjectServerSettings;
import org.sonar.scanner.scan.SonarGlobalPropertiesFilter;
import org.sonar.scanner.scm.ScmConfiguration;
import org.sonar.scanner.util.ProgressReport;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Index project input files into {@link InputComponentStore}.
 */
public class ProjectFileIndexer {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectFileIndexer.class);
  private final ProjectExclusionFilters projectExclusionFilters;
  private final SonarGlobalPropertiesFilter sonarGlobalPropertiesFilter;
  private final ProjectCoverageAndDuplicationExclusions projectCoverageAndDuplicationExclusions;
  private final ScmConfiguration scmConfiguration;
  private final InputComponentStore componentStore;
  private final InputModuleHierarchy inputModuleHierarchy;
  private final GlobalConfiguration globalConfig;
  private final GlobalServerSettings globalServerSettings;
  private final ProjectServerSettings projectServerSettings;
  private final FileIndexer fileIndexer;
  private final IgnoreCommand ignoreCommand;
  private final boolean useScmExclusion;
  private final AnalysisWarnings analysisWarnings;

  private ProgressReport progressReport;

  public ProjectFileIndexer(InputComponentStore componentStore, ProjectExclusionFilters exclusionFilters,
    SonarGlobalPropertiesFilter sonarGlobalPropertiesFilter, InputModuleHierarchy inputModuleHierarchy,
    GlobalConfiguration globalConfig, GlobalServerSettings globalServerSettings, ProjectServerSettings projectServerSettings,
    FileIndexer fileIndexer, ProjectCoverageAndDuplicationExclusions projectCoverageAndDuplicationExclusions, ScmConfiguration scmConfiguration,
    AnalysisWarnings analysisWarnings) {
    this.componentStore = componentStore;
    this.sonarGlobalPropertiesFilter = sonarGlobalPropertiesFilter;
    this.inputModuleHierarchy = inputModuleHierarchy;
    this.globalConfig = globalConfig;
    this.globalServerSettings = globalServerSettings;
    this.projectServerSettings = projectServerSettings;
    this.fileIndexer = fileIndexer;
    this.projectExclusionFilters = exclusionFilters;
    this.projectCoverageAndDuplicationExclusions = projectCoverageAndDuplicationExclusions;
    this.scmConfiguration = scmConfiguration;
    this.analysisWarnings = analysisWarnings;
    this.ignoreCommand = loadIgnoreCommand();
    this.useScmExclusion = ignoreCommand != null;
  }

  public void index() {
    progressReport = new ProgressReport("Report about progress of file indexation", TimeUnit.SECONDS.toMillis(10));
    progressReport.start("Indexing files...");
    LOG.info("Project configuration:");
    projectExclusionFilters.log("  ");
    projectCoverageAndDuplicationExclusions.log("  ");
    ExclusionCounter exclusionCounter = new ExclusionCounter();

    if (useScmExclusion) {
      ignoreCommand.init(inputModuleHierarchy.root().getBaseDir().toAbsolutePath());
      indexModulesRecursively(inputModuleHierarchy.root(), exclusionCounter);
      ignoreCommand.clean();
    } else {
      indexModulesRecursively(inputModuleHierarchy.root(), exclusionCounter);
    }

    int totalIndexed = componentStore.inputFiles().size();
    progressReport.stop(totalIndexed + " " + pluralizeFiles(totalIndexed) + " indexed");

    int excludedFileByPatternCount = exclusionCounter.getByPatternsCount();
    if (projectExclusionFilters.hasPattern() || excludedFileByPatternCount > 0) {
      LOG.info("{} {} ignored because of inclusion/exclusion patterns", excludedFileByPatternCount, pluralizeFiles(excludedFileByPatternCount));
    }
    int excludedFileByScmCount = exclusionCounter.getByScmCount();
    if (useScmExclusion) {
      LOG.info("{} {} ignored because of scm ignore settings", excludedFileByScmCount, pluralizeFiles(excludedFileByScmCount));
    }
  }

  private IgnoreCommand loadIgnoreCommand() {
    try {
      if (!scmConfiguration.isExclusionDisabled() && scmConfiguration.provider() != null) {
        return scmConfiguration.provider().ignoreCommand();
      }
    } catch (UnsupportedOperationException e) {
      LOG.debug("File exclusion based on SCM ignore information is not available with this plugin.");
    }

    return null;
  }

  private void indexModulesRecursively(DefaultInputModule module, ExclusionCounter exclusionCounter) {
    inputModuleHierarchy.children(module).stream().sorted(Comparator.comparing(DefaultInputModule::key)).forEach(m -> indexModulesRecursively(m, exclusionCounter));
    index(module, exclusionCounter);
  }

  private void index(DefaultInputModule module, ExclusionCounter exclusionCounter) {
    // Emulate creation of module level settings
    ModuleConfiguration moduleConfig = new ModuleConfigurationProvider(sonarGlobalPropertiesFilter).provide(globalConfig, module, globalServerSettings, projectServerSettings);
    ModuleExclusionFilters moduleExclusionFilters = new ModuleExclusionFilters(moduleConfig, analysisWarnings);
    ModuleCoverageAndDuplicationExclusions moduleCoverageAndDuplicationExclusions = new ModuleCoverageAndDuplicationExclusions(moduleConfig);
    if (componentStore.allModules().size() > 1) {
      LOG.info("Indexing files of module '{}'", module.getName());
      LOG.info("  Base dir: {}", module.getBaseDir().toAbsolutePath());
      module.getSourceDirsOrFiles().ifPresent(srcs -> logPaths("  Source paths: ", module.getBaseDir(), srcs));
      module.getTestDirsOrFiles().ifPresent(tests -> logPaths("  Test paths: ", module.getBaseDir(), tests));
      moduleExclusionFilters.log("  ");
      moduleCoverageAndDuplicationExclusions.log("  ");
    }
    boolean hasChildModules = !module.definition().getSubProjects().isEmpty();
    boolean hasTests = module.getTestDirsOrFiles().isPresent();
    // Default to index basedir when no sources provided
    List<Path> mainSourceDirsOrFiles = module.getSourceDirsOrFiles()
      .orElseGet(() -> hasChildModules || hasTests ? emptyList() : singletonList(module.getBaseDir().toAbsolutePath()));
    indexFiles(module, moduleExclusionFilters, moduleCoverageAndDuplicationExclusions, mainSourceDirsOrFiles, Type.MAIN, exclusionCounter);
    module.getTestDirsOrFiles().ifPresent(tests -> indexFiles(module, moduleExclusionFilters, moduleCoverageAndDuplicationExclusions, tests, Type.TEST, exclusionCounter));
  }

  private static void logPaths(String label, Path baseDir, List<Path> paths) {
    if (!paths.isEmpty()) {
      StringBuilder sb = new StringBuilder(label);
      for (Iterator<Path> it = paths.iterator(); it.hasNext(); ) {
        Path file = it.next();
        Optional<String> relativePathToBaseDir = PathResolver.relativize(baseDir, file);
        if (!relativePathToBaseDir.isPresent()) {
          sb.append(file);
        } else if (StringUtils.isBlank(relativePathToBaseDir.get())) {
          sb.append(".");
        } else {
          sb.append(relativePathToBaseDir.get());
        }
        if (it.hasNext()) {
          sb.append(", ");
        }
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug(sb.toString());
      } else {
        LOG.info(StringUtils.abbreviate(sb.toString(), 80));
      }
    }
  }

  private static String pluralizeFiles(int count) {
    return count == 1 ? "file" : "files";
  }

  private void indexFiles(DefaultInputModule module, ModuleExclusionFilters moduleExclusionFilters,
    ModuleCoverageAndDuplicationExclusions moduleCoverageAndDuplicationExclusions, List<Path> sources, Type type, ExclusionCounter exclusionCounter) {
    try {
      for (Path dirOrFile : sources) {
        if (dirOrFile.toFile().isDirectory()) {
          indexDirectory(module, moduleExclusionFilters, moduleCoverageAndDuplicationExclusions, dirOrFile, type, exclusionCounter);
        } else {
          fileIndexer.indexFile(module, moduleExclusionFilters, moduleCoverageAndDuplicationExclusions, dirOrFile, type, progressReport, exclusionCounter,
            ignoreCommand);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to index files", e);
    }
  }

  private void indexDirectory(DefaultInputModule module, ModuleExclusionFilters moduleExclusionFilters,
    ModuleCoverageAndDuplicationExclusions moduleCoverageAndDuplicationExclusions, Path dirToIndex, Type type, ExclusionCounter exclusionCounter)
    throws IOException {
    Files.walkFileTree(dirToIndex.normalize(), Collections.singleton(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
      new IndexFileVisitor(module, moduleExclusionFilters, moduleCoverageAndDuplicationExclusions, type, exclusionCounter));
  }


  /**
   * <p>Checks if the path is a directory that is excluded.</p>
   *
   * <p>Exclusions patterns are checked both at project and module level.</p>
   *
   * @param moduleExclusionFilters The exclusion filters.
   * @param realAbsoluteFile       The path to be checked.
   * @param projectBaseDir         The project base directory.
   * @param moduleBaseDir          The module base directory.
   * @param type                   The input file type.
   * @return True if path is an excluded directory, false otherwise.
   */
  private static boolean isExcludedDirectory(ModuleExclusionFilters moduleExclusionFilters, Path realAbsoluteFile, Path projectBaseDir, Path moduleBaseDir,
    InputFile.Type type) {
    Path projectRelativePath = projectBaseDir.relativize(realAbsoluteFile);
    Path moduleRelativePath = moduleBaseDir.relativize(realAbsoluteFile);
    return moduleExclusionFilters.isExcludedAsParentDirectoryOfExcludedChildren(realAbsoluteFile, projectRelativePath, projectBaseDir, type)
      || moduleExclusionFilters.isExcludedAsParentDirectoryOfExcludedChildren(realAbsoluteFile, moduleRelativePath, moduleBaseDir, type);
  }

  private class IndexFileVisitor implements FileVisitor<Path> {
    private final DefaultInputModule module;
    private final ModuleExclusionFilters moduleExclusionFilters;
    private final ModuleCoverageAndDuplicationExclusions moduleCoverageAndDuplicationExclusions;
    private final Type type;
    private final ExclusionCounter exclusionCounter;

    IndexFileVisitor(DefaultInputModule module, ModuleExclusionFilters moduleExclusionFilters, ModuleCoverageAndDuplicationExclusions moduleCoverageAndDuplicationExclusions,
      Type type,
      ExclusionCounter exclusionCounter) {
      this.module = module;
      this.moduleExclusionFilters = moduleExclusionFilters;
      this.moduleCoverageAndDuplicationExclusions = moduleCoverageAndDuplicationExclusions;
      this.type = type;
      this.exclusionCounter = exclusionCounter;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      if (isHidden(dir)) {
        return FileVisitResult.SKIP_SUBTREE;
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      if (!Files.isHidden(file)) {
        fileIndexer.indexFile(module, moduleExclusionFilters, moduleCoverageAndDuplicationExclusions, file, type, progressReport, exclusionCounter, ignoreCommand);
      }
      return FileVisitResult.CONTINUE;
    }

    /**
     * <p>Overridden method to handle exceptions while visiting files in the analysis.</p>
     *
     * <p>
     *   <ul>
     *     <li>FileSystemLoopException - We show a warning that a symlink loop exists and we skip the file.</li>
     *     <li>AccessDeniedException for excluded files/directories - We skip the file, as files excluded from the analysis, shouldn't throw access exceptions.</li>
     *   </ul>
     * </p>
     *
     * @param file a reference to the file
     * @param exc  the I/O exception that prevented the file from being visited
     * @throws IOException
     */
    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
      if (exc instanceof FileSystemLoopException) {
        LOG.warn("Not indexing due to symlink loop: {}", file.toFile());
        return FileVisitResult.CONTINUE;
      } else if (exc instanceof AccessDeniedException && isExcluded(file)) {
        return FileVisitResult.CONTINUE;
      }
      throw exc;
    }

    /**
     * <p>Checks if the directory is excluded in the analysis or not. Only the exclusions are checked.</p>
     *
     * <p>The inclusions cannot be checked for directories, since the current implementation of pattern matching is intended only for files.</p>
     *
     * @param path The file or directory.
     * @return True if file/directory is excluded from the analysis, false otherwise.
     */
    private boolean isExcluded(Path path) throws IOException {
      Path realAbsoluteFile = path.toRealPath(LinkOption.NOFOLLOW_LINKS).toAbsolutePath().normalize();
      return isExcludedDirectory(moduleExclusionFilters, realAbsoluteFile, inputModuleHierarchy.root().getBaseDir(), module.getBaseDir(), type);
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
      return FileVisitResult.CONTINUE;
    }

    private boolean isHidden(Path path) throws IOException {
      if (SystemUtils.IS_OS_WINDOWS) {
        try {
          DosFileAttributes dosFileAttributes = Files.readAttributes(path, DosFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
          return dosFileAttributes.isHidden();
        } catch (UnsupportedOperationException e) {
          return path.toFile().isHidden();
        }
      } else {
        return Files.isHidden(path);
      }
    }
  }

  static class ExclusionCounter {
    private final AtomicInteger excludedByPatternsCount = new AtomicInteger(0);
    private final AtomicInteger excludedByScmCount = new AtomicInteger(0);

    public void increaseByPatternsCount() {
      excludedByPatternsCount.incrementAndGet();
    }

    public int getByPatternsCount() {
      return excludedByPatternsCount.get();
    }

    public void increaseByScmCount() {
      excludedByScmCount.incrementAndGet();
    }

    public int getByScmCount() {
      return excludedByScmCount.get();
    }
  }
}
