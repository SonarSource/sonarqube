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
package org.sonar.core.plugin;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.annotations.Cascade;
import org.sonar.api.database.BaseIdentifiable;

import javax.persistence.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Installed plugins
 *
 * @since 2.2
 */
@Entity
@Table(name = "plugins")
public class JpaPlugin extends BaseIdentifiable {

  @Column(name = "plugin_key", updatable = true, nullable = false, length = 100)
  private String key;

  @Column(name = "version", updatable = true, nullable = true, length = 100)
  private String version;

  @Column(name = "name", updatable = true, nullable = true, length = 100)
  private String name;

  @Column(name = "description", updatable = true, nullable = true, length = 3000)
  private String description;

  @Column(name = "organization", updatable = true, nullable = true, length = 100)
  private String organization;

  @Column(name = "organization_url", updatable = true, nullable = true, length = 500)
  private String organizationUrl;

  @Column(name = "license", updatable = true, nullable = true, length = 50)
  private String license;

  @Column(name = "installation_date", updatable = true, nullable = true)
  private Date installationDate;

  @Column(name = "plugin_class", updatable = true, nullable = true, length = 100)
  private String pluginClass;

  @Column(name = "homepage", updatable = true, nullable = true, length = 500)
  private String homepage;

  @Column(name = "core", updatable = true, nullable = true)
  private Boolean core;

  @Cascade({org.hibernate.annotations.CascadeType.SAVE_UPDATE,
            org.hibernate.annotations.CascadeType.DELETE,
            org.hibernate.annotations.CascadeType.MERGE,
            org.hibernate.annotations.CascadeType.PERSIST,
            org.hibernate.annotations.CascadeType.DELETE_ORPHAN})
  @OneToMany(mappedBy = "plugin", cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
  private List<JpaPluginFile> files = new ArrayList<JpaPluginFile>();

  public JpaPlugin() {
  }

  public JpaPlugin(String pluginKey) {
    if (StringUtils.isBlank(pluginKey)) {
      throw new IllegalArgumentException("LocalExtension.pluginKey can not be blank");
    }
    this.key = pluginKey;
  }

  public String getKey() {
    return key;
  }

  public JpaPlugin setKey(String s) {
    this.key = s;
    return this;
  }

  public String getName() {
    return name;
  }

  public JpaPlugin setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public JpaPlugin setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  public JpaPlugin setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganizationUrl() {
    return organizationUrl;
  }

  public JpaPlugin setOrganizationUrl(URI uri) {
    this.organizationUrl = (uri != null ? uri.toString() : null);
    return this;
  }

  public JpaPlugin setOrganizationUrl(String s) {
    this.organizationUrl = s;
    return this;
  }

  public String getLicense() {
    return license;
  }

  public JpaPlugin setLicense(String license) {
    this.license = license;
    return this;
  }

  public String getVersion() {
    return version;
  }

  public JpaPlugin setVersion(String s) {
    this.version = s;
    return this;
  }

  public Date getInstallationDate() {
    return installationDate;
  }

  public JpaPlugin setInstallationDate(Date installationDate) {
    this.installationDate = installationDate;
    return this;
  }

  public String getPluginClass() {
    return pluginClass;
  }

  public JpaPlugin setPluginClass(String s) {
    this.pluginClass = s;
    return this;
  }

  public String getHomepage() {
    return homepage;
  }

  public JpaPlugin setHomepage(URI uri) {
    this.homepage = (uri != null ? uri.toString() : null);
    return this;
  }

  public JpaPlugin setHomepage(String s) {
    this.homepage = s;
    return this;
  }

  public Boolean isCore() {
    return core;
  }

  public JpaPlugin setCore(Boolean b) {
    this.core = b;
    return this;
  }

  public void createFile(String filename) {
    JpaPluginFile file = new JpaPluginFile(this, filename);
    this.files.add(file);
  }

  public List<JpaPluginFile> getFiles() {
    return files;
  }

  public void removeFiles() {
    files.clear();
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JpaPlugin other = (JpaPlugin) o;
    return key.equals(other.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("id", getId())
        .append("key", key)
        .append("version", version)
        .append("homepage", homepage)
        .append("installationDate", installationDate)
        .toString();
  }


  public static JpaPlugin create(String pluginKey) {
    return new JpaPlugin(pluginKey);
  }

}
