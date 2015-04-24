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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;
import org.sonar.updatecenter.common.PluginManifest;
import org.sonar.updatecenter.common.Version;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PluginInfo implements Comparable<PluginInfo> {

  public static class RequiredPlugin {
    private final String key;
    private final Version minimalVersion;

    public RequiredPlugin(String key, Version minimalVersion) {
      this.key = key;
      this.minimalVersion = minimalVersion;
    }

    public String getKey() {
      return key;
    }

    public Version getMinimalVersion() {
      return minimalVersion;
    }

    public static RequiredPlugin parse(String s) {
      if (!s.matches("\\w+:.+")) {
        throw new IllegalArgumentException("Manifest field does not have correct format: " + s);
      }
      String[] fields = StringUtils.split(s, ':');
      return new RequiredPlugin(fields[0], Version.create(fields[1]).removeQualifier());
    }
  }

  private File file;
  private String key;
  private String name;
  private Version version;
  private Version minimalSqVersion;
  private String mainClass;
  private String description;
  private String organizationName;
  private String organizationUrl;
  private String license;
  private String homepageUrl;
  private String issueTrackerUrl;
  private boolean useChildFirstClassLoader;
  private String basePlugin;
  private boolean core;
  private String implementationBuild;
  private final List<RequiredPlugin> requiredPlugins = new ArrayList<>();

  public PluginInfo() {
  }

  /**
   * For tests only
   */
  public PluginInfo(String key) {
    this.key = key;
  }

  public File getFile() {
    return file;
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  @CheckForNull
  public Version getVersion() {
    return version;
  }

  @CheckForNull
  public Version getMinimalSqVersion() {
    return minimalSqVersion;
  }

  public String getMainClass() {
    return mainClass;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  @CheckForNull
  public String getOrganizationName() {
    return organizationName;
  }

  @CheckForNull
  public String getOrganizationUrl() {
    return organizationUrl;
  }

  @CheckForNull
  public String getLicense() {
    return license;
  }

  @CheckForNull
  public String getHomepageUrl() {
    return homepageUrl;
  }

  @CheckForNull
  public String getIssueTrackerUrl() {
    return issueTrackerUrl;
  }

  public boolean isUseChildFirstClassLoader() {
    return useChildFirstClassLoader;
  }

  @CheckForNull
  public String getBasePlugin() {
    return basePlugin;
  }

  public boolean isCore() {
    return core;
  }

  @CheckForNull
  public String getImplementationBuild() {
    return implementationBuild;
  }

  public List<RequiredPlugin> getRequiredPlugins() {
    return requiredPlugins;
  }

  /**
   * Required
   */
  public PluginInfo setFile(File file) {
    this.file = file;
    return this;
  }

  /**
   * Required
   */
  public PluginInfo setKey(String key) {
    this.key = key;
    return this;
  }

  /**
   * Required
   */
  public PluginInfo setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Required
   */
  public PluginInfo setVersion(Version version) {
    this.version = version;
    return this;
  }

  public PluginInfo setMinimalSqVersion(@Nullable Version v) {
    this.minimalSqVersion = v;
    return this;
  }

  /**
   * Required
   */
  public PluginInfo setMainClass(String mainClass) {
    this.mainClass = mainClass;
    return this;
  }

  public PluginInfo setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  public PluginInfo setOrganizationName(@Nullable String s) {
    this.organizationName = s;
    return this;
  }

  public PluginInfo setOrganizationUrl(@Nullable String s) {
    this.organizationUrl = s;
    return this;
  }

  public PluginInfo setLicense(@Nullable String license) {
    this.license = license;
    return this;
  }

  public PluginInfo setHomepageUrl(@Nullable String s) {
    this.homepageUrl = s;
    return this;
  }

  public PluginInfo setIssueTrackerUrl(@Nullable String s) {
    this.issueTrackerUrl = s;
    return this;
  }

  public PluginInfo setUseChildFirstClassLoader(boolean b) {
    this.useChildFirstClassLoader = b;
    return this;
  }

  public PluginInfo setBasePlugin(@Nullable String s) {
    this.basePlugin = s;
    return this;
  }

  public PluginInfo setCore(boolean b) {
    this.core = b;
    return this;
  }

  public PluginInfo setImplementationBuild(@Nullable String implementationBuild) {
    this.implementationBuild = implementationBuild;
    return this;
  }

  public PluginInfo addRequiredPlugin(RequiredPlugin p) {
    this.requiredPlugins.add(p);
    return this;
  }

  /**
   * Find out if this plugin is compatible with a given version of SonarQube.
   * The version of SQ must be greater than or equal to the minimal version
   * needed by the plugin.
   */
  public boolean isCompatibleWith(String sqVersion) {
    if (null == this.minimalSqVersion) {
      // no constraint defined on the plugin
      return true;
    }

    Version effectiveMin = Version.create(minimalSqVersion.getName()).removeQualifier();
    Version actualVersion = Version.create(sqVersion).removeQualifier();
    return actualVersion.compareTo(effectiveMin) >= 0;
  }

  @Override
  public String toString() {
    return String.format("[%s]", Joiner.on(" / ").skipNulls().join(key, version, implementationBuild));
  }

  @Override
  public int compareTo(PluginInfo other) {
    int cmp = name.compareTo(other.name);
    if (cmp != 0) {
      return cmp;
    }
    return version.compareTo(other.version);
  }

  public static PluginInfo create(File jarFile) {
    try {
      PluginManifest manifest = new PluginManifest(jarFile);
      return create(jarFile, manifest);

    } catch (Exception e) {
      throw new IllegalStateException("Fail to extract plugin metadata from file: " + jarFile, e);
    }
  }

  @VisibleForTesting
  static PluginInfo create(File jarFile, PluginManifest manifest) {
    PluginInfo info = new PluginInfo();

    // required fields
    info.setKey(manifest.getKey());
    info.setFile(jarFile);
    info.setName(manifest.getName());
    info.setMainClass(manifest.getMainClass());
    info.setVersion(Version.create(manifest.getVersion()));

    // optional fields
    info.setDescription(manifest.getDescription());
    info.setLicense(manifest.getLicense());
    info.setOrganizationName(manifest.getOrganization());
    info.setOrganizationUrl(manifest.getOrganizationUrl());
    String minSqVersion = manifest.getSonarVersion();
    if (minSqVersion != null) {
      info.setMinimalSqVersion(Version.create(minSqVersion));
    }
    info.setHomepageUrl(manifest.getHomepage());
    info.setIssueTrackerUrl(manifest.getIssueTrackerUrl());
    info.setUseChildFirstClassLoader(manifest.isUseChildFirstClassLoader());
    info.setBasePlugin(manifest.getBasePlugin());
    info.setImplementationBuild(manifest.getImplementationBuild());
    String[] requiredPlugins = manifest.getRequirePlugins();
    if (requiredPlugins != null) {
      for (String s : requiredPlugins) {
        info.addRequiredPlugin(RequiredPlugin.parse(s));
      }
    }
    return info;
  }

  public enum JarToPluginInfo implements Function<File, PluginInfo> {
    INSTANCE;

    @Override
    public PluginInfo apply(@Nonnull File jarFile) {
      return create(jarFile);
    }
  };
}
