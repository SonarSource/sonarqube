/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.io.Closeable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.Plugin;
import org.sonar.updatecenter.common.Version;

import static java.util.Collections.singleton;

/**
 * Loads the plugin JAR files by creating the appropriate classloaders and by instantiating
 * the entry point classes as defined in manifests. It assumes that JAR files are compatible with current
 * environment (minimal sonarqube version, compatibility between plugins, ...):
 * <ul>
 * <li>server verifies compatibility of JARs before deploying them at startup (see ServerPluginRepository)</li>
 * <li>batch loads only the plugins deployed on server (see BatchPluginRepository)</li>
 * </ul>
 * <p/>
 * Plugins have their own isolated classloader, inheriting only from API classes.
 * Some plugins can extend a "base" plugin, sharing the same classloader.
 * <p/>
 * This class is stateless. It does not keep pointers to classloaders and {@link org.sonar.api.Plugin}.
 */
public class PluginClassLoader {
  private static final String[] DEFAULT_SHARED_RESOURCES = {"org/sonar/plugins", "com/sonar/plugins", "com/sonarsource/plugins"};
  private static final Version COMPATIBILITY_MODE_MAX_VERSION = Version.create("5.2");

  private final PluginClassloaderFactory classloaderFactory;
  private final Map<PluginClassLoaderDef, ClassLoader> classLoaders = new HashMap<>();

  public PluginClassLoader(PluginClassloaderFactory classloaderFactory) {
    this.classloaderFactory = classloaderFactory;
  }

  public Map<String, Plugin> load(Collection<ExplodedPlugin> plugins) {
    return load(plugins.stream().collect(Collectors.toMap(ExplodedPlugin::getKey, x -> x)));
  }

  public Map<String, Plugin> load(Map<String, ExplodedPlugin> pluginsByKey) {
    Collection<PluginClassLoaderDef> defs = defineClassloaders(pluginsByKey);
    Map<PluginClassLoaderDef, ClassLoader> newClassloaders = classloaderFactory.create(classLoaders, defs);
    classLoaders.putAll(newClassloaders);
    return instantiatePluginClasses(newClassloaders);
  }

  /**
   * Defines the different classloaders to be created. Number of classloaders can be
   * different than number of plugins.
   */
  @VisibleForTesting
  Collection<PluginClassLoaderDef> defineClassloaders(Map<String, ExplodedPlugin> pluginsByKey) {
    Map<String, PluginClassLoaderDef> classloadersByBasePlugin = new HashMap<>();

    for (ExplodedPlugin plugin : pluginsByKey.values()) {
      PluginInfo info = plugin.getPluginInfo();
      String baseKey = basePluginKey(info, pluginsByKey);
      PluginClassLoaderDef def = classloadersByBasePlugin.get(baseKey);
      if (def == null) {
        def = new PluginClassLoaderDef(baseKey);
        classloadersByBasePlugin.put(baseKey, def);
      }
      def.addFiles(singleton(plugin.getMain()));
      def.addFiles(plugin.getLibs());
      def.addMainClass(info.getKey(), info.getMainClass());

      for (String defaultSharedResource : DEFAULT_SHARED_RESOURCES) {
        def.getExportMask().include(String.format("%s/%s/api/", defaultSharedResource, info.getKey()));
      }

      // The plugins that extend other plugins can only add some files to classloader.
      // They can't change metadata like ordering strategy or compatibility mode.
      if (Strings.isNullOrEmpty(info.getBasePlugin())) {
        if (info.isUseChildFirstClassLoader()) {
          LoggerFactory.getLogger(getClass()).warn("Plugin {} [{}] uses a child first classloader which is deprecated", info.getName(),
            info.getKey());
        }
        def.setSelfFirstStrategy(info.isUseChildFirstClassLoader());
        Version minSonarPluginApiVersion = info.getMinimalSonarPluginApiVersion();
        boolean compatibilityMode = minSonarPluginApiVersion != null && minSonarPluginApiVersion.compareToIgnoreQualifier(COMPATIBILITY_MODE_MAX_VERSION) < 0;
        if (compatibilityMode) {
          LoggerFactory.getLogger(getClass()).warn("API compatibility mode is no longer supported. In case of error, plugin {} [{}] " +
              "should package its dependencies.",
            info.getName(), info.getKey());
        }
      }
    }
    return classloadersByBasePlugin.values();

  }

  /**
   * Instantiates collection of {@link org.sonar.api.Plugin} according to given metadata and classloaders
   *
   * @return the instances grouped by plugin key
   * @throws IllegalStateException if at least one plugin can't be correctly loaded
   */
  private static Map<String, Plugin> instantiatePluginClasses(Map<PluginClassLoaderDef, ClassLoader> classloaders) {
    // instantiate plugins
    Map<String, Plugin> instancesByPluginKey = new HashMap<>();
    for (Map.Entry<PluginClassLoaderDef, ClassLoader> entry : classloaders.entrySet()) {
      PluginClassLoaderDef def = entry.getKey();
      ClassLoader classLoader = entry.getValue();

      // the same classloader can be used by multiple plugins
      for (Map.Entry<String, String> mainClassEntry : def.getMainClassesByPluginKey().entrySet()) {
        String pluginKey = mainClassEntry.getKey();
        String mainClass = mainClassEntry.getValue();
        try {
          instancesByPluginKey.put(pluginKey, (Plugin) classLoader.loadClass(mainClass).getDeclaredConstructor().newInstance());
        } catch (UnsupportedClassVersionError e) {
          throw new IllegalStateException(String.format("The plugin [%s] does not support Java %s", pluginKey, SystemUtils.JAVA_VERSION_TRIMMED), e);
        } catch (Throwable e) {
          throw new IllegalStateException(String.format("Fail to instantiate class [%s] of plugin [%s]", mainClass, pluginKey), e);
        }
      }
    }
    return instancesByPluginKey;
  }

  public void unload(Collection<Plugin> plugins) {
    for (Plugin plugin : plugins) {
      ClassLoader classLoader = plugin.getClass().getClassLoader();
      if (classLoader instanceof Closeable closeable && classLoader != classloaderFactory.baseClassLoader()) {
        try {
          closeable.close();
        } catch (Exception e) {
          LoggerFactory.getLogger(getClass()).error("Fail to close classloader " + classLoader.toString(), e);
        }
      }
    }
  }

  /**
   * Get the root key of a tree of plugins. For example if plugin C depends on B, which depends on A, then
   * B and C must be attached to the classloader of A. The method returns A in the three cases.
   */
  private static String basePluginKey(PluginInfo plugin, Map<String, ExplodedPlugin> pluginsByKey) {
    String base = plugin.getKey();
    String parentKey = plugin.getBasePlugin();
    while (!Strings.isNullOrEmpty(parentKey)) {
      PluginInfo parentPlugin = pluginsByKey.get(parentKey).getPluginInfo();
      base = parentPlugin.getKey();
      parentKey = parentPlugin.getBasePlugin();
    }
    return base;
  }
}
