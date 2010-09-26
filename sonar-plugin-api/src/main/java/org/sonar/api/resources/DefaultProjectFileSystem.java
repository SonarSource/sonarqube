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
package org.sonar.api.resources;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.*;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.FileFilter;
import org.sonar.api.batch.maven.MavenUtils;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.WildcardPattern;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An implementation  of ProjectFileSystem
 *
 * @since 1.10
 */
public class DefaultProjectFileSystem implements ProjectFileSystem {

  private Project project;
  private List<IOFileFilter> filters = Lists.newArrayList();

  /**
   * Creates a DefaultProjectFileSystem based on a project
   *
   * @param project
   */
  public DefaultProjectFileSystem(Project project) {
    this.project = project;
  }

  /**
   * Source encoding. Never null, it returns the default plateform charset if it is not defined in project.
   */
  public Charset getSourceCharset() {
    return MavenUtils.getSourceCharset(project.getPom());
  }


  public DefaultProjectFileSystem addFileFilters(List<FileFilter> l) {
    for (FileFilter fileFilter : l) {
      addFileFilter(fileFilter);
    }
    return this;
  }

  public DefaultProjectFileSystem addFileFilter(FileFilter fileFilter) {
    filters.add(new DelegateFileFilter(fileFilter));
    return this;
  }

  /**
   * Basedir is the project root directory.
   */
  public File getBasedir() {
    return project.getPom().getBasedir();
  }

  /**
   * Build directory is by default "target" in maven projects.
   */
  public File getBuildDir() {
    return resolvePath(project.getPom().getBuild().getDirectory());
  }

  /**
   * Directory where classes are placed. By default "target/classes" in maven projects.
   */
  public File getBuildOutputDir() {
    return resolvePath(project.getPom().getBuild().getOutputDirectory());
  }

  /**
   * The list of directories for sources
   */
  public List<File> getSourceDirs() {
    return resolvePaths(project.getPom().getCompileSourceRoots());
  }

  /**
   * Adds a source directory
   *
   * @return the current object
   */
  public DefaultProjectFileSystem addSourceDir(File dir) {
    if (dir == null) {
      throw new IllegalArgumentException("Can not add null to project source dirs");
    }
    project.getPom().getCompileSourceRoots().add(0, dir.getAbsolutePath());
    return this;
  }

  /**
   * The list of directories for tests
   */
  public List<File> getTestDirs() {
    return resolvePaths(project.getPom().getTestCompileSourceRoots());
  }

  /**
   * Adds a test directory
   *
   * @return the current object
   */
  public DefaultProjectFileSystem addTestDir(File dir) {
    if (dir == null) {
      throw new IllegalArgumentException("Can not add null to project test dirs");
    }
    project.getPom().getTestCompileSourceRoots().add(0, dir.getAbsolutePath());
    return this;
  }

  /**
   * @return the directory where reporting is placed. Default is target/sites
   */
  public File getReportOutputDir() {
    return resolvePath(project.getPom().getReporting().getOutputDirectory());
  }

  /**
   * @return the Sonar working directory. Default is "target/sonar"
   */
  public File getSonarWorkingDirectory() {
    try {
      File dir = new File(project.getPom().getBuild().getDirectory(), "sonar");
      FileUtils.forceMkdir(dir);
      return dir;

    } catch (IOException e) {
      throw new SonarException("Unable to retrieve Sonar working directory.", e);
    }
  }

  public File resolvePath(String path) {
    File file = new File(path);
    if (!file.isAbsolute()) {
      file = new File(project.getPom().getBasedir(), path);
    }
    return file;
  }

  private List<File> resolvePaths(List<String> paths) {
    List<File> result = new ArrayList<File>();
    if (paths != null) {
      for (String path : paths) {
        result.add(resolvePath(path));
      }
    }

    return result;
  }

  /**
   * Gets the list of source files for given languages
   *
   * @param langs language filter. If null or empty, will return empty list
   */
  public List<File> getSourceFiles(Language... langs) {
    return getFiles(getSourceDirs(), true, langs);
  }

  /**
   * Gets the list of java source files
   */
  public List<File> getJavaSourceFiles() {
    return getSourceFiles(Java.INSTANCE);
  }

  /**
   * @return whether there are java source
   */
  public boolean hasJavaSourceFiles() {
    return !getJavaSourceFiles().isEmpty();
  }

  /**
   * Gets the list of test files for given languages
   *
   * @param langs language filter. If null or empty, will return empty list
   */
  public List<File> getTestFiles(Language... langs) {
    return getFiles(getTestDirs(), false, langs);
  }

  /**
   * @return whether there are tests files
   */
  public boolean hasTestFiles(Language lang) {
    return !getTestFiles(lang).isEmpty();
  }

  private List<File> getFiles(List<File> directories, boolean applyExclusionPatterns, Language... langs) {
    List<File> result = new ArrayList<File>();
    if (directories == null) {
      return result;
    }

    IOFileFilter suffixFilter = getFileSuffixFilter(langs);
    WildcardPattern[] exclusionPatterns = getExclusionPatterns(applyExclusionPatterns);

    for (File dir : directories) {
      if (dir.exists()) {
        IOFileFilter exclusionFilter = new ExclusionFilter(dir, exclusionPatterns);
        IOFileFilter visibleFileFilter = HiddenFileFilter.VISIBLE;
        List dirFilters = Lists.newArrayList(visibleFileFilter, suffixFilter, exclusionFilter);
        dirFilters.addAll(this.filters);
        result.addAll(FileUtils.listFiles(dir, new AndFileFilter(dirFilters), HiddenFileFilter.VISIBLE));
      }
    }
    return result;
  }

  private WildcardPattern[] getExclusionPatterns(boolean applyExclusionPatterns) {
    WildcardPattern[] exclusionPatterns;
    if (applyExclusionPatterns) {
      exclusionPatterns = WildcardPattern.create(project.getExclusionPatterns());
    } else {
      exclusionPatterns = new WildcardPattern[0];
    }
    return exclusionPatterns;
  }

  private IOFileFilter getFileSuffixFilter(Language... langs) {
    IOFileFilter suffixFilter = FileFilterUtils.trueFileFilter();
    if (langs != null && langs.length > 0) {
      List<String> suffixes = new ArrayList<String>();
      for (Language lang : langs) {
        if (lang.getFileSuffixes() != null) {
          suffixes.addAll(Arrays.asList(lang.getFileSuffixes()));
        }
      }
      if (!suffixes.isEmpty()) {
        suffixFilter = new SuffixFileFilter(suffixes);
      }
    }

    return suffixFilter;
  }

  private static class ExclusionFilter implements IOFileFilter {
    File sourceDir;
    WildcardPattern[] patterns;

    ExclusionFilter(File sourceDir, WildcardPattern[] patterns) {
      this.sourceDir = sourceDir;
      this.patterns = patterns;
    }

    public boolean accept(File file) {
      String relativePath = getRelativePath(file, sourceDir);
      if (relativePath == null) {
        return false;
      }
      for (WildcardPattern pattern : patterns) {
        if (pattern.match(relativePath)) {
          return false;
        }
      }
      return true;
    }

    public boolean accept(File file, String name) {
      return accept(file);
    }
  }

  /**
   * Save data into a new file of Sonar working directory.
   *
   * @return the created file
   */
  public File writeToWorkingDirectory(String content, String fileName) throws IOException {
    return writeToFile(content, getSonarWorkingDirectory(), fileName);
  }

  protected static File writeToFile(String content, File dir, String fileName) throws IOException {
    File file = new File(dir, fileName);
    FileUtils.writeStringToFile(file, content, CharEncoding.UTF_8);
    return file;
  }

  /**
   * getRelativePath("c:/foo/src/my/package/Hello.java", "c:/foo/src") is "my/package/Hello.java"
   *
   * @return null if file is not in dir (including recursive subdirectories)
   */
  public static String getRelativePath(File file, File dir) {
    return getRelativePath(file, Arrays.asList(dir));
  }

  /**
   * getRelativePath("c:/foo/src/my/package/Hello.java", ["c:/bar", "c:/foo/src"]) is "my/package/Hello.java".
   * <p/>
   * <p>Relative path is composed of slashes. Windows backslaches are replaced by /</p>
   *
   * @return null if file is not in dir (including recursive subdirectories)
   */
  public static String getRelativePath(File file, List<File> dirs) {
    List<String> stack = new ArrayList<String>();
    String path = FilenameUtils.normalize(file.getAbsolutePath());
    File cursor = new File(path);
    while (cursor != null) {
      if (containsFile(dirs, cursor)) {
        return StringUtils.join(stack, "/");
      }
      stack.add(0, cursor.getName());
      cursor = cursor.getParentFile();
    }
    return null;
  }

  public File getFileFromBuildDirectory(String filename) {
    File file = new File(getBuildDir(), filename);
    return (file.exists() ? file : null);
  }

  public Resource toResource(File file) {
    if (file == null || !file.exists()) {
      return null;
    }

    String relativePath = getRelativePath(file, getSourceDirs());
    if (relativePath == null) {
      return null;
    }

    return (file.isFile() ? new org.sonar.api.resources.File(relativePath) : new org.sonar.api.resources.Directory(relativePath));
  }

  private static boolean containsFile(List<File> dirs, File cursor) {
    for (File dir : dirs) {
      if (FilenameUtils.equalsNormalizedOnSystem(dir.getAbsolutePath(), cursor.getAbsolutePath())) {
        return true;
      }
    }
    return false;
  }
}
