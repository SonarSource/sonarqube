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
package org.sonar.api.scan.filesystem;

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
import java.util.Collections;
import java.util.List;

/**
 * @since 3.5
 */
public class SimpleModuleFileSystem implements ModuleFileSystem {
  private File baseDir;

  public SimpleModuleFileSystem(File baseDir) {
    this.baseDir = baseDir;
  }

  public File baseDir() {
    return baseDir;
  }

  public File buildDir() {
    return new File(baseDir, "build");
  }

  public List<File> sourceDirs() {
    return Arrays.asList(new File(baseDir, "src"));
  }

  public List<File> testDirs() {
    return Arrays.asList(new File(baseDir, "test"));
  }

  public List<File> binaryDirs() {
    return Arrays.asList(new File(baseDir, "binary"));
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
