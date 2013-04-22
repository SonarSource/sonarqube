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
package org.sonar.api.scan.filesystem;

import com.google.common.collect.Lists;
import org.apache.commons.lang.CharEncoding;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

/**
 * @since 3.5
 */
public class SimpleModuleFileSystem implements ModuleFileSystem {
  private File baseDir;
  private File buildDir;
  private List<File> sourceDirs = Lists.newArrayList();
  private List<File> testDirs = Lists.newArrayList();
  private List<File> binaryDirs = Lists.newArrayList();

  public SimpleModuleFileSystem(File baseDir) {
    this.baseDir = baseDir;
    this.buildDir = new File(baseDir, "build");
  }

  public File baseDir() {
    return baseDir;
  }

  public File buildDir() {
    return buildDir;
  }

  public List<File> sourceDirs() {
    return sourceDirs;
  }

  public List<File> testDirs() {
    return testDirs;
  }

  public List<File> binaryDirs() {
    return binaryDirs;
  }

  public SimpleModuleFileSystem addSourceDir(File d) {
    sourceDirs.add(d);
    return this;
  }

  public SimpleModuleFileSystem addTestDir(File d) {
    testDirs.add(d);
    return this;
  }

  public SimpleModuleFileSystem addBinaryDir(File d) {
    binaryDirs.add(d);
    return this;
  }

  public List<File> files(FileQuery query) {
    return Collections.emptyList();
  }

  public Charset sourceCharset() {
    return Charset.forName(CharEncoding.UTF_8);
  }

  public File workingDir() {
    return new File(baseDir, "work");
  }
}
