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
package org.sonar.core.platform;

import com.google.common.base.Strings;
import org.apache.commons.lang.SystemUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.Plugin;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.log.Loggers;
import org.sonar.classloader.ClassloaderBuilder;
import org.sonar.classloader.ClassloaderBuilder.LoadingOrder;
import org.sonar.classloader.Mask;

import java.io.Closeable;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.sonar.classloader.ClassloaderBuilder.LoadingOrder.SELF_FIRST;

/**
 * Loads the plugin JAR files by creating the appropriate classloaders and by instantiating
 * the entry point classes as defined in manifests. It assumes that JAR files are compatible with current
 * environment (minimal sonarqube version, compatibility between plugins, ...).
 * <p/>
 * Standard plugins have their own isolated classloader. Some others can extend a "base" plugin.
 * In this case they share the same classloader then the base plugin.
 * <p/>
 * This class is stateless. It does not keep classloaders and {@link Plugin} in memory.
 */
public class PluginLoader implements BatchComponent, ServerComponent {

  private static final String[] DEFAULT_SHARED_RESOURCES = {"org/sonar/plugins/", "com/sonar/plugins/", "com/sonarsource/plugins/"};

  /**
   * Information about the classloader to be created for a set of plugins.
   */
  static class ClassloaderDef {
    final String basePluginKey;
    final Map<String, String> mainClassesByPluginKey = new HashMap<>();
    final List<File> files = new ArrayList<>();
    final Mask mask = new Mask();
    boolean selfFirstStrategy = false;
    ClassLoader classloader = null;

    public ClassloaderDef(String basePluginKey) {
      this.basePluginKey = basePluginKey;
    }
  }

  private final PluginUnzipper unzipper;

  public PluginLoader(PluginUnzipper unzipper) {
    this.unzipper = unzipper;
  }

  public Map<String, Plugin> load(Map<String, PluginInfo> infoByKeys) {
    Collection<ClassloaderDef> defs = defineClassloaders(infoByKeys).values();
    buildClassloaders(defs);
    return instantiatePluginInstances(defs);
  }

  /**
   * Step 1 - define the different classloaders to be created. Number of classloaders can be
   * different than number of plugins.
   */
  Map<String, ClassloaderDef> defineClassloaders(Map<String, PluginInfo> infoByKeys) {
    Map<String, ClassloaderDef> classloadersByBasePlugin = new HashMap<>();

    for (PluginInfo info : infoByKeys.values()) {
      String baseKey = basePluginKey(info, infoByKeys);
      ClassloaderDef def = classloadersByBasePlugin.get(baseKey);
      if (def == null) {
        def = new ClassloaderDef(baseKey);
        classloadersByBasePlugin.put(baseKey, def);
      }
      UnzippedPlugin unzippedPlugin = unzipper.unzip(info);
      def.files.add(unzippedPlugin.getMain());
      def.files.addAll(unzippedPlugin.getLibs());
      def.mainClassesByPluginKey.put(info.getKey(), info.getMainClass());
      for (String defaultSharedResource : DEFAULT_SHARED_RESOURCES) {
        def.mask.addInclusion(defaultSharedResource + info.getKey() + "/");
      }
      if (Strings.isNullOrEmpty(info.getBasePlugin())) {
        // The plugins that extend other plugins can only add some files to classloader.
        // They can't change ordering strategy.
        def.selfFirstStrategy = info.isUseChildFirstClassLoader();
      }
    }
    return classloadersByBasePlugin;
  }

  /**
   * Step 2 - create classloaders with appropriate constituents and metadata
   */
  void buildClassloaders(Collection<ClassloaderDef> defs) {
    ClassloaderBuilder builder = new ClassloaderBuilder();
    for (ClassloaderDef def : defs) {
      builder
        .newClassloader(def.basePluginKey, getClass().getClassLoader())
        .setExportMask(def.basePluginKey, def.mask)
        .setLoadingOrder(def.basePluginKey, def.selfFirstStrategy ? SELF_FIRST : LoadingOrder.PARENT_FIRST);
      for (File file : def.files) {
        builder.addURL(def.basePluginKey, fileToUrl(file));
      }
    }
    Map<String, ClassLoader> classloadersByBasePluginKey = builder.build();
    for (ClassloaderDef def : defs) {
      def.classloader = classloadersByBasePluginKey.get(def.basePluginKey);
    }
  }

  /**
   * Step 3 - instantiate plugin instances ({@link Plugin}
   *
   * @return the instances grouped by plugin key
   * @throws IllegalStateException if at least one plugin can't be correctly loaded
   */
  Map<String, Plugin> instantiatePluginInstances(Collection<ClassloaderDef> defs) {
    // instantiate plugins
    Map<String, Plugin> instancesByPluginKey = new HashMap<>();
    for (ClassloaderDef def : defs) {
      // the same classloader can be used by multiple plugins
      for (Map.Entry<String, String> entry : def.mainClassesByPluginKey.entrySet()) {
        String pluginKey = entry.getKey();
        String mainClass = entry.getValue();
        try {
          instancesByPluginKey.put(pluginKey, (Plugin) def.classloader.loadClass(mainClass).newInstance());
        } catch (UnsupportedClassVersionError e) {
          throw new IllegalStateException(String.format("The plugin [%s] does not support Java %s",
            pluginKey, SystemUtils.JAVA_VERSION_TRIMMED), e);
        } catch (Exception e) {
          throw new IllegalStateException(String.format(
            "Fail to instantiate class [%s] of plugin [%s]", mainClass, pluginKey), e);
        }
      }
    }
    return instancesByPluginKey;
  }

  public void unload(Collection<Plugin> plugins) {
    for (Plugin plugin : plugins) {
      ClassLoader classLoader = plugin.getClass().getClassLoader();
      if (classLoader instanceof Closeable && classLoader != getClass().getClassLoader()) {
        try {
          ((Closeable) classLoader).close();
        } catch (Exception e) {
          Loggers.get(getClass()).error("Fail to close classloader " + classLoader.toString(), e);
        }
      }
    }
  }

  /**
   * Get the root key of a tree of plugins. For example if plugin C depends on B, which depends on A, then
   * B and C must be attached to the classloader of A. The method returns A in the three cases.
   */
  static String basePluginKey(PluginInfo plugin, Map<String, PluginInfo> allPluginsPerKey) {
    String base = plugin.getKey();
    String parentKey = plugin.getBasePlugin();
    while (!Strings.isNullOrEmpty(parentKey)) {
      PluginInfo parentPlugin = allPluginsPerKey.get(parentKey);
      base = parentPlugin.getKey();
      parentKey = parentPlugin.getBasePlugin();
    }
    return base;
  }

  private URL fileToUrl(File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    }
  }
}
