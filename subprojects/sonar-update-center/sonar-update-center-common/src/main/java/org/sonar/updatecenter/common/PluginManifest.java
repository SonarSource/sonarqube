/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.updatecenter.common;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.sonar.updatecenter.common.FormatUtils.toDate;

/**
 * This class loads Sonar plugin metadata from JAR manifest
 *
 * @since 2.2
 */
public final class PluginManifest {

  public static final String KEY = "Plugin-Key";
  public static final String MAIN_CLASS = "Plugin-Class";
  public static final String NAME = "Plugin-Name";
  public static final String DESCRIPTION = "Plugin-Description";
  public static final String ORGANIZATION = "Plugin-Organization";
  public static final String ORGANIZATION_URL = "Plugin-OrganizationUrl";
  public static final String LICENSE = "Plugin-License";
  public static final String VERSION = "Plugin-Version";
  public static final String SONAR_VERSION = "Sonar-Version";
  public static final String DEPENDENCIES = "Plugin-Dependencies";
  public static final String HOMEPAGE = "Plugin-Homepage";
  public static final String TERMS_CONDITIONS_URL = "Plugin-TermsConditionsUrl";
  public static final String BUILD_DATE = "Build-Date";
  public static final String ISSUE_TRACKER_URL = "Plugin-IssueTrackerUrl";

  private String key;
  private String name;
  private String mainClass;
  private String description;
  private String organization;
  private String organizationUrl;
  private String license;
  private String version;
  private String sonarVersion;
  private String[] dependencies = new String[0];
  private String homepage;
  private String termsConditionsUrl;
  private Date buildDate;
  private String issueTrackerUrl;
  

  /**
   * Load the manifest from a JAR file.
   */
  public PluginManifest(File file) throws IOException {
    JarFile jar = new JarFile(file);
    try {
      if (jar.getManifest() != null) {
        loadManifest(jar.getManifest());
      }

    } finally {
      jar.close();
    }
  }

  /**
   * @param manifest, can not be null
   */
  public PluginManifest(Manifest manifest) {
    loadManifest(manifest);
  }

  public PluginManifest() {
  }

  private void loadManifest(Manifest manifest) {
    Attributes attributes = manifest.getMainAttributes();
    this.key = attributes.getValue(KEY);
    this.mainClass = attributes.getValue(MAIN_CLASS);
    this.name = attributes.getValue(NAME);
    this.description = attributes.getValue(DESCRIPTION);
    this.license = attributes.getValue(LICENSE);
    this.organization = attributes.getValue(ORGANIZATION);
    this.organizationUrl = attributes.getValue(ORGANIZATION_URL);
    this.version = attributes.getValue(VERSION);
    this.homepage = attributes.getValue(HOMEPAGE);
    this.termsConditionsUrl = attributes.getValue(TERMS_CONDITIONS_URL);
    this.sonarVersion = attributes.getValue(SONAR_VERSION);
    this.issueTrackerUrl = attributes.getValue(ISSUE_TRACKER_URL);
    this.buildDate = toDate(attributes.getValue(BUILD_DATE), true);

    String deps = attributes.getValue(DEPENDENCIES);
    this.dependencies = StringUtils.split(StringUtils.defaultString(deps), ' ');
  }

  public String getKey() {
    return key;
  }

  public PluginManifest setKey(String key) {
    this.key = key;
    return this;
  }

  public String getName() {
    return name;
  }

  public PluginManifest setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public PluginManifest setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  public PluginManifest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganizationUrl() {
    return organizationUrl;
  }

  public PluginManifest setOrganizationUrl(String url) {
    this.organizationUrl = url;
    return this;
  }

  public String getLicense() {
    return license;
  }

  public PluginManifest setLicense(String license) {
    this.license = license;
    return this;
  }

  public String getVersion() {
    return version;
  }

  public PluginManifest setVersion(String version) {
    this.version = version;
    return this;
  }

  public String getSonarVersion() {
    return sonarVersion;
  }

  public PluginManifest setSonarVersion(String sonarVersion) {
    this.sonarVersion = sonarVersion;
    return this;
  }

  public String getMainClass() {
    return mainClass;
  }

  public PluginManifest setMainClass(String mainClass) {
    this.mainClass = mainClass;
    return this;
  }

  public String[] getDependencies() {
    return dependencies;
  }

  public PluginManifest setDependencies(String[] dependencies) {
    this.dependencies = dependencies;
    return this;
  }

  public Date getBuildDate() {
    return buildDate;
  }

  public PluginManifest setBuildDate(Date buildDate) {
    this.buildDate = buildDate;
    return this;
  }

  public String getHomepage() {
    return homepage;
  }

  public PluginManifest setHomepage(String homepage) {
    this.homepage = homepage;
    return this;
  }

  public String getTermsConditionsUrl() {
    return termsConditionsUrl;
  }

  public PluginManifest setTermsConditionsUrl(String termsConditionsUrl) {
    this.termsConditionsUrl = termsConditionsUrl;
    return this;
  }

  public String getIssueTrackerUrl() {
    return issueTrackerUrl;
  }

  public PluginManifest setIssueTrackerUrl(String issueTrackerUrl) {
    this.issueTrackerUrl = issueTrackerUrl;
    return this;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this).toString();
  }

  public boolean isValid() {
    return StringUtils.isNotBlank(key) && StringUtils.isNotBlank(version);
  }
}
