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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.bytecode.loader.SquidClassLoader;

import com.google.common.collect.Lists;

public final class ClassLoaderBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(ClassLoaderBuilder.class);

  private ClassLoaderBuilder() {
    // only static methods
  }

  public static ClassLoader create(Collection<File> bytecodeFilesOrDirectories) {
    List<File> files = Lists.newArrayList();
    for (File file : bytecodeFilesOrDirectories) {
      if (file.isFile() && file.getPath().endsWith(".class")) {
        LOG.info("Sonar Squid ClassLoader was expecting a JAR file instead of CLASS file : '" + file.getAbsolutePath() + "'");
      } else {
        files.add(file);
      }
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("----- Classpath analyzed by Squid:");
      for (File file : files) {
        LOG.debug(file.getAbsolutePath());
      }
      LOG.debug("-----");
    }

    try {
      return new SquidClassLoader(files);
    } catch (Exception e) {
      throw new IllegalStateException("Can not create ClassLoader", e);
    }
  }

  /**
   * For tests.
   */
  public static ClassLoader create(File bytecodeFileOrDirectory) {
    return create(Arrays.asList(bytecodeFileOrDirectory));
  }

}
