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
package org.sonar.test;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;

/**
 * Utilities for unit tests
 *
 * @since 2.2
 */
public final class TestUtils {

  private TestUtils() {
  }

  /**
   * Search for a test resource in the classpath. For example getResource("org/sonar/MyClass/foo.txt");
   *
   * @param path the starting slash is optional
   * @return the resource. Null if resource not found
   */
  public static File getResource(String path) {
    String resourcePath = path;
    if (!resourcePath.startsWith("/")) {
      resourcePath = "/" + resourcePath;
    }
    URL url = TestUtils.class.getResource(resourcePath);
    if (url != null) {
      return FileUtils.toFile(url);
    }
    return null;
  }

  /**
   * Search for a resource in the classpath. For example calling the method getResource(getClass(), "myTestName/foo.txt") from
   * the class org.sonar.Foo loads the file $basedir/src/test/resources/org/sonar/Foo/myTestName/foo.txt
   *
   * @return the resource. Null if resource not found
   */
  public static File getResource(Class baseClass, String path) {
    String resourcePath = StringUtils.replaceChars(baseClass.getCanonicalName(), '.', '/');
    if (!path.startsWith("/")) {
      resourcePath += "/";
    }
    resourcePath += path;
    return getResource(resourcePath);
  }

  /**
   * Asserts that all constructors are private, usually for helper classes with
   * only static methods. If a constructor does not have any parameters, then
   * it's instantiated.
   */
  public static boolean hasOnlyPrivateConstructors(Class clazz) {
    boolean ok = true;
    for (Constructor constructor : clazz.getDeclaredConstructors()) {
      ok &= Modifier.isPrivate(constructor.getModifiers());
      if (constructor.getParameterTypes().length == 0) {
        constructor.setAccessible(true);
        try {
          constructor.newInstance();
        } catch (Exception e) {
          throw new IllegalStateException(String.format("Fail to instantiate %s", clazz), e);
        }
      }
    }
    return ok;
  }

  public static File newTempDir(String prefix) {
    try {
      // Technique to create a temp directory from a temp file
      File f = File.createTempFile(prefix, "");
      f.delete();
      f.mkdir();
      return f;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to create temp dir", e);
    }
  }
}
