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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharEncoding;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.ModuleFileSystem;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

public class DeprecatedFileSystemAdapter implements ProjectFileSystem {

  private final ModuleFileSystem target;
  private final PathResolver pathResolver;

  public DeprecatedFileSystemAdapter(ModuleFileSystem target, PathResolver pathResolver) {
    this.target = target;
    this.pathResolver = pathResolver;
  }

  public Charset getSourceCharset() {
    return target.sourceCharset();
  }

  public File getBasedir() {
    return target.baseDir();
  }

  public File getBuildDir() {
    // TODO
    return null;
  }

  public File getBuildOutputDir() {
    return Iterables.getFirst(target.binaryDirs(), null);
  }

  public List<File> getSourceDirs() {
    return target.sourceDirs();
  }

  public ProjectFileSystem addSourceDir(File dir) {
    throw new UnsupportedOperationException("File system is immutable");
  }

  public List<File> getTestDirs() {
    return target.testDirs();
  }

  public ProjectFileSystem addTestDir(File dir) {
    throw new UnsupportedOperationException("File system is immutable");
  }

  public File getReportOutputDir() {
    // TODO
    return null;
  }

  public File getSonarWorkingDirectory() {
    return target.workingDir();
  }

  public File resolvePath(String path) {
    // TODO
    return null;
  }

  public List<File> getSourceFiles(Language... langs) {
    List<File> result = Lists.newArrayList();
    for (Language lang : langs) {
      result.addAll(target.sourceFilesOfLang(lang.getKey()));
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
      result.addAll(target.testFilesOfLang(lang.getKey()));
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
    return (file.exists() ? file : null);
  }

  public Resource toResource(File file) {
    // TODO
    return null;
  }

  public List<InputFile> mainFiles(String... langs) {
    // TODO
    return null;
  }

  public List<InputFile> testFiles(String... langs) {
    // TODO
    return null;
  }
}
