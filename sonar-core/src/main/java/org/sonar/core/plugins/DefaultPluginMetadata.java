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

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.updatecenter.common.Version;

import java.io.File;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class DefaultPluginMetadata implements PluginMetadata, Comparable<PluginMetadata> {
  private File file;
  private List<File> deployedFiles;
  private List<String> pathsToInternalDeps;
  private String key;
  private String version;
  private String sonarVersion;
  private String name;
  private String mainClass;
  private String description;
  private String organization;
  private String organizationUrl;
  private String license;
  private String homepage;
  private String issueTrackerUrl;
  private boolean useChildFirstClassLoader;
  private String basePlugin;
  private boolean core;
  private String implementationBuild;
  private List<String> requiredPlugins;

  private DefaultPluginMetadata() {
    deployedFiles = newArrayList();
    pathsToInternalDeps = newArrayList();
    requiredPlugins = newArrayList();
  }

  public static DefaultPluginMetadata create(File file) {
    return new DefaultPluginMetadata().setFile(file);
  }

  public static DefaultPluginMetadata create(String key) {
    return new DefaultPluginMetadata().setKey(key);
  }

  @Override
  public File getFile() {
    return file;
  }

  public DefaultPluginMetadata setFile(File file) {
    this.file = file;
    return this;
  }

  @Override
  public List<File> getDeployedFiles() {
    return deployedFiles;
  }

  public DefaultPluginMetadata addDeployedFile(File f) {
    this.deployedFiles.add(f);
    return this;
  }

  public List<String> getPathsToInternalDeps() {
    return ImmutableList.copyOf(pathsToInternalDeps);
  }

  public DefaultPluginMetadata setPathsToInternalDeps(List<String> pathsToInternalDeps) {
    this.pathsToInternalDeps = ImmutableList.copyOf(pathsToInternalDeps);
    return this;
  }

  @Override
  public String getKey() {
    return key;
  }

  public DefaultPluginMetadata setKey(String key) {
    this.key = key;
    return this;
  }

  @Override
  public String getName() {
    return name;
  }

  public DefaultPluginMetadata setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public String getMainClass() {
    return mainClass;
  }

  public DefaultPluginMetadata setMainClass(String mainClass) {
    this.mainClass = mainClass;
    return this;
  }

  @Override
  public String getDescription() {
    return description;
  }

  public DefaultPluginMetadata setDescription(String description) {
    this.description = description;
    return this;
  }

  @Override
  public String getOrganization() {
    return organization;
  }

  public DefaultPluginMetadata setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  @Override
  public String getOrganizationUrl() {
    return organizationUrl;
  }

  public DefaultPluginMetadata setOrganizationUrl(String organizationUrl) {
    this.organizationUrl = organizationUrl;
    return this;
  }

  @Override
  public String getLicense() {
    return license;
  }

  public DefaultPluginMetadata setLicense(String license) {
    this.license = license;
    return this;
  }

  @Override
  public String getVersion() {
    return version;
  }

  public DefaultPluginMetadata setVersion(String version) {
    this.version = version;
    return this;
  }

  public String getSonarVersion() {
    return sonarVersion;
  }

  public DefaultPluginMetadata setSonarVersion(String sonarVersion) {
    this.sonarVersion = sonarVersion;
    return this;
  }

  @Override
  public List<String> getRequiredPlugins() {
    return ImmutableList.copyOf(requiredPlugins);
  }

  public DefaultPluginMetadata setRequiredPlugins(List<String> requiredPlugins) {
    this.requiredPlugins = ImmutableList.copyOf(requiredPlugins);
    return this;
  }

  /**
   * Find out if this plugin is compatible with a given version of Sonar.
   * The version of sonar must be greater than or equal to the minimal version
   * needed by the plugin.
   *
   * @param sonarVersion
   * @return <code>true</code> if the plugin is compatible
   */
  public boolean isCompatibleWith(String sonarVersion) {
    if (null == this.sonarVersion) {
      // Plugins without sonar version are so old, they are compatible with a version containing this code
      return true;
    }

    Version minimumVersion = Version.create(this.sonarVersion).removeQualifier();
    Version actualVersion = Version.create(sonarVersion).removeQualifier();
    return actualVersion.compareTo(minimumVersion) >= 0;
  }

  @Override
  public String getHomepage() {
    return homepage;
  }

  public DefaultPluginMetadata setHomepage(String homepage) {
    this.homepage = homepage;
    return this;
  }

  @Override
  public String getIssueTrackerUrl() {
    return issueTrackerUrl;
  }

  public DefaultPluginMetadata setIssueTrackerUrl(String issueTrackerUrl) {
    this.issueTrackerUrl = issueTrackerUrl;
    return this;
  }

  public DefaultPluginMetadata setUseChildFirstClassLoader(boolean use) {
    this.useChildFirstClassLoader = use;
    return this;
  }

  @Override
  public boolean isUseChildFirstClassLoader() {
    return useChildFirstClassLoader;
  }

  public DefaultPluginMetadata setBasePlugin(String key) {
    this.basePlugin = key;
    return this;
  }

  @Override
  public String getBasePlugin() {
    return basePlugin;
  }

  @Override
  public boolean isCore() {
    return core;
  }

  public DefaultPluginMetadata setCore(boolean b) {
    this.core = b;
    return this;
  }

  @Override
  public String getImplementationBuild() {
    return implementationBuild;
  }

  public DefaultPluginMetadata setImplementationBuild(String implementationBuild) {
    this.implementationBuild = implementationBuild;
    return this;
  }

  @Override
  public String getParent() {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultPluginMetadata that = (DefaultPluginMetadata) o;
    return key == null ? that.key == null : key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return key != null ? key.hashCode() : 0;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
      .append("key", key)
      .append("version", StringUtils.defaultIfEmpty(version, "-"))
      .toString();
  }

  @Override
  public int compareTo(PluginMetadata other) {
    return name.compareTo(other.getName());
  }
}
