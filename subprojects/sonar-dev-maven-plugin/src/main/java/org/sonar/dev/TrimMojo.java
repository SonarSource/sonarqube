/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.dev;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;

/**
 * @goal trim
 */
public class TrimMojo extends AbstractMojo {

  /**
   * @parameter
   * @required
   */
  private File directory;

  /**
   * List of ant-style patterns. If
   * this is not specified, allfiles in the project source directories are included.
   *
   * @parameter
   */
  private String[] includes;

  /**
   * @parameter
   */
  private String[] excludes;


  /**
   * Specifies the encoding of the source files.
   *
   * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
   */
  private String sourceEncoding;

  public void execute() throws MojoExecutionException, MojoFailureException {
    if (shouldExecute()) {
      trimDirectory();
    }
  }

  private void trimDirectory() throws MojoExecutionException {
    File[] files = scanFiles();
    for (File file : files) {
      StringBuilder sb = new StringBuilder();
      try {
        LineIterator lines = FileUtils.lineIterator(file, sourceEncoding);
        while (lines.hasNext()) {
          String line = lines.nextLine();
          if (StringUtils.isNotBlank(line)) {
            sb.append(StringUtils.trim(line));
            sb.append(IOUtils.LINE_SEPARATOR);
          }
        }
        FileUtils.writeStringToFile(file, sb.toString(), sourceEncoding);

      } catch (IOException e) {
        throw new MojoExecutionException("Can not trim the file " + file, e);
      }
    }
    getLog().info("Trimmed files: " + files.length);
  }

  private boolean shouldExecute() {
    return directory != null && directory.exists();
  }

  /**
   * gets a list of all files in the source directory.
   *
   * @return the list of all files in the source directory;
   */
  private File[] scanFiles() {
    String[] defaultIncludes = {"**\\*"};
    DirectoryScanner ds = new DirectoryScanner();
    if (includes == null) {
      ds.setIncludes(defaultIncludes);
    } else {
      ds.setIncludes(includes);
    }
    ds.addDefaultExcludes(); // .svn, ...
    if (excludes != null) {
      ds.setExcludes(excludes);
    }
    ds.setBasedir(directory);
    getLog().info("Scanning directory " + directory);
    ds.scan();
    int maxFiles = ds.getIncludedFiles().length;
    File[] result = new File[maxFiles];
    for (int i = 0; i < maxFiles; i++) {
      result[i] = new File(directory, ds.getIncludedFiles()[i]);
    }
    return result;
  }

  public File getDirectory() {
    return directory;
  }

  public void setDirectory(File directory) {
    this.directory = directory;
  }

  public String[] getIncludes() {
    return includes;
  }

  public void setIncludes(String[] includes) {
    this.includes = includes;
  }

  public String[] getExcludes() {
    return excludes;
  }

  public void setExcludes(String[] excludes) {
    this.excludes = excludes;
  }

  public String getSourceEncoding() {
    return sourceEncoding;
  }

  public void setSourceEncoding(String sourceEncoding) {
    this.sourceEncoding = sourceEncoding;
  }
}
