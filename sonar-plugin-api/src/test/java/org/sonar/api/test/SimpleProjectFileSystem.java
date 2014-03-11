/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.api.test;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.NotImplementedException;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

public class SimpleProjectFileSystem implements ProjectFileSystem {

  private File basedir;

  public SimpleProjectFileSystem(File basedir) {
    this.basedir = basedir;
  }

  public Charset getSourceCharset() {
    return Charset.defaultCharset();
  }

  private File createDir(String path) {
    try {
      File dir = new File(basedir, path);
      FileUtils.forceMkdir(dir);
      return dir;

    } catch (IOException e) {
      throw new SonarException(e);
    }
  }

  public File getBasedir() {
    return basedir;
  }

  public File getBuildDir() {
    return createDir("target");
  }

  public File getBuildOutputDir() {
    return createDir("target/classes");
  }

  public List<File> getSourceDirs() {
    return Arrays.asList(createDir("src/main"));
  }

  public ProjectFileSystem addSourceDir(File dir) {
    throw new NotImplementedException();
  }

  public List<File> getTestDirs() {
    return Arrays.asList(createDir("src/test"));
  }

  public ProjectFileSystem addTestDir(File dir) {
    throw new NotImplementedException();
  }

  public File getReportOutputDir() {
    return createDir("target/site");
  }

  public File getSonarWorkingDirectory() {
    return createDir("target/sonar");
  }

  public File resolvePath(String path) {
    return null;
  }

  public List<File> getSourceFiles(Language... langs) {
    return null;
  }

  public List<File> getJavaSourceFiles() {
    return null;
  }

  public boolean hasJavaSourceFiles() {
    return false;
  }

  public List<File> getTestFiles(Language... langs) {
    return null;
  }

  public boolean hasTestFiles(Language lang) {
    return false;
  }

  public File writeToWorkingDirectory(String content, String filename) throws IOException {
    File file = new File(getSonarWorkingDirectory(), filename);
    FileUtils.writeStringToFile(file, content, CharEncoding.UTF_8);
    return file;
  }

  public File getFileFromBuildDirectory(String filename) {
    return null;
  }

  public Resource toResource(File file) {
    return null;
  }

  /**
   * @since 2.6
   */
  public List<InputFile> mainFiles(String... lang) {
    return null;
  }

  /**
   * @since 2.6
   */
  public List<InputFile> testFiles(String... lang) {
    return null;
  }

}
