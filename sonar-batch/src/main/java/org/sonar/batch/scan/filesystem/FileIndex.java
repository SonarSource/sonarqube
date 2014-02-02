/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.scan.filesystem;

import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.InputDir;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.scan.filesystem.internal.DefaultInputDir;
import org.sonar.api.scan.filesystem.internal.DefaultInputFile;
import org.sonar.api.scan.filesystem.internal.InputFileFilter;
import org.sonar.api.utils.PathUtils;
import org.sonar.api.utils.SonarException;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Index input files into {@link InputFileCache}.
 */
public class FileIndex implements BatchComponent {

  private static class Progress {
    private final Set<String> removedPaths;
    private final Set<String> indexed;

    Progress(Set<String> removedPaths) {
      this.removedPaths = removedPaths;
      this.indexed = new HashSet<String>();
    }

    void markAsIndexed(String relativePath) {
      if (indexed.contains(relativePath)) {
        throw new SonarException("File " + relativePath + " can't be indexed twice. Please check that inclusion/exclusion patterns produce "
          + "disjoint sets for main and test files");
      }
      removedPaths.remove(relativePath);
      indexed.add(relativePath);
    }

    int count() {
      return indexed.size();
    }
  }

  private static final IOFileFilter DIR_FILTER = FileFilterUtils.and(HiddenFileFilter.VISIBLE, FileFilterUtils.notFileFilter(FileFilterUtils.prefixFileFilter(".")));
  private static final IOFileFilter FILE_FILTER = HiddenFileFilter.VISIBLE;

  private final PathResolver pathResolver;
  private final List<InputFileFilter> filters;
  private final LanguageRecognizer languageRecognizer;
  private final InputFileCache fileCache;
  private final FileHashes fileHashes;
  private final Project module;
  private final ExclusionFilters exclusionFilters;

  public FileIndex(List<InputFileFilter> filters, ExclusionFilters exclusionFilters, LanguageRecognizer languageRecognizer,
    InputFileCache cache, FileHashes fileHashes, PathResolver pathResolver, Project project) {
    this.filters = filters;
    this.exclusionFilters = exclusionFilters;
    this.languageRecognizer = languageRecognizer;
    this.fileCache = cache;
    this.fileHashes = fileHashes;
    this.pathResolver = pathResolver;
    this.module = project;
  }

  void index(DefaultModuleFileSystem fileSystem) {
    Logger logger = LoggerFactory.getLogger(FileIndex.class);
    if (!module.getModules().isEmpty()) {
      // No indexing for an aggregator module
      return;
    }
    logger.info("Index files");
    exclusionFilters.logConfiguration(fileSystem);
    // TODO log configuration too (replace FileSystemLogger)

    Progress progress = new Progress(fileCache.fileRelativePaths(fileSystem.moduleKey()));

    if (!fileSystem.sourceFiles().isEmpty() || !fileSystem.testFiles().isEmpty()) {
      // Index only provided files
      indexFiles(fileSystem, progress, fileSystem.sourceFiles(), InputFile.TYPE_MAIN);
      indexFiles(fileSystem, progress, fileSystem.testFiles(), InputFile.TYPE_TEST);
    } else {
      // index from basedir
      indexDirectory(fileSystem, progress, fileSystem.baseDir(), InputFile.TYPE_MAIN);
      indexDirectory(fileSystem, progress, fileSystem.baseDir(), InputFile.TYPE_TEST);
    }

    // Remove files that have been removed since previous indexation
    for (String path : progress.removedPaths) {
      fileCache.remove(fileSystem.moduleKey(), path);
    }

    logger.info(String.format("%d files indexed", progress.count()));

  }

  private void indexFiles(DefaultModuleFileSystem fileSystem, Progress progress, List<File> sourceFiles, String type) {
    for (File sourceFile : sourceFiles) {
      String path = pathResolver.relativePath(fileSystem.baseDir(), sourceFile);
      if (path == null) {
        LoggerFactory.getLogger(getClass()).warn(String.format(
          "File '%s' is not declared in module basedir %s", sourceFile.getAbsoluteFile(), fileSystem.baseDir()
          ));
      } else {
        indexFile(fileSystem, progress, sourceFile, type);
      }
    }
  }

  Iterable<InputFile> inputFiles(String moduleKey) {
    return fileCache.byModule(moduleKey);
  }

  InputFile inputFile(DefaultModuleFileSystem fileSystem, File ioFile) {
    String path = computeFilePath(fileSystem, ioFile);
    return fileCache.byPath(fileSystem.moduleKey(), path);
  }

  InputDir inputDir(DefaultModuleFileSystem fileSystem, File ioFile) {
    String path = computeFilePath(fileSystem, ioFile);
    // TODO no cache for InputDir
    Map<String, String> attributes = Maps.newHashMap();
    // paths
    String resourceKey = PathUtils.sanitize(path);
    set(attributes, DefaultInputFile.ATTRIBUTE_COMPONENT_KEY, module.getEffectiveKey() + ":" + resourceKey);
    return DefaultInputDir.create(ioFile, path, attributes);
  }

  private void indexDirectory(DefaultModuleFileSystem fileSystem, Progress status, File dirToIndex, String type) {
    Collection<File> files = FileUtils.listFiles(dirToIndex, FILE_FILTER, DIR_FILTER);
    for (File file : files) {
      indexFile(fileSystem, status, file, type);
    }
  }

  private void indexFile(DefaultModuleFileSystem fileSystem, Progress status, File file, String type) {
    String path = computeFilePath(fileSystem, file);
    if (path == null) {
      LoggerFactory.getLogger(getClass()).warn(String.format("File '%s' is not in basedir '%s'", file.getAbsolutePath(), fileSystem.baseDir()));
    } else {
      InputFile input = newInputFile(fileSystem, type, file, path);
      if (input != null && accept(input, fileSystem)) {
        fileCache.put(fileSystem.moduleKey(), input);
        status.markAsIndexed(path);
      }
    }
  }

  private String computeFilePath(DefaultModuleFileSystem fileSystem, File file) {
    return pathResolver.relativePath(fileSystem.baseDir(), file);
  }

  @CheckForNull
  private InputFile newInputFile(ModuleFileSystem fileSystem, String type, File file, String path) {

    Map<String, String> attributes = Maps.newHashMap();
    set(attributes, InputFile.ATTRIBUTE_TYPE, type);

    String resourceKey = PathUtils.sanitize(path);
    set(attributes, DefaultInputFile.ATTRIBUTE_COMPONENT_KEY, module.getEffectiveKey() + ":" + resourceKey);
    // hash + status
    initStatus(file, fileSystem.sourceCharset(), path, attributes);

    DefaultInputFile inputFile = DefaultInputFile.create(file, fileSystem.sourceCharset(), path, attributes);
    String lang = languageRecognizer.of(inputFile);
    if (lang == null) {
      return null;
    }
    set(inputFile.attributes(), InputFile.ATTRIBUTE_LANGUAGE, lang);

    setDeprecatedAttributes(fileSystem, type, file, attributes, inputFile, lang);

    return inputFile;
  }

  private void setDeprecatedAttributes(ModuleFileSystem fileSystem, String type, File file, Map<String, String> attributes, DefaultInputFile inputFile, String lang) {
    List<File> sourceDirs = InputFile.TYPE_MAIN.equals(type) ? fileSystem.sourceDirs() : fileSystem.testDirs();
    for (File src : sourceDirs) {
      String sourceRelativePath = pathResolver.relativePath(src, file);
      if (sourceRelativePath != null) {
        set(attributes, DefaultInputFile.ATTRIBUTE_SOURCEDIR_PATH, PathUtils.canonicalPath(src));
        set(attributes, DefaultInputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH, sourceRelativePath);
        if (Java.KEY.equals(lang)) {
          set(inputFile.attributes(), DefaultInputFile.ATTRIBUTE_COMPONENT_DEPRECATED_KEY, module.getEffectiveKey() + ":"
            + JavaFile.fromRelativePath(sourceRelativePath, false).getDeprecatedKey());
        } else {
          set(inputFile.attributes(), DefaultInputFile.ATTRIBUTE_COMPONENT_DEPRECATED_KEY, module.getEffectiveKey() + ":" + sourceRelativePath);
        }
        return;
      }
    }
  }

  private void initStatus(File file, Charset charset, String baseRelativePath, Map<String, String> attributes) {
    String hash = fileHashes.hash(file, charset);
    set(attributes, DefaultInputFile.ATTRIBUTE_HASH, hash);

    String remoteHash = fileHashes.remoteHash(baseRelativePath);
    // currently no need to store this remote hash in attributes
    if (StringUtils.equals(hash, remoteHash)) {
      set(attributes, InputFile.ATTRIBUTE_STATUS, InputFile.STATUS_SAME);
    } else if (StringUtils.isEmpty(remoteHash)) {
      set(attributes, InputFile.ATTRIBUTE_STATUS, InputFile.STATUS_ADDED);
    } else {
      set(attributes, InputFile.ATTRIBUTE_STATUS, InputFile.STATUS_CHANGED);
    }
  }

  private void set(Map<String, String> attributes, String key, @Nullable String value) {
    if (value != null) {
      attributes.put(key, value);
    }
  }

  private boolean accept(InputFile inputFile, ModuleFileSystem fs) {
    if (!exclusionFilters.accept(inputFile, fs)) {
      return false;
    }
    // Other InputFileFilter extensions
    for (InputFileFilter filter : filters) {
      if (!filter.accept(inputFile)) {
        return false;
      }
    }
    return true;
  }
}
