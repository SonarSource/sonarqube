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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.maven.project.MavenProject;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.SonarException;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Adapter for keeping the backward-compatibility of the deprecated component {@link org.sonar.api.resources.ProjectFileSystem}
 *
 * @since 3.5
 */
public class ProjectFileSystemAdapter implements ProjectFileSystem {

  private final DefaultModuleFileSystem target;
  private final PathResolver pathResolver = new PathResolver();
  private final MavenProject pom;

  public ProjectFileSystemAdapter(DefaultModuleFileSystem target, Project project, @Nullable MavenProject pom) {
    this.target = target;
    this.pom = pom;

    // TODO See http://jira.codehaus.org/browse/SONAR-2126
    // previously MavenProjectBuilder was responsible for creation of ProjectFileSystem
    project.setFileSystem(this);
  }

  public ProjectFileSystemAdapter(DefaultModuleFileSystem target, Project project) {
    this(target, project, null);
  }

  public void start() {
    // used to avoid NPE in Project#getFileSystem()
  }

  public Charset getSourceCharset() {
    return target.sourceCharset();
  }

  public File getBasedir() {
    return target.baseDir();
  }

  public File getBuildDir() {
    File dir = target.buildDir();
    if (dir == null) {
      // emulate build dir to keep backward-compatibility
      dir = new File(getSonarWorkingDirectory(), "build");
    }
    return dir;
  }

  public File getBuildOutputDir() {
    File dir = Iterables.getFirst(target.binaryDirs(), null);
    if (dir == null) {
      // emulate binary dir
      dir = new File(getBuildDir(), "classes");
    }

    return dir;
  }

  public List<File> getSourceDirs() {
    return target.sourceDirs();
  }

  public ProjectFileSystem addSourceDir(File dir) {
    target.addSourceDir(dir);
    return this;
  }

  public List<File> getTestDirs() {
    return target.testDirs();
  }

  public ProjectFileSystem addTestDir(File dir) {
    target.addTestDir(dir);
    return this;
  }

  public File getReportOutputDir() {
    if (pom != null) {
      return resolvePath(pom.getReporting().getOutputDirectory());
    }
    // emulate Maven report output dir
    return new File(getBuildDir(), "site");
  }

  public File getSonarWorkingDirectory() {
    return target.workingDir();
  }

  public File resolvePath(String path) {
    File file = new File(path);
    if (!file.isAbsolute()) {
      try {
        file = new File(getBasedir(), path).getCanonicalFile();
      } catch (IOException e) {
        throw new SonarException("Unable to resolve path '" + path + "'", e);
      }
    }
    return file;
  }

  public List<File> getSourceFiles(Language... langs) {
    List<File> result = Lists.newArrayList();
    for (Language lang : langs) {
      result.addAll(target.files(FileQuery.onMain().onLanguage(lang.getKey())));
    }
    return result;
  }

  public List<File> getJavaSourceFiles() {
    return getSourceFiles(Java.INSTANCE);
  }

  public boolean hasJavaSourceFiles() {
    return !getJavaSourceFiles().isEmpty();
  }

  public List<File> getTestFiles(Language... langs) {
    List<File> result = Lists.newArrayList();
    for (Language lang : langs) {
      result.addAll(target.files(FileQuery.onTest().onLanguage(lang.getKey())));
    }
    return result;
  }

  public boolean hasTestFiles(Language lang) {
    return !getTestFiles(lang).isEmpty();
  }

  public File writeToWorkingDirectory(String content, String fileName) throws IOException {
    File file = new File(target.workingDir(), fileName);
    FileUtils.writeStringToFile(file, content, CharEncoding.UTF_8);
    return file;
  }

  public File getFileFromBuildDirectory(String filename) {
    File file = new File(getBuildDir(), filename);
    return file.exists() ? file : null;
  }

  public Resource toResource(File file) {
    if (file == null || !file.exists()) {
      return null;
    }
    PathResolver.RelativePath relativePath = pathResolver.relativePath(getSourceDirs(), file);
    if (relativePath == null) {
      return null;
    }
    return file.isFile() ? new org.sonar.api.resources.File(relativePath.path()) : new org.sonar.api.resources.Directory(relativePath.path());
  }

  public List<InputFile> mainFiles(String... langs) {
    List<InputFile> result = Lists.newArrayList();
    Iterable<org.sonar.api.scan.filesystem.InputFile> files = target.inputFiles(FileQuery.onMain().onLanguage(langs));
    for (org.sonar.api.scan.filesystem.InputFile file : files) {
      String sourceDir = file.attribute(org.sonar.api.scan.filesystem.internal.DefaultInputFile.ATTRIBUTE_SOURCEDIR_PATH);
      String sourceRelativePath = file.attribute(org.sonar.api.scan.filesystem.internal.DefaultInputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH);
      if (sourceDir != null && sourceRelativePath != null) {
        result.add(InputFileUtils.create(new File(sourceDir), sourceRelativePath));
      } else {
        result.add(InputFileUtils.create(target.baseDir(), file.path()));
      }
    }
    return result;
  }

  public List<InputFile> testFiles(String... langs) {
    List<InputFile> result = Lists.newArrayList();
    Iterable<org.sonar.api.scan.filesystem.InputFile> files = target.inputFiles(FileQuery.onTest().onLanguage(langs));
    for (org.sonar.api.scan.filesystem.InputFile file : files) {
      String sourceDir = file.attribute(org.sonar.api.scan.filesystem.internal.DefaultInputFile.ATTRIBUTE_SOURCEDIR_PATH);
      String sourceRelativePath = file.attribute(org.sonar.api.scan.filesystem.internal.DefaultInputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH);
      if (sourceDir != null && sourceRelativePath != null) {
        result.add(InputFileUtils.create(new File(sourceDir), sourceRelativePath));
      } else {
        result.add(InputFileUtils.create(target.baseDir(), file.path()));
      }
    }
    return result;
  }
}
