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
package org.sonar.api.batch.maven;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A class to handle maven plugins
 *
 * @since 1.10
 * @deprecated since 4.5 we don't want any dependency on Maven anymore
 */
@Deprecated
public class MavenPlugin {

  private static final String CONFIGURATION_ELEMENT = "configuration";
  private Plugin plugin;
  private Xpp3Dom configuration;

  /**
   * Creates a MavenPlugin based on a Plugin
   *
   * @param plugin the plugin
   */
  public MavenPlugin(Plugin plugin) {
    this.plugin = plugin;
    this.configuration = (Xpp3Dom) plugin.getConfiguration();
    if (this.configuration == null) {
      configuration = new Xpp3Dom(CONFIGURATION_ELEMENT);
      plugin.setConfiguration(this.configuration);
    }
  }

  /**
   * Creates a Maven plugin based on artifact + group + version
   *
   * @param groupId the group id
   * @param artifactId the artifact id
   * @param version the version
   */
  public MavenPlugin(String groupId, String artifactId, String version) {
    this.plugin = new Plugin();
    plugin.setGroupId(groupId);
    plugin.setArtifactId(artifactId);
    plugin.setVersion(version);
    configuration = new Xpp3Dom(CONFIGURATION_ELEMENT);
    plugin.setConfiguration(this.configuration);
  }

  /**
   * @since 3.5 - see SONAR-4070
   * @return the XML node <configuration> of pom
   */
  public Xpp3Dom getConfigurationXmlNode() {
    return configuration;
  }

  /**
   * Sets the maven plugin version
   *
   * @param version the version
   * @return this
   */
  public MavenPlugin setVersion(String version) {
    this.plugin.setVersion(version);
    return this;
  }

  /**
   * @return the underlying plugin
   */
  public Plugin getPlugin() {
    return plugin;
  }

  /**
   * Gets a parameter of the plugin based on its key
   *
   * @param key the param key
   * @return the parameter if exist, null otherwise
   */
  public String getParameter(String key) {
    Xpp3Dom node = findNodeWith(key);
    return node == null ? null : node.getValue();
  }

  /**
   * Gets a list of parameters of the plugin from a param key
   *
   * @param key param key with option-index snippet: e.g. item[0], item[1]. If no index snippet is passed, then
   *            0 is default (index <=> index[0])
   * @return an array of parameters if any, an empty array otherwise
   */
  public String[] getParameters(String key) {
    String[] keyParts = StringUtils.split(key, "/");
    Xpp3Dom node = configuration;
    for (int i = 0; i < keyParts.length - 1; i++) {
      node = getOrCreateChild(node, keyParts[i]);
    }
    Xpp3Dom[] children = node.getChildren(keyParts[keyParts.length - 1]);
    String[] result = new String[children.length];
    for (int i = 0; i < children.length; i++) {
      result[i] = children[i].getValue();
    }
    return result;
  }

  /**
   * Sets a parameter for the maven plugin. This will overrides an existing parameter.
   *
   * @param key the param key
   * @param value the param value
   * @return this
   */
  public MavenPlugin setParameter(String key, String value) {
    checkKeyArgument(key);
    String[] keyParts = StringUtils.split(key, "/");
    Xpp3Dom node = configuration;
    for (String keyPart : keyParts) {
      node = getOrCreateChild(node, keyPart);
    }
    node.setValue(value);
    return this;
  }

  /**
   * Sets a parameter to the maven plugin. Overrides existing parameter only id specified.
   *
   * @param key the param key
   * @param value the param value
   * @param override whether to override existing parameter
   */
  public void setParameter(String key, String value, boolean override) {
    if (getParameter(key) == null || override) {
      setParameter(key, value);
    }
  }

  /**
   * Removes all parameters from the maven plugin
   */
  public void removeParameters() {
    configuration = new Xpp3Dom(CONFIGURATION_ELEMENT);
    plugin.setConfiguration(this.configuration);
  }

  /**
   * Adds a parameter to the maven plugin
   *
   * @param key the param key with option-index snippet: e.g. item[0], item[1]. If no index snippet is passed, then
   *            0 is default (index <=> index[0])
   * @param value the param value
   * @return this
   */
  public MavenPlugin addParameter(String key, String value) {
    String[] keyParts = StringUtils.split(key, "/");
    Xpp3Dom node = configuration;
    for (int i = 0; i < keyParts.length - 1; i++) {
      node = getOrCreateChild(node, keyParts[i]);
    }
    Xpp3Dom leaf = new Xpp3Dom(keyParts[keyParts.length - 1]);
    leaf.setValue(value);
    node.addChild(leaf);
    return this;
  }

  private static Xpp3Dom getOrCreateChild(Xpp3Dom node, String key) {
    int childIndex = getIndex(key);

    if (node.getChildren(removeIndexSnippet(key)).length <= childIndex) {
      Xpp3Dom child = new Xpp3Dom(removeIndexSnippet(key));
      node.addChild(child);
      return child;
    }
    return node.getChildren(removeIndexSnippet(key))[childIndex];

  }

  private static int getIndex(String key) {
    // parsing index-syntax (e.g. item[1])
    if (key.matches(".*?\\[\\d+\\]")) {
      return Integer.parseInt(StringUtils.substringBetween(key, "[", "]"));
    }
    // for down-compatibility of api we fallback to default 0
    return 0;
  }

  private static String removeIndexSnippet(String key) {
    return StringUtils.substringBefore(key, "[");
  }

  /**
   * Remove a parameter from the maven plugin based on its key
   *
   * @param key param key with option-index snippet: e.g. item[0], item[1]. If no index snippet is passed, then
   *            0 is default (index <=> index[0])
   */
  public void removeParameter(String key) {
    Xpp3Dom node = findNodeWith(key);
    if (node != null) {
      remove(node);
    }
  }

  private Xpp3Dom findNodeWith(String key) {
    checkKeyArgument(key);
    String[] keyParts = key.split("/");
    Xpp3Dom node = configuration;
    for (String keyPart : keyParts) {

      if (node.getChildren(removeIndexSnippet(keyPart)).length <= getIndex(keyPart)) {
        return null;
      }

      node = node.getChildren(removeIndexSnippet(keyPart))[getIndex(keyPart)];
      if (node == null) {
        return null;
      }
    }
    return node;
  }

  private static void remove(Xpp3Dom node) {
    Xpp3Dom parent = node.getParent();
    for (int i = 0; i < parent.getChildCount(); i++) {
      Xpp3Dom child = parent.getChild(i);
      if (child.equals(node)) {
        parent.removeChild(i);
        break;
      }
    }
  }

  /**
   * @return whether the maven plugin has got configuration
   */
  public boolean hasConfiguration() {
    return configuration.getChildCount() > 0;
  }

  private static void checkKeyArgument(String key) {
    if (key == null) {
      throw new IllegalArgumentException("Parameter 'key' should not be null.");
    }
  }

  /**
   * Registers a plugin in a project pom
   * <p/>
   * <p>Adds the plugin if it does not exist or amend its version if it does exist and specified</p>
   *
   * @param pom the project pom
   * @param groupId the plugin group id
   * @param artifactId the plugin artifact id
   * @param version the plugin version
   * @param overrideVersion whether to override the version if the plugin is already registered
   * @return the registered plugin
   */
  public static MavenPlugin registerPlugin(MavenProject pom, String groupId, String artifactId, String version, boolean overrideVersion) {
    MavenPlugin plugin = getPlugin(pom, groupId, artifactId);
    if (plugin == null) {
      plugin = new MavenPlugin(groupId, artifactId, version);

    } else if (overrideVersion) {
      plugin.setVersion(version);
    }

    // remove from pom
    unregisterPlugin(pom, groupId, artifactId);

    // register
    pom.getBuild().addPlugin(plugin.getPlugin());

    return plugin;
  }

  /**
   * Returns a plugin from a pom based on its group id and artifact id
   * <p/>
   * <p>It searches in the build section, then the reporting section and finally the pluginManagement section</p>
   *
   * @param pom the project pom
   * @param groupId the plugin group id
   * @param artifactId the plugin artifact id
   * @return the plugin if it exists, null otherwise
   */
  public static MavenPlugin getPlugin(MavenProject pom, String groupId, String artifactId) {
    if (pom == null) {
      return null;
    }
    // look for plugin in <build> section
    Plugin plugin = null;
    if (pom.getBuildPlugins() != null) {
      plugin = getPlugin(pom.getBuildPlugins(), groupId, artifactId);
    }

    // look for plugin in <report> section
    if (plugin == null && pom.getReportPlugins() != null) {
      plugin = getReportPlugin(pom.getReportPlugins(), groupId, artifactId);
    }

    // look for plugin in <pluginManagement> section
    if (pom.getPluginManagement() != null) {
      Plugin pluginManagement = getPlugin(pom.getPluginManagement().getPlugins(), groupId, artifactId);
      if (plugin == null) {
        plugin = pluginManagement;

      } else if (pluginManagement != null) {
        if (pluginManagement.getConfiguration() != null) {
          if (plugin.getConfiguration() == null) {
            plugin.setConfiguration(pluginManagement.getConfiguration());
          } else {
            Xpp3Dom.mergeXpp3Dom((Xpp3Dom) plugin.getConfiguration(), (Xpp3Dom) pluginManagement.getConfiguration());
          }
        }
        if (plugin.getDependencies() == null && pluginManagement.getDependencies() != null) {
          plugin.setDependencies(pluginManagement.getDependencies());
        }
        if (plugin.getVersion() == null) {
          plugin.setVersion(pluginManagement.getVersion());
        }
      }
    }

    if (plugin != null) {
      return new MavenPlugin(plugin);
    }
    return null;
  }

  private static Plugin getPlugin(Collection<Plugin> plugins, String groupId, String artifactId) {
    if (plugins == null) {
      return null;
    }

    for (Plugin plugin : plugins) {
      if (MavenUtils.equals(plugin, groupId, artifactId)) {
        return plugin;
      }
    }
    return null;
  }

  private static Plugin getReportPlugin(Collection<ReportPlugin> plugins, String groupId, String artifactId) {
    if (plugins == null) {
      return null;
    }

    for (ReportPlugin plugin : plugins) {
      if (MavenUtils.equals(plugin, groupId, artifactId)) {
        return cloneReportPluginToPlugin(plugin);
      }
    }
    return null;
  }

  private static Plugin cloneReportPluginToPlugin(ReportPlugin reportPlugin) {
    Plugin plugin = new Plugin();
    plugin.setGroupId(reportPlugin.getGroupId());
    plugin.setArtifactId(reportPlugin.getArtifactId());
    plugin.setVersion(reportPlugin.getVersion());
    plugin.setConfiguration(reportPlugin.getConfiguration());
    return plugin;
  }

  private static void unregisterPlugin(MavenProject pom, String groupId, String artifactId) {
    if (pom.getPluginManagement() != null && pom.getPluginManagement().getPlugins() != null) {
      unregisterPlugin(pom.getPluginManagement().getPlugins(), groupId, artifactId);
    }
    List plugins = pom.getBuildPlugins();
    if (plugins != null) {
      unregisterPlugin(plugins, groupId, artifactId);
    }
    plugins = pom.getReportPlugins();
    if (plugins != null) {
      unregisterReportPlugin(plugins, groupId, artifactId);
    }
  }

  private static void unregisterPlugin(List<Plugin> plugins, String groupId, String artifactId) {
    for (Iterator<Plugin> iterator = plugins.iterator(); iterator.hasNext();) {
      Plugin p = iterator.next();
      if (MavenUtils.equals(p, groupId, artifactId)) {
        iterator.remove();
      }
    }
  }

  private static void unregisterReportPlugin(List<ReportPlugin> plugins, String groupId, String artifactId) {
    for (Iterator<ReportPlugin> iterator = plugins.iterator(); iterator.hasNext();) {
      ReportPlugin p = iterator.next();
      if (MavenUtils.equals(p, groupId, artifactId)) {
        iterator.remove();
      }
    }
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("groupId", plugin.getGroupId())
      .append("artifactId", plugin.getArtifactId())
      .append("version", plugin.getVersion())
      .toString();
  }
}
