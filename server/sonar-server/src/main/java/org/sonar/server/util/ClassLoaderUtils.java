/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.util;

import com.google.common.base.Throwables;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.log.Loggers;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ClassLoaderUtils {

  private ClassLoaderUtils() {
    // only static methods
  }

  /**
   * Finds files within a given directory and its subdirectories
   *
   * @param classLoader
   * @param rootPath    the root directory, for example org/sonar/sqale
   * @return a list of relative paths, for example {"org/sonar/sqale/foo/bar.txt}. Never null.
   */
  public static Collection<String> listFiles(ClassLoader classLoader, String rootPath) {
    return listResources(classLoader, rootPath, path -> !StringUtils.endsWith(path, "/"));
  }

  /**
   * Finds directories and files within a given directory and its subdirectories.
   *
   * @param classLoader
   * @param rootPath    the root directory, for example org/sonar/sqale, or a file in this root directory, for example org/sonar/sqale/index.txt
   * @param predicate
   * @return a list of relative paths, for example {"org/sonar/sqale", "org/sonar/sqale/foo", "org/sonar/sqale/foo/bar.txt}. Never null.
   */
  public static Collection<String> listResources(ClassLoader classLoader, String rootPath, Predicate<String> predicate) {
    String jarPath = null;
    JarFile jar = null;
    try {
      Collection<String> paths = new ArrayList<>();
      URL root = classLoader.getResource(rootPath);
      if (root != null) {
        checkJarFile(root);

        // Path of the root directory
        // Examples :
        // org/sonar/sqale/index.txt -> rootDirectory is org/sonar/sqale
        // org/sonar/sqale/ -> rootDirectory is org/sonar/sqale
        // org/sonar/sqale -> rootDirectory is org/sonar/sqale
        String rootDirectory = rootPath;
        if (StringUtils.substringAfterLast(rootPath, "/").indexOf('.') >= 0) {
          rootDirectory = StringUtils.substringBeforeLast(rootPath, "/");
        }
        // strip out only the JAR file
        jarPath = root.getPath().substring(5, root.getPath().indexOf('!'));
        jar = new JarFile(URLDecoder.decode(jarPath, UTF_8.name()));
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
          String name = entries.nextElement().getName();
          if (name.startsWith(rootDirectory) && predicate.test(name)) {
            paths.add(name);
          }
        }
      }
      return paths;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    } finally {
      closeJar(jar, jarPath);
    }
  }

  private static void closeJar(@Nullable JarFile jar, String jarPath) {
    if (jar != null) {
      try {
        jar.close();
      } catch (Exception e) {
        Loggers.get(ClassLoaderUtils.class).error("Fail to close JAR file: " + jarPath, e);
      }
    }
  }

  private static void checkJarFile(URL root) {
    if (!"jar".equals(root.getProtocol())) {
      throw new IllegalStateException("Unsupported protocol: " + root.getProtocol());
    }
  }
}
