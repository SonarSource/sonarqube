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
package org.sonar.server.platform;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class ClassLoaderUtilsTest {

  private ClassLoader classLoader;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void prepareClassLoader() {
    //  This JAR file has the three following files :
    //    org/sonar/sqale/app/copyright.txt
    //    org/sonar/sqale/app/README.md
    //    org/sonar/other/other.txt
    URL jarUrl = getClass().getResource("/org/sonar/server/platform/ClassLoaderUtilsTest/ClassLoaderUtilsTest.jar");
    classLoader = new URLClassLoader(new URL[]{jarUrl}, /* no parent classloader */null);
  }

  @Test
  public void listResources_unknown_root() {
    Collection<String> strings = ClassLoaderUtils.listResources(classLoader, "unknown/directory", Predicates.<String>alwaysTrue());
    assertThat(strings.size(), Is.is(0));
  }

  @Test
  public void listResources_all() {
    Collection<String> strings = ClassLoaderUtils.listResources(classLoader, "org/sonar/sqale", Predicates.<String>alwaysTrue());
    assertThat(strings, hasItems(
      "org/sonar/sqale/",
      "org/sonar/sqale/app/",
      "org/sonar/sqale/app/copyright.txt",
      "org/sonar/sqale/app/README.md"));
    assertThat(strings.size(), Is.is(4));
  }

  @Test
  public void listResources_use_predicate() {
    Collection<String> strings = ClassLoaderUtils.listResources(classLoader, "org/sonar/sqale", new Predicate<String>() {
      public boolean apply(@Nullable String s) {
        return StringUtils.endsWith(s, "md");
      }
    });
    assertThat(strings.size(), Is.is(1));
    assertThat(strings, hasItems("org/sonar/sqale/app/README.md"));
  }

  @Test
  public void listFiles() {
    Collection<String> strings = ClassLoaderUtils.listFiles(classLoader, "org/sonar/sqale");
    assertThat(strings, hasItems(
      "org/sonar/sqale/app/copyright.txt",
      "org/sonar/sqale/app/README.md"));
    assertThat(strings.size(), Is.is(2));
  }

  @Test
  public void copyRubyRailsApp() throws IOException {
    File toDir = temp.newFolder("dest");
    ClassLoaderUtils.copyResources(classLoader, "org/sonar/sqale", toDir, Functions.<String>identity());

    assertThat(FileUtils.listFiles(toDir, null, true).size(), Is.is(2));
    assertThat(new File(toDir, "org/sonar/sqale/app/copyright.txt").exists(), Is.is(true));
    assertThat(new File(toDir, "org/sonar/sqale/app/README.md").exists(), Is.is(true));
  }

  @Test
  public void copyRubyRailsApp_relocate_files() throws IOException {
    File toDir = temp.newFolder("dest");
    ClassLoaderUtils.copyResources(classLoader, "org/sonar/sqale", toDir, new Function<String, String>() {
      public String apply(@Nullable String path) {
        return "foo/" + FilenameUtils.getName(path);
      }
    });

    assertThat(FileUtils.listFiles(toDir, null, true).size(), Is.is(2));
    assertThat(new File(toDir, "foo/copyright.txt").exists(), Is.is(true));
    assertThat(new File(toDir, "foo/README.md").exists(), Is.is(true));
  }
}
