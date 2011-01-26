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
 * @TODO in fact this class should not be located in sonar-plugin-api
 */
public class DefaultProjectFileSystem implements ProjectFileSystem {

  private Project project;
  private Languages languages;
  private List<IOFileFilter> filters = Lists.newArrayList();

  public DefaultProjectFileSystem(Project project, Languages languages) {
    this.project = project;
    this.languages = languages;
  }

  public DefaultProjectFileSystem(Project project, Languages languages, FileFilter... fileFilters) {
    this(project, languages);
    for (FileFilter fileFilter : fileFilters) {
      filters.add(new DelegateFileFilter(fileFilter));
    }
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

  public File getBasedir() {
    return project.getPom().getBasedir();
  }

  public File getBuildDir() {
    return resolvePath(project.getPom().getBuild().getDirectory());
  }

  public File getBuildOutputDir() {
    return resolvePath(project.getPom().getBuild().getOutputDirectory());
  }

  /**
   * Maven can modify source directories during Sonar execution - see MavenPhaseExecutor.
   */
  public List<File> getSourceDirs() {
    return resolvePaths(project.getPom().getCompileSourceRoots());
  }

  /**
   * @deprecated since 2.6, because should be immutable
   */
  @Deprecated
  public DefaultProjectFileSystem addSourceDir(File dir) {
    if (dir == null) {
      throw new IllegalArgumentException("Can not add null to project source dirs");
    }
    project.getPom().getCompileSourceRoots().add(0, dir.getAbsolutePath());
    return this;
  }

  /**
   * Maven can modify test directories during Sonar execution - see MavenPhaseExecutor.
   */
  public List<File> getTestDirs() {
    return resolvePaths(project.getPom().getTestCompileSourceRoots());
  }

  /**
   * @deprecated since 2.6, because should be immutable
   */
  @Deprecated
  public DefaultProjectFileSystem addTestDir(File dir) {
    if (dir == null) {
      throw new IllegalArgumentException("Can not add null to project test dirs");
    }
    project.getPom().getTestCompileSourceRoots().add(0, dir.getAbsolutePath());
    return this;
  }

  public File getReportOutputDir() {
    return resolvePath(project.getPom().getReporting().getOutputDirectory());
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

  private List<File> resolvePaths(List<String> paths) {
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
    return toFiles(mainFiles(getLanguageKeys(langs)));
  }

  @Deprecated
  public List<File> getJavaSourceFiles() {
    return getSourceFiles(Java.INSTANCE);
  }

  public boolean hasJavaSourceFiles() {
    return !mainFiles(Java.KEY).isEmpty();
  }

  @Deprecated
  public List<File> getTestFiles(Language... langs) {
    return toFiles(testFiles(getLanguageKeys(langs)));
  }

  @Deprecated
  public boolean hasTestFiles(Language lang) {
    return !testFiles(lang.getKey()).isEmpty();
  }

  private List<InputFile> getFiles(List<File> directories, boolean applyExclusionPatterns, String... langs) {
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
          String relativePath = DefaultProjectFileSystem.getRelativePath(file, dir);
          result.add(new DefaultInputFile(dir, relativePath));
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

  private IOFileFilter getFileSuffixFilter(String... langKeys) {
    IOFileFilter suffixFilter = FileFilterUtils.trueFileFilter();
    if (langKeys != null && langKeys.length > 0) {
      List<String> suffixes = Arrays.asList(languages.getSuffixes(langKeys));
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

  /**
   * Conversion from Language to key. Allows to provide backward compatibility.
   */
  private String[] getLanguageKeys(Language[] langs) {
    String[] keys = new String[langs.length];
    for (int i = 0; i < langs.length; i++) {
      keys[i] = langs[i].getKey();
    }
    return keys;
  }

  /**
   * Conversion from InputFile to File. Allows to provide backward compatibility.
   */
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
  public List<InputFile> mainFiles(String... langs) {
    return getFiles(getSourceDirs(), true, langs);
  }

  /**
   * @since 2.6
   */
  public List<InputFile> testFiles(String... langs) {
    return getFiles(getTestDirs(), false /* FIXME should be true? */, langs);
  }

  private static final class DefaultInputFile implements InputFile {
    private File basedir;
    private String relativePath;

    DefaultInputFile(File basedir, String relativePath) {
      this.basedir = basedir;
      this.relativePath = relativePath;
    }

    public File getFileBaseDir() {
      return basedir;
    }

    public File getFile() {
      return new File(basedir, relativePath);
    }

    public String getRelativePath() {
      return relativePath;
    }
  }
}
