/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.sonar.server.util.ClassLoaderUtils;

import static org.apache.commons.lang.StringUtils.endsWith;
import static org.assertj.core.api.Assertions.assertThat;

public class ClassLoaderUtilsTest {

  private ClassLoader classLoader;

  @Before
  public void prepareClassLoader() {
    // This JAR file has the three following files :
    // org/sonar/sqale/app/copyright.txt
    // org/sonar/sqale/app/README.md
    // org/sonar/other/other.txt
    URL jarUrl = getClass().getResource("/org/sonar/server/platform/ClassLoaderUtilsTest/ClassLoaderUtilsTest.jar");
    classLoader = new URLClassLoader(new URL[] {jarUrl}, /* no parent classloader */null);
  }

  @Test
  public void listResources_unknown_root() {
    Collection<String> strings = ClassLoaderUtils.listResources(classLoader, "unknown/directory", s -> true);
    assertThat(strings).isEmpty();
  }

  @Test
  public void listResources_all() {
    Collection<String> strings = ClassLoaderUtils.listResources(classLoader, "org/sonar/sqale", s -> true);
    assertThat(strings).containsOnly(
      "org/sonar/sqale/",
      "org/sonar/sqale/app/",
      "org/sonar/sqale/app/copyright.txt",
      "org/sonar/sqale/app/README.md");
  }

  @Test
  public void listResources_use_predicate() {
    Collection<String> strings = ClassLoaderUtils.listResources(classLoader, "org/sonar/sqale", s -> endsWith(s, "md"));
    assertThat(strings).containsOnly("org/sonar/sqale/app/README.md");
  }

  @Test
  public void listFiles() {
    Collection<String> strings = ClassLoaderUtils.listFiles(classLoader, "org/sonar/sqale");
    assertThat(strings).containsOnly(
      "org/sonar/sqale/app/copyright.txt",
      "org/sonar/sqale/app/README.md");
  }

}
