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
package org.sonar.server.plugins;

import com.google.common.base.*;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * TODO it this class needed in sonar-plugin-api ?
 *
 * @since 2.15
 */
public final class ClassLoaderUtils {

  private ClassLoaderUtils() {
  }

  public static File copyResources(ClassLoader classLoader, String rootPath, File toDir) {
    return copyResources(classLoader, rootPath, toDir, Functions.<String>identity());
  }

  public static File copyResources(ClassLoader classLoader, String rootPath, File toDir, Function<String, String> relocationFunction) {
    Collection<String> relativePaths = listFiles(classLoader, rootPath);
    for (String relativePath : relativePaths) {
      URL resource = classLoader.getResource(relativePath);
      String filename = relocationFunction.apply(relativePath);
      File toFile = new File(toDir, filename);
      try {
        FileUtils.copyURLToFile(resource, toFile);
      } catch (IOException e) {
        throw new IllegalStateException("Fail to extract " + relativePath + " to " + toFile.getAbsolutePath());
      }
    }

    return toDir;
  }

  public static Collection<String> listFiles(ClassLoader classLoader, String rootPath) {
    return listResources(classLoader, rootPath, new Predicate<String>() {
      @Override
      public boolean apply(@Nullable String path) {
        return !StringUtils.endsWith(path, "/");
      }
    });
  }

  public static Collection<String> listResources(ClassLoader classloader, String rootPath) {
    return listResources(classloader, rootPath, Predicates.<String>alwaysTrue());
  }

  public static Collection<String> listResources(ClassLoader classloader, String rootPath, Predicate<String> predicate) {
    try {
      Collection<String> paths = Lists.newArrayList();
      rootPath = StringUtils.removeStart(rootPath, "/");

      URL root = classloader.getResource(rootPath);
      if (root == null) {
        return paths;
      }
      if (!"jar".equals(root.getProtocol())) {
        throw new IllegalStateException("Unsupported protocol: " + root.getProtocol());
      }
      String jarPath = root.getPath().substring(5, root.getPath().indexOf("!")); //strip out only the JAR file
      JarFile jar = new JarFile(URLDecoder.decode(jarPath, CharEncoding.UTF_8));
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        String name = entries.nextElement().getName();
        if (name.startsWith(rootPath) && predicate.apply(name)) {
          paths.add(name);
        }
      }
      return paths;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
