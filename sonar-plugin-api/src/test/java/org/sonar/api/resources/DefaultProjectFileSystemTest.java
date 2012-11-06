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
package org.sonar.api.resources;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.FileFilter;
import org.sonar.api.test.MavenTestUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultProjectFileSystemTest {
  Project project;

  @Before
  public void before() {
    project = MavenTestUtils.loadProjectFromPom(DefaultProjectFileSystemTest.class, "sample/pom.xml");
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2266
   */
  @Test
  public void shouldReturnOnlyExistingSourceAndTestDirectories() {
    // in this example : "src/main/java" is a file, "src/test/java" doesn't exist
    project = MavenTestUtils.loadProjectFromPom(DefaultProjectFileSystemTest.class, "nonexistent-dirs/pom.xml");

    ProjectFileSystem fs = project.getFileSystem();

    assertThat(fs.getSourceDirs()).isEmpty();
    assertThat(fs.getTestDirs()).isEmpty();
  }

  @Test
  public void getJavaSourceFiles() {
    ProjectFileSystem fs = project.getFileSystem();

    assertThat(fs.getJavaSourceFiles()).onProperty("name").containsOnly("Whizz.java", "Bar.java");
  }

  @Test
  public void hasJavaSourceFiles() {
    ProjectFileSystem fs = project.getFileSystem();
    assertThat(fs.hasJavaSourceFiles()).isTrue();

    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "**/*.java");
    project.setConfiguration(conf);

    assertThat(fs.hasJavaSourceFiles()).isFalse();
  }

  @Test
  public void getTestFiles() {
    ProjectFileSystem fs = project.getFileSystem();

    assertThat(fs.getTestFiles(Java.INSTANCE)).onProperty("name").containsOnly("BarTest.java");
  }

  @Test
  public void applyExclusionPatternsToSourceFiles() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "**/B*.java");
    project.setConfiguration(conf);

    ProjectFileSystem fs = project.getFileSystem();

    assertThat(fs.getJavaSourceFiles()).onProperty("name").containsOnly("Whizz.java");
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-1449
   */
  @Test
  public void exclusionPatternOnAjFiles() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "**/*.aj");
    project.setConfiguration(conf);

    ProjectFileSystem fs = project.getFileSystem();

    assertThat(fs.getSourceFiles(Java.INSTANCE)).onProperty("name").containsOnly("Whizz.java", "Bar.java");
  }

  @Test
  public void should_apply_inclusions() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "!**/W*.*");
    project.setConfiguration(conf);

    ProjectFileSystem fs = project.getFileSystem();

    assertThat(fs.getSourceFiles(Java.INSTANCE)).onProperty("name").containsOnly("Whizz.java");
  }

  @Test
  public void doNotApplyExclusionPatternsToTestFiles() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "**/B*.java");
    project.setConfiguration(conf);

    ProjectFileSystem fs = project.getFileSystem();

    assertThat(fs.getTestFiles(Java.INSTANCE)).onProperty("name").containsOnly("BarTest.java");
  }

  @Test
  public void createSonarWorkingDirectory() {
    ProjectFileSystem fs = project.getFileSystem();
    java.io.File dir = fs.getSonarWorkingDirectory();

    assertThat(dir).exists();
    assertThat(dir.listFiles()).isEmpty();
  }

  @Test
  public void getJapaneseCharSet() {
    project = MavenTestUtils.loadProjectFromPom(DefaultProjectFileSystemTest.class, "japanese-project/pom.xml");
    ProjectFileSystem fs = project.getFileSystem();

    assertThat(fs.getSourceCharset().name()).isEqualTo("Shift_JIS");
  }

  @Test
  public void languageWithNoSpecificFileSuffixes() {
    class NoSuffixLanguage implements Language {
      public String getKey() {
        return "no-suffix";
      }

      public String getName() {
        return "no-suffix";
      }

      public String[] getFileSuffixes() {
        return new String[0];
      }
    }

    project = MavenTestUtils.loadProjectFromPom(DefaultProjectFileSystemTest.class, "sample-with-different-suffixes/pom.xml");
    ProjectFileSystem fs = project.getFileSystem();
    List<File> files = fs.getSourceFiles(new NoSuffixLanguage());

    assertThat(files).hasSize(2);
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2280
   */
  @Test
  public void resolvePathShouldReturnCanonicalFile() {
    MavenProject pom = mock(MavenProject.class);
    when(pom.getBasedir()).thenReturn(new File("/project"));
    Project project = new Project("foo").setPom(pom);
    ProjectFileSystem fs = new DefaultProjectFileSystem(project, null);

    assertThat(fs.resolvePath(".").getAbsolutePath()).endsWith("project");
    assertThat(fs.resolvePath("../project").getAbsolutePath()).endsWith("project");
  }

  /**
   * Example of hidden files/directories : .DSStore, .svn, .git
   */
  @Test
  public void hiddenFilesAreIgnored() {
    if (!SystemUtils.IS_OS_WINDOWS) {
      // hidden files/directories can not be stored in svn windows
      // On Mac/Linux it's easy, just prefix the filename by '.'
      project = MavenTestUtils.loadProjectFromPom(DefaultProjectFileSystemTest.class, "hidden-files/pom.xml");
      ProjectFileSystem fs = project.getFileSystem();
      List<File> files = fs.getSourceFiles();
      assertThat(files).hasSize(1);
      assertThat(files.get(0).getName()).isEqualTo("foo.sql");
    }
  }

  @Test
  public void shouldUseExtendedFilters() {
    ProjectFileSystem fsWithoutFilter = project.getFileSystem();
    assertThat(fsWithoutFilter.getSourceFiles()).hasSize(2);
    assertThat(fsWithoutFilter.getSourceFiles()).onProperty("name").contains("Bar.java");

    FileFilter filter = new FileFilter() {
      public boolean accept(File file) {
        return !StringUtils.equals(file.getName(), "Bar.java");
      }
    };
    DefaultProjectFileSystem fsWithFilter = new DefaultProjectFileSystem(project, new Languages(Java.INSTANCE), filter);
    assertThat(fsWithFilter.getSourceFiles()).hasSize(1);
    assertThat(fsWithFilter.getSourceFiles()).onProperty("name").excludes("Bar.java");
  }

  @Test
  public void testSelectiveFileFilter() {
    DefaultProjectFileSystem.FileSelectionFilter filter = new DefaultProjectFileSystem.FileSelectionFilter(
        Arrays.asList(new File("foo/Bar.java"), new File("hello/Bar.java"), new File("hello/World.java")));
    assertThat(filter.accept(new File("foo/Bar.java"))).isTrue();
    assertThat(filter.accept(new File("hello/Bar.java"))).isTrue();
    assertThat(filter.accept(new File("hello/World.java"))).isTrue();

    assertThat(filter.accept(new File("foo/Unknown.java"))).isFalse();
    assertThat(filter.accept(new File("foo/bar/Bar.java"))).isFalse();
    assertThat(filter.accept(new File("foo/World.java"))).isFalse();
  }

  /**
   * SONAR-3096
   */
  @Test
  public void shouldExcludeDirectoriesStartingWithDot() {
    List<File> dirs = Arrays.asList(new File("test-resources/org/sonar/api/resources/DefaultProjectFileSystemTest/shouldExcludeDirectoriesStartingWithDot/src"));

    List<InputFile> files = new DefaultProjectFileSystem(new Project("foo"), null).getFiles(dirs, Collections.<File> emptyList(), new String[0], new String[0], true);
    assertThat(files).hasSize(1);
    assertThat(files.get(0).getRelativePath()).isEqualTo("org/sonar/Included.java");
  }
}
