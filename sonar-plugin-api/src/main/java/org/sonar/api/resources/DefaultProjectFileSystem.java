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
import org.sonar.api.utils.Logs;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.WildcardPattern;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An implementation of {@link ProjectFileSystem}.
 * For internal use only.
 * 
 * @since 1.10
 * @TODO inject into container
 */
public class DefaultProjectFileSystem implements ProjectFileSystem {

  private Project project;
  private List<IOFileFilter> filters = Lists.newArrayList();

  private File basedir;
  private File buildDir;
  private List<File> sourceDirs = Lists.newArrayList();
  private List<File> testDirs = Lists.newArrayList();

  public DefaultProjectFileSystem(Project project) {
    this.project = project;
  }

  public Charset getSourceCharset() {
    // TODO was return MavenUtils.getSourceCharset(project.getPom());
    String encoding = project.getConfiguration().getString("project.build.sourceEncoding");
    if (StringUtils.isNotEmpty(encoding)) {
      try {
        return Charset.forName(encoding);
      } catch (Exception e) {
        Logs.INFO.warn("Can not get project charset", e);
      }
    }
    return Charset.defaultCharset();
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

  public File getBasedir() {
    // TODO was return project.getPom().getBasedir();
    return basedir;
  }

  public File getBuildDir() {
    // TODO was return resolvePath(project.getPom().getBuild().getDirectory());
    return buildDir;
  }

  public File getBuildOutputDir() {
    // TODO was return resolvePath(project.getPom().getBuild().getOutputDirectory());
    return resolvePath(project.getConfiguration().getString("project.build.outputDirectory"));
  }

  public List<File> getSourceDirs() {
    return sourceDirs;
  }

  public DefaultProjectFileSystem addSourceDir(File dir) {
    if (dir == null) {
      throw new IllegalArgumentException("Can not add null to project source dirs");
    }
    sourceDirs.add(dir);
    return this;
  }

  public List<File> getTestDirs() {
    return testDirs;
  }

  public DefaultProjectFileSystem addTestDir(File dir) {
    if (dir == null) {
      throw new IllegalArgumentException("Can not add null to project test dirs");
    }
    testDirs.add(dir);
    return this;
  }

  public File getReportOutputDir() {
    // TODO was return resolvePath(project.getPom().getReporting().getOutputDirectory());
    return resolvePath(project.getConfiguration().getString("project.reporting.outputDirectory"));
  }

  public File getSonarWorkingDirectory() {
    try {
      File dir = new File(getBuildDir(), "sonar");
      FileUtils.forceMkdir(dir);
      return dir;

    } catch (IOException e) {
      throw new SonarException("Unable to retrieve Sonar working directory.", e);
    }
  }

  public File resolvePath(String path) {
    File file = new File(path);
    if (!file.isAbsolute()) {
      file = new File(getBasedir(), path);
    }
    return file;
  }

  // TODO was private
  public List<File> resolvePaths(List<String> paths) {
    List<File> result = Lists.newArrayList();
    if (paths != null) {
      for (String path : paths) {
        result.add(resolvePath(path));
      }
    }
    return result;
  }

  @Deprecated
  public List<File> getSourceFiles(Language... langs) {
    return toFiles(mainFiles(langs));
  }

  @Deprecated
  public List<File> getJavaSourceFiles() {
    return getSourceFiles(Java.INSTANCE);
  }

  public boolean hasJavaSourceFiles() {
    return !mainFiles(Java.INSTANCE).isEmpty();
  }

  @Deprecated
  public List<File> getTestFiles(Language... langs) {
    return toFiles(testFiles(langs));
  }

  public boolean hasTestFiles(Language lang) {
    return !testFiles(lang).isEmpty();
  }

  private List<InputFile> getFiles(List<File> directories, boolean applyExclusionPatterns, Language... langs) {
    List<InputFile> result = Lists.newArrayList();
    if (directories == null) {
      return result;
    }

    IOFileFilter suffixFilter = getFileSuffixFilter(langs);
    WildcardPattern[] exclusionPatterns = getExclusionPatterns(applyExclusionPatterns);

    for (File dir : directories) {
      if (dir.exists()) {
        IOFileFilter exclusionFilter = new ExclusionFilter(dir, exclusionPatterns);
        IOFileFilter visibleFileFilter = HiddenFileFilter.VISIBLE;
        List<IOFileFilter> dirFilters = Lists.newArrayList(visibleFileFilter, suffixFilter, exclusionFilter);
        dirFilters.addAll(this.filters);
        List<File> files = (List<File>) FileUtils.listFiles(dir, new AndFileFilter(dirFilters), HiddenFileFilter.VISIBLE);
        for (File file : files) {
          result.add(new DefaultInputFile(dir, file));
        }
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
   * <p>
   * Relative path is composed of slashes. Windows backslaches are replaced by /
   * </p>
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

  private static List<File> toFiles(List<InputFile> files) {
    List<File> result = Lists.newArrayList();
    for (InputFile file : files) {
      result.add(file.getFile());
    }
    return result;
  }

  /**
   * @since 2.6
   */
  public List<InputFile> mainFiles(Language... langs) {
    return getFiles(getSourceDirs(), true, langs);
  }

  /**
   * @since 2.6
   */
  public List<InputFile> testFiles(Language... langs) {
    return getFiles(getTestDirs(), false /* FIXME should be true? */, langs);
  }

  private class DefaultInputFile implements InputFile {
    private File basedir;
    private File file;

    public DefaultInputFile(File basedir, File file) {
      this.basedir = basedir;
      this.file = file;
    }

    public File getBaseDir() {
      return basedir;
    }

    public File getFile() {
      return file;
    }
  }

  /**
   * @since 2.6
   */
  public void setBaseDir(File basedir) {
    this.basedir = basedir;
  }

  /**
   * @since 2.6
   */
  public void setBuildDir(String path) {
    this.buildDir = path == null ? resolvePath("target") : resolvePath(path);
  }
}
