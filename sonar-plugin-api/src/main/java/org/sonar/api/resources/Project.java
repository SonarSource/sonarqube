/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.api.resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.component.Component;
import org.sonar.api.config.Settings;

/**
 * @since 1.10
 * @deprecated since 5.6 replaced by {@link InputModule}.
 */
@Deprecated
public class Project extends Resource implements Component {

  /**
   * Internal use
   */
  public static final Language NONE_LANGUAGE = new AbstractLanguage("none", "None") {
    @Override
    public String[] getFileSuffixes() {
      return new String[0];
    }
  };

  static final String MAVEN_KEY_FORMAT = "%s:%s";
  private static final String BRANCH_KEY_FORMAT = "%s:%s";

  public static final String SCOPE = Scopes.PROJECT;

  private String branch;
  private String name;
  private String description;
  private Date analysisDate;
  private String analysisVersion;
  private Settings settings;
  private String originalName;

  // For internal use
  private java.io.File baseDir;

  // modules tree
  private Project parent;
  private List<Project> modules = new ArrayList<>();

  public Project(String key) {
    setKey(key);
    setEffectiveKey(key);
  }

  public Project(String key, String branch, String name) {
    if (StringUtils.isNotBlank(branch)) {
      setKey(String.format(BRANCH_KEY_FORMAT, key, branch));
      this.name = String.format("%s %s", name, branch);
    } else {
      setKey(key);
      this.name = name;
    }
    this.originalName = this.name;
    setEffectiveKey(getKey());
    this.branch = branch;
  }

  public String getBranch() {
    return branch;
  }

  /**
   * For internal use only.
   */
  public Project setBranch(String branch) {
    this.branch = branch;
    return this;
  }

  @CheckForNull
  public String getOriginalName() {
    return originalName;
  }

  public void setOriginalName(String originalName) {
    if (StringUtils.isNotBlank(branch)) {
      this.originalName = String.format("%s %s", originalName, branch);
    } else {
      this.originalName = originalName;
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getLongName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  /**
   * For internal use only.
   */
  public Project setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public Language getLanguage() {
    return null;
  }

  /**
   * For internal use only.
   */
  public Project setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * @return whether the current project is root project
   */
  public boolean isRoot() {
    return getParent() == null;
  }

  public Project getRoot() {
    return parent == null ? this : parent.getRoot();
  }

  /**
   * @return whether the current project is a module
   */
  public boolean isModule() {
    return !isRoot();
  }

  /**
   * For internal use only.
   *
   * @deprecated in 3.6. It's not possible to analyze a project before the latest known quality snapshot.
   * See http://jira.sonarsource.com/browse/SONAR-4334
   */
  @Deprecated
  public Project setLatestAnalysis(boolean b) {
    if (!b) {
      throw new UnsupportedOperationException("The analysis is always the latest one. " +
        "Past analysis must be done in a chronological order.");
    }
    return this;
  }

  /**
     * @return the language key or empty if no language is specified
     * @deprecated since 4.2 use {@link org.sonar.api.batch.fs.FileSystem#languages()}
     */
  @Deprecated
  public String getLanguageKey() {
    if (settings == null) {
      throw new IllegalStateException("Project is not yet initialized");
    }
    return StringUtils.defaultIfEmpty(settings.getString(CoreProperties.PROJECT_LANGUAGE_PROPERTY), "");
  }

  /**
   * Internal use
   */
  public Project setSettings(Settings settings) {
    this.settings = settings;
    return this;
  }

  /**
   * Internal use for backward compatibility. Settings should be retrieved as an IoC dependency.
   * @deprecated since 5.0
   */
  @Deprecated
  public Settings getSettings() {
    return settings;
  }

  /**
   * For internal use only.
   */
  public Project setAnalysisDate(Date analysisDate) {
    this.analysisDate = analysisDate;
    return this;
  }

  /**
   * For internal use only.
   */
  public Project setAnalysisVersion(String analysisVersion) {
    this.analysisVersion = analysisVersion;
    return this;
  }

  /**
   * @return the scope of the current object
   */
  @Override
  public String getScope() {
    return Scopes.PROJECT;
  }

  /**
   * @return the qualifier of the current object
   */
  @Override
  public String getQualifier() {
    return isRoot() ? Qualifiers.PROJECT : Qualifiers.MODULE;
  }

  @Override
  public boolean matchFilePattern(String antPattern) {
    return false;
  }

  @CheckForNull
  @Override
  public Project getParent() {
    return parent;
  }

  /**
   * For internal use only.
   */
  public Project setParent(Project parent) {
    this.parent = parent;
    if (parent != null) {
      parent.modules.add(this);
    }
    return this;
  }

  /**
   * For internal use only.
   */
  public void removeFromParent() {
    if (parent != null) {
      parent.modules.remove(this);
    }
  }

  /**
   * @return the list of modules
   */
  public List<Project> getModules() {
    return modules;
  }

  /**
   * @return the current version of the project
   */
  public String getAnalysisVersion() {
    return analysisVersion;
  }

  /**
   * @return the analysis date, i.e. the date that will be used to store the snapshot
   */
  public Date getAnalysisDate() {
    return analysisDate;
  }

  public static Project createFromMavenIds(String groupId, String artifactId) {
    return createFromMavenIds(groupId, artifactId, null);
  }

  public static Project createFromMavenIds(String groupId, String artifactId, @Nullable String branch) {
    return new Project(String.format(MAVEN_KEY_FORMAT, groupId, artifactId), branch, "");
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("id", getId())
      .append("key", getKey())
      .append("qualifier", getQualifier())
      .toString();
  }

  @Override
  public String key() {
    return getKey();
  }

  @Override
  public String name() {
    return getName();
  }

  @Override
  public String path() {
    return getPath();
  }

  @Override
  public String longName() {
    return getLongName();
  }

  @Override
  public String qualifier() {
    return getQualifier();
  }

  // For internal use
  public void setBaseDir(java.io.File baseDir) {
    this.baseDir = baseDir;
  }

  java.io.File getBaseDir() {
    return baseDir;
  }
}
