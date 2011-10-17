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
package org.sonar.java.bytecode;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.ClassWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.bytecode.loader.SquidClassLoader;

public final class ClassworldsClassLoader {

  private static final Logger LOG = LoggerFactory.getLogger(ClassworldsClassLoader.class);

  private ClassworldsClassLoader() {
    // only static methods
  }

  public static ClassLoader create(File bytecodeFileOrDirectory) {
    return create(Arrays.asList(bytecodeFileOrDirectory));
  }

  public static ClassLoader create(Collection<File> bytecodeFilesOrDirectories) {
    try {
      return new SquidClassLoader(bytecodeFilesOrDirectories);
    } catch (Exception e) {
      throw new IllegalStateException("Can not create classloader", e);
    }
  }

  public static ClassLoader createUsingClassWorld(Collection<File> bytecodeFilesOrDirectories) {
    try {
      ClassWorld world = new ClassWorld();
      ClassRealm realm = world.newRealm("squid.project", null /* explicit declaration that parent should be bootstrap class loader */);

      for (File bytecode : bytecodeFilesOrDirectories) {
        URL url = getURL(bytecode);
        if (bytecode.isFile() && url.toString().endsWith(".class")) {
          LOG.info("Sonar Squid ClassLoader was expecting a JAR file instead of CLASS file : '" + bytecode.getAbsolutePath() + "'");
        } else {
          // JAR file or directory
          realm.addConstituent(url);
        }
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("----- Classpath analyzed by Squid:");
        for (URL url : realm.getConstituents()) {
          LOG.debug(url.toString());
        }
        LOG.debug("-----");
      }

      return realm.getClassLoader();

    } catch (Exception e) {
      throw new IllegalStateException("Can not create classloader", e);
    }
  }

  private static URL getURL(File file) throws MalformedURLException {
    URL url = file.toURI().toURL();
    if (file.isDirectory() && !url.toString().endsWith("/")) {
      /*
       * See ClassRealm javadoc : If the constituent is a directory, then the URL must end with a slash (/). Otherwise the constituent will
       * be treated as a JAR file.
       */
      url = new URL(url.toString() + "/");
    }
    return url;
  }
}