/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.scan.filesystem;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.injectors.ProviderAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.FailToCreateFileException;
import org.sonar.api.scan.filesystem.FileFilter;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.batch.bootstrap.TempDirectories;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;

/**
 * @since 3.5
 */
public class ModuleFileSystemProvider extends ProviderAdapter {
  private static final Logger LOG = LoggerFactory.getLogger(ModuleFileSystemProvider.class);

  private ModuleFileSystem singleton;

  public ModuleFileSystem provide(ProjectDefinition module, PathResolver pathResolver, TempDirectories tempDirectories,
                                  LanguageFileFilters languageFileFilters, Settings settings, FileFilter[] pluginFileFilters) {
    if (singleton == null) {
      DefaultModuleFileSystem.Builder builder = new DefaultModuleFileSystem.Builder();

      // dependencies
      builder.pathResolver(pathResolver);
      builder.languageFileFilters(languageFileFilters);

      // files and directories
      // TODO should the basedir always exist ? If yes, then we check also check that it's a dir but not a file
      builder.baseDir(module.getBaseDir());
      builder.sourceCharset(guessCharset(settings));
      builder.workingDir(guessWorkingDir(module, tempDirectories));
      initBinaryDirs(module, pathResolver, builder);
      initSources(module, pathResolver, builder);
      initTests(module, pathResolver, builder);

      // file filters
      initPluginFilters(builder, pluginFileFilters);
      initSourceInclusions(builder, settings);
      initTestInclusions(builder, settings);

      singleton = builder.build();
    }
    return singleton;
  }

  private File guessWorkingDir(ProjectDefinition module, TempDirectories tempDirectories) {
    File workDir = module.getWorkDir();
    if (workDir == null) {
      workDir = tempDirectories.getDir("work");
      LOG.warn("Working dir is not set. Using: " + workDir.getAbsolutePath());
    } else {
      LOG.warn("Working dir: " + workDir.getAbsolutePath());
      try {
        FileUtils.forceMkdir(workDir);
      } catch (Exception e) {
        throw new FailToCreateFileException("Fail to create working dir: " + workDir.getAbsolutePath(), e);
      }
    }
    return workDir;
  }

  private Charset guessCharset(Settings settings) {
    final Charset charset;
    String encoding = settings.getString(CoreProperties.ENCODING_PROPERTY);
    if (StringUtils.isNotEmpty(encoding)) {
      charset = Charset.forName(StringUtils.trim(encoding));
      LOG.info("Source encoding: " + charset.displayName() + ", default locale: " + Locale.getDefault());
    } else {
      charset = Charset.defaultCharset();
      LOG.warn("Source encoding is platform dependent (" + charset.displayName() + "), default locale: " + Locale.getDefault());
    }
    return charset;
  }

  private void initSources(ProjectDefinition module, PathResolver pathResolver, DefaultModuleFileSystem.Builder builder) {
    for (String sourcePath : module.getSourceDirs()) {
      builder.addSourceDir(pathResolver.relativeFile(module.getBaseDir(), sourcePath));
    }
    List<File> sourceFiles = pathResolver.relativeFiles(module.getBaseDir(), module.getSourceFiles());
    if (!sourceFiles.isEmpty()) {
      builder.addFileFilter(new WhiteListFileFilter(FileFilter.FileType.SOURCE, ImmutableSet.copyOf(sourceFiles)));
    }
  }

  private void initTests(ProjectDefinition module, PathResolver pathResolver, DefaultModuleFileSystem.Builder builder) {
    for (String testPath : module.getTestDirs()) {
      builder.addTestDir(pathResolver.relativeFile(module.getBaseDir(), testPath));
    }
    List<File> testFiles = pathResolver.relativeFiles(module.getBaseDir(), module.getTestFiles());
    if (!testFiles.isEmpty()) {
      builder.addFileFilter(new WhiteListFileFilter(FileFilter.FileType.TEST, ImmutableSet.copyOf(testFiles)));
    }
  }

  private void initPluginFilters(DefaultModuleFileSystem.Builder builder, FileFilter[] pluginFileFilters) {
    for (FileFilter pluginFileFilter : pluginFileFilters) {
      builder.addFileFilter(pluginFileFilter);
    }
  }

  private void initSourceInclusions(DefaultModuleFileSystem.Builder builder, Settings settings) {
    initInclusions(builder, settings, FileFilter.FileType.SOURCE,
      CoreProperties.PROJECT_INCLUSIONS_PROPERTY, CoreProperties.GLOBAL_EXCLUSIONS_PROPERTY, CoreProperties.PROJECT_EXCLUSIONS_PROPERTY);
  }

  private void initTestInclusions(DefaultModuleFileSystem.Builder builder, Settings settings) {
    initInclusions(builder, settings, FileFilter.FileType.TEST,
      CoreProperties.PROJECT_TEST_INCLUSIONS_PROPERTY, CoreProperties.GLOBAL_TEST_EXCLUSIONS_PROPERTY, CoreProperties.PROJECT_TEST_EXCLUSIONS_PROPERTY);
  }

  private static void initInclusions(DefaultModuleFileSystem.Builder builder, Settings settings, FileFilter.FileType fileType,
                                     String inclusionsProperty, String globalExclusionsProperty, String exclusionsProperty) {
    String[] inclusions = settings.getStringArray(inclusionsProperty);
    for (String inclusion : inclusions) {
      InclusionFileFilter filter = InclusionFileFilter.create(fileType, inclusion);
      if (filter != null) {
        builder.addFileFilter(filter);
      }
    }
    String[] globalExclusions = settings.getStringArray(globalExclusionsProperty);
    for (String globalExclusion : globalExclusions) {
      builder.addFileFilter(new ExclusionFileFilter(fileType, globalExclusion));
    }
    String[] exclusions = settings.getStringArray(exclusionsProperty);
    for (String exclusion : exclusions) {
      builder.addFileFilter(new ExclusionFileFilter(fileType, exclusion));
    }
  }

  private void initBinaryDirs(ProjectDefinition module, PathResolver pathResolver, DefaultModuleFileSystem.Builder builder) {
    for (String path : module.getBinaries()) {
      builder.addBinaryDir(pathResolver.relativeFile(module.getBaseDir(), path));
    }
  }
}
