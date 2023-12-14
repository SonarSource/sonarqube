/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.core.platform;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.server.ServerSide;
import org.sonar.classloader.ClassloaderBuilder;
import org.sonar.classloader.Mask;

import static org.sonar.classloader.ClassloaderBuilder.LoadingOrder.PARENT_FIRST;
import static org.sonar.classloader.ClassloaderBuilder.LoadingOrder.SELF_FIRST;

/**
 * Builds the graph of classloaders to be used to instantiate plugins. It deals with:
 * <ul>
 *   <li>isolation of plugins against core classes (except api)</li>
 *   <li>sharing of some packages between plugins</li>
 *   <li>loading of the libraries embedded in plugin JAR files (directory META-INF/libs)</li>
 * </ul>
 */
@ScannerSide
@ServerSide
@ComputeEngineSide
public class PluginClassloaderFactory {

  // underscores are used to not conflict with plugin keys (if someday a plugin key is "api")
  private static final String API_CLASSLOADER_KEY = "_api_";

  /**
   * Creates as many classloaders as requested by the input parameter.
   */
  public Map<PluginClassLoaderDef, ClassLoader> create(Map<PluginClassLoaderDef, ClassLoader> previouslyCreatedClassloaders,
    Collection<PluginClassLoaderDef> newDefs) {
    ClassLoader baseClassLoader = baseClassLoader();

    Collection<PluginClassLoaderDef> allDefs = new HashSet<>();
    allDefs.addAll(newDefs);
    allDefs.addAll(previouslyCreatedClassloaders.keySet());

    ClassloaderBuilder builder = new ClassloaderBuilder(previouslyCreatedClassloaders.values());
    builder.newClassloader(API_CLASSLOADER_KEY, baseClassLoader);
    builder.setMask(API_CLASSLOADER_KEY, apiMask());

    for (PluginClassLoaderDef def : newDefs) {
      builder.newClassloader(def.getBasePluginKey());
      builder.setParent(def.getBasePluginKey(), API_CLASSLOADER_KEY, Mask.ALL);
      builder.setLoadingOrder(def.getBasePluginKey(), def.isSelfFirstStrategy() ? SELF_FIRST : PARENT_FIRST);
      for (File jar : def.getFiles()) {
        builder.addURL(def.getBasePluginKey(), fileToUrl(jar));
      }
      exportResources(def, builder, allDefs);
    }

    return build(newDefs, builder);
  }

  /**
   * A plugin can export some resources to other plugins
   */
  private static void exportResources(PluginClassLoaderDef newDef, ClassloaderBuilder builder,
    Collection<PluginClassLoaderDef> allPlugins) {
    // export the resources to all other plugins
    builder.setExportMask(newDef.getBasePluginKey(), newDef.getExportMask().build());
    for (PluginClassLoaderDef other : allPlugins) {
      if (!other.getBasePluginKey().equals(newDef.getBasePluginKey())) {
        builder.addSibling(newDef.getBasePluginKey(), other.getBasePluginKey(), Mask.ALL);
      }
    }
  }

  /**
   * Builds classloaders and verifies that all of them are correctly defined
   */
  private static Map<PluginClassLoaderDef, ClassLoader> build(Collection<PluginClassLoaderDef> defs, ClassloaderBuilder builder) {
    Map<PluginClassLoaderDef, ClassLoader> result = new HashMap<>();
    Map<String, ClassLoader> classloadersByBasePluginKey = builder.build();
    for (PluginClassLoaderDef def : defs) {
      ClassLoader classloader = classloadersByBasePluginKey.get(def.getBasePluginKey());
      if (classloader == null) {
        throw new IllegalStateException(String.format("Fail to create classloader for plugin [%s]", def.getBasePluginKey()));
      }
      result.put(def, classloader);
    }
    return result;
  }

  ClassLoader baseClassLoader() {
    return getClass().getClassLoader();
  }

  private static URL fileToUrl(File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * The resources (packages) that API exposes to plugins. Other core classes (SonarQube, MyBatis, ...)
   * can't be accessed.
   * <p>To sum-up, these are the classes packaged in sonar-plugin-api.jar or available as
   * a transitive dependency of sonar-plugin-api</p>
   */
  private static Mask apiMask() {
    return Mask.builder()
      .include("org/sonar/api/",
        "org/sonar/check/",
        "org/codehaus/stax2/",
        "org/codehaus/staxmate/",
        "com/ctc/wstx/",
        "org/slf4j/",

        // SLF4J bridges. Do not let plugins re-initialize and configure their logging system
        "org/apache/commons/logging/",
        "org/apache/log4j/",
        "ch/qos/logback/",

        // Exposed by org.sonar.api.server.authentication.IdentityProvider
        "javax/servlet/",

        // required for some internal SonarSource plugins (billing, orchestrator, ...)
        "org/sonar/server/platform/",

        // required for commercial plugins at SonarSource
        "com/sonarsource/plugins/license/api/")

      // API exclusions
      .exclude("org/sonar/api/internal/")
      .build();
  }
}
