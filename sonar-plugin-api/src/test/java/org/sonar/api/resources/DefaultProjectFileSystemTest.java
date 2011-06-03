/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.project.MavenProject;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.FileFilter;
import org.sonar.api.test.MavenTestUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class DefaultProjectFileSystemTest {

  private Project project = null;

  @Before
  public void before() {
    project = MavenTestUtils.loadProjectFromPom(DefaultProjectFileSystemTest.class, "sample/pom.xml");
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2266
   */
  @Test
  public void shouldReturnOnlyExistingSourceAndTestDirectories() {
    // in this example : "src/main/java" is a file, "src/test/java" doesn't exists
    project = MavenTestUtils.loadProjectFromPom(DefaultProjectFileSystemTest.class, "nonexistent-dirs/pom.xml");
    DefaultProjectFileSystem fs = newDefaultProjectFileSystem(project);
    assertThat(fs.getSourceDirs().size(), is(0));
    assertThat(fs.getTestDirs().size(), is(0));
  }

  @Test
  public void getJavaSourceFiles() {
    final DefaultProjectFileSystem fs = newDefaultProjectFileSystem(project);

    assertThat(fs.getJavaSourceFiles().size(), is(2));
    assertThat(fs.getJavaSourceFiles(), hasItem(named("Bar.java")));
    assertThat(fs.getJavaSourceFiles(), hasItem(named("Whizz.java")));
  }

  @Test
  public void hasJavaSourceFiles() {
    final DefaultProjectFileSystem fs = newDefaultProjectFileSystem(project);
    assertThat(fs.hasJavaSourceFiles(), is(true));

    project.setExclusionPatterns(new String[] { "**/*.java" });
    assertThat(fs.hasJavaSourceFiles(), is(false));
  }

  @Test
  public void getTestFiles() {
    final DefaultProjectFileSystem fs = newDefaultProjectFileSystem(project);

    assertThat(fs.getTestFiles(Java.INSTANCE).size(), is(1));
    assertThat(fs.getTestFiles(Java.INSTANCE), hasItem(named("BarTest.java")));
  }

  @Test
  public void applyExclusionPatternsToSourceFiles() {
    project.setExclusionPatterns(new String[] { "**/B*.java" });

    final DefaultProjectFileSystem fs = newDefaultProjectFileSystem(project);

    assertThat(fs.getJavaSourceFiles().size(), is(1));
    assertThat(fs.getJavaSourceFiles(), hasItem(named("Whizz.java")));
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-1449
   */
  @Test
  public void exclusionPatternOnAjFiles() {
    project.setExclusionPatterns(new String[] { "**/*.aj" });

    final DefaultProjectFileSystem fs = newDefaultProjectFileSystem(project);

    assertThat(fs.getSourceFiles(Java.INSTANCE).size(), is(2));
    assertThat(fs.getSourceFiles(Java.INSTANCE), hasItem(named("Whizz.java")));
    assertThat(fs.getSourceFiles(Java.INSTANCE), hasItem(named("Bar.java")));
  }

  @Test
  public void doNotApplyExclusionPatternsToTestFiles() {
    project.setExclusionPatterns(new String[] { "**/B*.java" });

    final DefaultProjectFileSystem fs = newDefaultProjectFileSystem(project);

    assertThat(fs.getTestFiles(Java.INSTANCE).size(), is(1));
    assertThat(fs.getTestFiles(Java.INSTANCE), hasItem(named("BarTest.java")));
  }

  @Test
  public void createSonarWorkingDirectory() {
    DefaultProjectFileSystem fs = newDefaultProjectFileSystem(project);
    java.io.File dir = fs.getSonarWorkingDirectory();
    assertThat(dir.exists(), is(true));
    assertThat(dir.listFiles().length, is(0));
  }

  @Test
  public void getJapaneseCharSet() {
    project = MavenTestUtils.loadProjectFromPom(DefaultProjectFileSystemTest.class, "japanese-project/pom.xml");
    DefaultProjectFileSystem fs = newDefaultProjectFileSystem(project);
    assertThat(fs.getSourceCharset().name(), is("Shift_JIS"));
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
    ProjectFileSystem fs = newDefaultProjectFileSystem(project);
    List<File> files = fs.getSourceFiles(new NoSuffixLanguage());
    assertThat(files.size(), is(2));
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2280
   */
  @Test
  public void resolvePathShouldReturnCanonicalFile() {
    MavenProject pom = mock(MavenProject.class);
    when(pom.getBasedir()).thenReturn(new File("/project"));
    Project project = new Project("").setPom(pom);
    DefaultProjectFileSystem fs = new DefaultProjectFileSystem(project, null);

    assertThat(fs.resolvePath(".").getAbsolutePath(), endsWith("project"));
    assertThat(fs.resolvePath("../project").getAbsolutePath(), endsWith("project"));
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
      ProjectFileSystem fs = newDefaultProjectFileSystem(project);
      List<File> files = fs.getSourceFiles();
      assertThat(files.size(), is(1));
      assertThat(files.get(0).getName(), is("foo.sql"));
    }
  }

  @Test
  public void shouldUseExtendedFilters() {
    DefaultProjectFileSystem fsWithoutFilter = newDefaultProjectFileSystem(project);
    assertThat(fsWithoutFilter.getSourceFiles().size(), is(2));
    assertThat(fsWithoutFilter.getSourceFiles(), hasItem(named("Bar.java")));

    FileFilter filter = new FileFilter() {
      public boolean accept(File file) {
        return !StringUtils.equals(file.getName(), "Bar.java");
      }
    };
    DefaultProjectFileSystem fsWithFilter = new DefaultProjectFileSystem(project, new Languages(Java.INSTANCE), filter);
    assertThat(fsWithFilter.getSourceFiles().size(), is(1));
    assertThat(fsWithFilter.getSourceFiles(), not(hasItem(named("Bar.java"))));
  }

  @Test
  public void testSelectiveFileFilter() {
    DefaultProjectFileSystem.FileSelectionFilter filter = new DefaultProjectFileSystem.FileSelectionFilter(
        Arrays.asList(new File("foo/Bar.java"), new File("hello/Bar.java"), new File("hello/World.java")));
    assertThat(filter.accept(new File("foo/Bar.java")), Matchers.is(true));
    assertThat(filter.accept(new File("hello/Bar.java")), Matchers.is(true));
    assertThat(filter.accept(new File("hello/World.java")), Matchers.is(true));

    assertThat(filter.accept(new File("foo/Unknown.java")), Matchers.is(false));
    assertThat(filter.accept(new File("foo/bar/Bar.java")), Matchers.is(false));
    assertThat(filter.accept(new File("foo/World.java")), Matchers.is(false));
  }

  private DefaultProjectFileSystem newDefaultProjectFileSystem(Project project) {
    return (DefaultProjectFileSystem) project.getFileSystem();
  }

  private static Matcher<java.io.File> named(final String name) {
    return new TypeSafeMatcher<java.io.File>() {
      java.io.File fileTested;

      @Override
      public boolean matchesSafely(java.io.File item) {
        fileTested = item;
        return name.equals(item.getName());
      }

      public void describeTo(Description description) {
        description.appendText(" that file ");
        description.appendValue(fileTested);
        description.appendText(" is named");
        description.appendText(name);
        description.appendText(" not ");
        description.appendValue(fileTested.getName());
      }
    };
  }
}