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

public final class Plugin extends Artifact {

  private String name;
  private String description;
  private String homepageUrl;
  private String license;
  private String organization;
  private String organizationUrl;
  private String termsConditionsUrl;
  private String category;
  private String issueTrackerUrl;
  
  public Plugin(String key) {
    super(key);
  }

  public String getName() {
    return name;
  }

  public Plugin setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Plugin setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getHomepageUrl() {
    return homepageUrl;
  }

  public Plugin setHomepageUrl(String s) {
    this.homepageUrl = s;
    return this;
  }

  public String getLicense() {
    return license;
  }

  public Plugin setLicense(String license) {
    this.license = license;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  public Plugin setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganizationUrl() {
    return organizationUrl;
  }

  public Plugin setOrganizationUrl(String url) {
    this.organizationUrl = url;
    return this;
  }

  public String getCategory() {
    return category;
  }

  public Plugin setCategory(String category) {
    this.category = category;
    return this;
  }

  public String getTermsConditionsUrl() {
    return termsConditionsUrl;
  }

  public Plugin setTermsConditionsUrl(String url) {
    this.termsConditionsUrl = url;
    return this;
  }

  public String getIssueTrackerUrl() {
    return issueTrackerUrl;
  }

  public Plugin setIssueTrackerUrl(String url) {
    this.issueTrackerUrl = url;
    return this;
  }

  public Plugin merge(PluginManifest manifest) {
    if (StringUtils.equals(key, manifest.getKey())) {
      name = manifest.getName();
      description = manifest.getDescription();
      organization = manifest.getOrganization();
      organizationUrl = manifest.getOrganizationUrl();
      issueTrackerUrl = manifest.getIssueTrackerUrl();
      license = manifest.getLicense();
      homepageUrl = manifest.getHomepage();
      termsConditionsUrl = manifest.getTermsConditionsUrl();
    }
    return this;
  }
}
