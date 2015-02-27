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
package org.sonar.core.plugins;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Plugin;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates manipulations with ClassLoaders, such as creation and establishing dependencies. Current implementation based on
 * {@link ClassWorld}.
 * <p/>
 * <h3>IMPORTANT</h3>
 * <p>
 * If we have pluginA , then all classes and resources from package and subpackages of <b>org.sonar.plugins.pluginA.api</b> will be visible
 * for all other plugins even if they are located in dependent library.
 * </p>
 * <p/>
 * <h4>Search order for {@link ClassRealm} :</h4>
 * <ul>
 * <li>parent class loader (passed via the constructor) if there is one</li>
 * <li>imports</li>
 * <li>realm's constituents</li>
 * <li>parent realm</li>
 * </ul>
 */
public class PluginClassloaders {

  private static final String[] PREFIXES_TO_EXPORT = {"org.sonar.plugins.", "com.sonar.plugins.", "com.sonarsource.plugins."};
  private static final Logger LOG = LoggerFactory.getLogger(PluginClassloaders.class);

  private ClassWorld world;
  private ClassLoader baseClassloader;
  private boolean done = false;

  public PluginClassloaders(ClassLoader baseClassloader) {
    this(baseClassloader, new ClassWorld());
  }

  @VisibleForTesting
  PluginClassloaders(ClassLoader baseClassloader, ClassWorld world) {
    this.baseClassloader = baseClassloader;
    this.world = world;
  }

  public Map<String, Plugin> init(Collection<PluginMetadata> plugins) {
    List<PluginMetadata> children = Lists.newArrayList();
    for (PluginMetadata plugin : plugins) {
      if (StringUtils.isBlank(plugin.getBasePlugin())) {
        add(plugin);
      } else {
        children.add(plugin);
      }
    }

    for (PluginMetadata child : children) {
      extend(child);
    }

    done();

    Map<String, Plugin> pluginsByKey = Maps.newHashMap();
    for (PluginMetadata metadata : plugins) {
      pluginsByKey.put(metadata.getKey(), instantiatePlugin(metadata));
    }
    return pluginsByKey;
  }

  public ClassLoader add(PluginMetadata plugin) {
    if (done) {
      throw new IllegalStateException("Plugin classloaders are already initialized");
    }
    try {
      List<URL> resources = Lists.newArrayList();
      List<URL> others = Lists.newArrayList();
      for (File file : plugin.getDeployedFiles()) {
        if (isResource(file)) {
          resources.add(file.toURI().toURL());
        } else {
          others.add(file.toURI().toURL());
        }
      }
      ClassLoader parent;
      if (resources.isEmpty()) {
        parent = baseClassloader;
      } else {
        parent = new ResourcesClassloader(resources, baseClassloader);
      }
      ClassRealm realm;
      if (plugin.isUseChildFirstClassLoader()) {
        ClassRealm parentRealm = world.newRealm(plugin.getKey() + "-parent", parent);
        realm = parentRealm.createChildRealm(plugin.getKey());
      } else {
        realm = world.newRealm(plugin.getKey(), parent);
      }
      for (URL url : others) {
        realm.addURL(url);
      }
      return realm;
    } catch (UnsupportedClassVersionError e) {
      throw new SonarException(String.format("The plugin %s is not supported with Java %s", plugin.getKey(),
        SystemUtils.JAVA_VERSION_TRIMMED), e);

    } catch (Exception e) {
      throw new SonarException(String.format("Fail to build the classloader of %s", plugin.getKey()), e);
    }
  }

  public boolean extend(PluginMetadata plugin) {
    if (done) {
      throw new IllegalStateException("Plugin classloaders are already initialized");
    }
    try {
      ClassRealm base = world.getRealm(plugin.getBasePlugin());
      if (base == null) {
        // Ignored, because base plugin is not installed
        LOG.warn(String.format("Plugin %s is ignored because base plugin is not installed: %s",
          plugin.getKey(), plugin.getBasePlugin()));
        return false;
      }
      // we create new realm to be able to return it by key without conversion to baseKey
      base.createChildRealm(plugin.getKey());
      for (File file : plugin.getDeployedFiles()) {
        base.addURL(file.toURI().toURL());
      }
      return true;
    } catch (UnsupportedClassVersionError e) {
      throw new SonarException(String.format("The plugin %s is not supported with Java %s",
        plugin.getKey(), SystemUtils.JAVA_VERSION_TRIMMED), e);

    } catch (Exception e) {
      throw new SonarException(String.format("Fail to extend the plugin %s for %s",
        plugin.getBasePlugin(), plugin.getKey()), e);
    }
  }

  /**
   * Establishes dependencies among ClassLoaders.
   */
  public void done() {
    if (done) {
      throw new IllegalStateException("Plugin classloaders are already initialized");
    }
    for (Object o : world.getRealms()) {
      ClassRealm realm = (ClassRealm) o;
      if (!StringUtils.endsWith(realm.getId(), "-parent")) {
        String[] packagesToExport = new String[PREFIXES_TO_EXPORT.length];
        for (int i = 0; i < PREFIXES_TO_EXPORT.length; i++) {
          // important to have dot at the end of package name only for classworlds 1.1
          packagesToExport[i] = String.format("%s%s.api", PREFIXES_TO_EXPORT[i], realm.getId());
        }
        export(realm, packagesToExport);
      }
    }
    done = true;
  }

  /**
   * Exports specified packages from given ClassRealm to all others.
   */
  private void export(ClassRealm realm, String... packages) {
    for (Object o : world.getRealms()) {
      ClassRealm dep = (ClassRealm) o;
      if (!StringUtils.equals(dep.getId(), realm.getId())) {
        try {
          for (String packageName : packages) {
            dep.importFrom(realm.getId(), packageName);
          }
        } catch (NoSuchRealmException e) {
          // should never happen
          throw new SonarException(e);
        }
      }
    }
  }

  /**
   * Note that this method should be called only after creation of all ClassLoaders - see {@link #done()}.
   */
  public ClassLoader get(String key) {
    if (!done) {
      throw new IllegalStateException("Plugin classloaders are not initialized");
    }
    try {
      return world.getRealm(key);
    } catch (NoSuchRealmException e) {
      return null;
    }
  }

  public Plugin instantiatePlugin(PluginMetadata plugin) {
    try {
      Class clazz = get(plugin.getKey()).loadClass(plugin.getMainClass());
      return (Plugin) clazz.newInstance();

    } catch (UnsupportedClassVersionError e) {
      throw new SonarException(String.format("The plugin %s is not supported with Java %s",
        plugin.getKey(), SystemUtils.JAVA_VERSION_TRIMMED), e);

    } catch (Exception e) {
      throw new SonarException(String.format("Fail to load plugin %s", plugin.getKey()), e);
    }
  }

  private boolean isResource(File file) {
    return !StringUtils.endsWithIgnoreCase(file.getName(), ".jar") && !file.isDirectory();
  }

  public void clean() {
    for (ClassRealm realm : world.getRealms()) {
      try {
        world.disposeRealm(realm.getId());
      } catch (Exception e) {
        // Ignore
      }
    }
    world = null;
    baseClassloader=null;
  }
}
