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
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.scan.filesystem.internal.DefaultInputFile;
import org.sonar.api.scan.filesystem.internal.InputFile;
import org.sonar.api.scan.filesystem.internal.InputFileFilter;
import org.sonar.api.utils.PathUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Index input files into {@link InputFileCache}.
 */
public class FileIndex implements BatchComponent {

  private static class Progress {
    private int count = 0;
    private final Set<String> removedPaths;

    Progress(Set<String> removedPaths) {
      this.removedPaths = removedPaths;
    }

    void markAsIndexed(String relativePath) {
      count++;
      removedPaths.remove(relativePath);
    }
  }

  private static final IOFileFilter DIR_FILTER = FileFilterUtils.and(HiddenFileFilter.VISIBLE, FileFilterUtils.notFileFilter(FileFilterUtils.prefixFileFilter(".")));
  private static final IOFileFilter FILE_FILTER = HiddenFileFilter.VISIBLE;

  private final PathResolver pathResolver;
  private final List<InputFileFilter> filters;
  private final LanguageRecognizer languageRecognizer;
  private final InputFileCache cache;
  private final FileHashes fileHashes;
  private final Project project;

  public FileIndex(List<InputFileFilter> filters, LanguageRecognizer languageRecognizer,
    InputFileCache cache, FileHashes fileHashes, PathResolver pathResolver, Project project) {
    this.filters = filters;
    this.languageRecognizer = languageRecognizer;
    this.cache = cache;
    this.fileHashes = fileHashes;
    this.pathResolver = pathResolver;
    this.project = project;
  }

  void index(DefaultModuleFileSystem fileSystem) {
    Logger logger = LoggerFactory.getLogger(FileIndex.class);
    logger.info("Index files");
    // TODO log configuration too (replace FileSystemLogger)

    Progress progress = new Progress(cache.fileRelativePaths(fileSystem.moduleKey()));

    if (fileSystem.sourceFiles().isEmpty()) {
      // index directories
      for (File sourceDir : fileSystem.sourceDirs()) {
        indexDirectory(fileSystem, progress, sourceDir, InputFile.TYPE_SOURCE);
      }
    } else {
      // index only given files
      indexFiles(fileSystem, progress, fileSystem.sourceDirs(), fileSystem.sourceFiles(), InputFile.TYPE_SOURCE);
    }

    if (fileSystem.testFiles().isEmpty()) {
      // index directories
      for (File testDir : fileSystem.testDirs()) {
        indexDirectory(fileSystem, progress, testDir, InputFile.TYPE_TEST);
      }
    } else {
      // index only given files
      indexFiles(fileSystem, progress, fileSystem.testDirs(), fileSystem.testFiles(), InputFile.TYPE_TEST);
    }

    // Remove files that have been removed since previous indexation
    for (String path : progress.removedPaths) {
      cache.remove(fileSystem.moduleKey(), path);
    }

    logger.info(String.format("%d files indexed", progress.count));
  }

  private void indexFiles(DefaultModuleFileSystem fileSystem, Progress progress, List<File> sourceDirs, List<File> sourceFiles, String type) {
    for (File sourceFile : sourceFiles) {
      PathResolver.RelativePath sourceDirPath = pathResolver.relativePath(sourceDirs, sourceFile);
      if (sourceDirPath == null) {
        LoggerFactory.getLogger(getClass()).warn(String.format(
          "File '%s' is not declared in source directories %s", sourceFile.getAbsoluteFile(), StringUtils.join(sourceDirs, ", ")
          ));
      } else {
        indexFile(fileSystem, progress, sourceDirPath.dir(), sourceFile, type);
      }
    }
  }

  Iterable<InputFile> inputFiles(String moduleKey) {
    return cache.byModule(moduleKey);
  }

  private void indexDirectory(DefaultModuleFileSystem fileSystem, Progress status, File sourceDir, String type) {
    Collection<File> files = FileUtils.listFiles(sourceDir, FILE_FILTER, DIR_FILTER);
    for (File file : files) {
      indexFile(fileSystem, status, sourceDir, file, type);
    }
  }

  private void indexFile(DefaultModuleFileSystem fileSystem, Progress status, File sourceDir, File file, String type) {
    String path = pathResolver.relativePath(fileSystem.baseDir(), file);
    if (path == null) {
      LoggerFactory.getLogger(getClass()).warn(String.format("File '%s' is not in basedir '%s'", file.getAbsolutePath(), fileSystem.baseDir()));
    } else {
      InputFile input = newInputFile(fileSystem, sourceDir, type, file, path);
      if (input != null && accept(input)) {
        cache.put(fileSystem.moduleKey(), input);
        status.markAsIndexed(path);
      }
    }
  }

  @CheckForNull
  private InputFile newInputFile(ModuleFileSystem fileSystem, File sourceDir, String type, File file, String path) {
    String lang = languageRecognizer.of(file);
    if (lang == null) {
      return null;
    }

    Map<String, String> attributes = Maps.newHashMap();
    set(attributes, InputFile.ATTRIBUTE_TYPE, type);
    set(attributes, InputFile.ATTRIBUTE_LANGUAGE, lang);

    // paths
    set(attributes, InputFile.ATTRIBUTE_SOURCEDIR_PATH, PathUtils.canonicalPath(sourceDir));
    String sourceRelativePath = pathResolver.relativePath(sourceDir, file);
    set(attributes, InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH, sourceRelativePath);

    String resourceKey = PathUtils.sanitize(path);
    if (!StringUtils.startsWith(resourceKey, "/")) {
      resourceKey = "/" + resourceKey;
    }
    set(attributes, DefaultInputFile.ATTRIBUTE_COMPONENT_KEY, project.getEffectiveKey() + ":" + resourceKey);
    if (Java.KEY.equals(lang)) {
      set(attributes, DefaultInputFile.ATTRIBUTE_COMPONENT_DEPRECATED_KEY, project.getEffectiveKey() + ":" + JavaFile.fromRelativePath(sourceRelativePath, false).getKey());
    } else {
      set(attributes, DefaultInputFile.ATTRIBUTE_COMPONENT_DEPRECATED_KEY, project.getEffectiveKey() + ":" + sourceRelativePath);
    }
    // hash + status
    initStatus(file, fileSystem.sourceCharset(), path, attributes);

    return DefaultInputFile.create(file, fileSystem.sourceCharset(), path, attributes);
  }

  private void initStatus(File file, Charset charset, String baseRelativePath, Map<String, String> attributes) {
    String hash = fileHashes.hash(file, charset);
    set(attributes, InputFile.ATTRIBUTE_HASH, hash);

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

  private boolean accept(InputFile inputFile) {
    for (InputFileFilter filter : filters) {
      if (!filter.accept(inputFile)) {
        return false;
      }
    }
    return true;
  }
}
