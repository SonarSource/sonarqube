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
package org.sonar.api.resources;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.maven.project.MavenProject;
import org.sonar.api.CoreProperties;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A class that manipulates Projects in the Sonar way, i.e. mixing MavenProjects with the way it should be analyzed
 *
 * @since 1.10
 */
public class Project extends Resource {

  /**
   * @deprecated since version 1.11. Constant moved to CoreProperties
   */
  @Deprecated
  public static final String PARAM_VERSION = "sonar.projectVersion";

  /**
   * @deprecated since version 1.11. Constant moved to CoreProperties
   */
  @Deprecated
  public static final String PARAM_DATE = "sonar.projectDate";

  /**
   * @deprecated since version 1.11. Constant moved to CoreProperties
   */
  @Deprecated
  public static final String PARAM_LANGUAGE = "sonar.language";

  /**
   * @deprecated since version 1.11. Constant moved to CoreProperties
   */
  @Deprecated
  public static final String PARAM_DYNAMIC_ANALYSIS = "sonar.dynamicAnalysis";

  /**
   * @deprecated since version 1.11. Constant moved to CoreProperties
   */
  @Deprecated
  public static final String PARAM_EXCLUSIONS = "sonar.exclusions";

  /**
   * @deprecated since version 1.11. Constant moved to CoreProperties
   */
  @Deprecated
  public static final String PARAM_REUSE_RULES_CONFIG = "sonar.reuseExistingRulesConfiguration";


  /**
   * Enumerates the type of possible analysis
   */
  public enum AnalysisType {
    STATIC, DYNAMIC, REUSE_REPORTS;

    /**
     * @param includeReuseReportMode whether to count report reuse as dynamic or not
     * @return whether this a dynamic analysis
     */
    public boolean isDynamic(boolean includeReuseReportMode) {
      return equals(Project.AnalysisType.DYNAMIC) ||
          (equals(Project.AnalysisType.REUSE_REPORTS) && includeReuseReportMode);
    }
  }

  private MavenProject pom;
  private String branch;
  private ProjectFileSystem fileSystem;
  private Configuration configuration;
  private String name;
  private String description;
  private String packaging;
  private Language language;
  private String languageKey;
  private Date analysisDate;
  private AnalysisType analysisType;
  private String[] exclusionPatterns;
  private String analysisVersion;
  private boolean latestAnalysis;
  
  // modules tree
  private Project parent;
  private List<Project> modules = new ArrayList<Project>();

  public Project(String key) {
    setKey(key);
  }

  public Project(String key, String branch, String name) {
    if (StringUtils.isNotBlank(branch)) {
      setKey(String.format("%s:%s", key, branch));
      this.name = String.format("%s %s", name, branch);
    } else {
      setKey(key);
      this.name = name;
    }
    this.branch = branch;
  }

  public String getBranch() {
    return branch;
  }

  public Project setBranch(String branch) {
    this.branch = branch;
    return this;
  }

  public final Project setPom(MavenProject pom) {
    this.pom = pom;
    return this;
  }

  /**
   * @return the project's packaging
   */
  public String getPackaging() {
    return packaging;
  }

  public String getName() {
    return name;
  }

  public String getLongName() {
    return null;
  }

  public String getDescription() {
    return description;
  }

  public Project setName(String name) {
    this.name = name;
    return this;
  }

  public Project setDescription(String description) {
    this.description = description;
    return this;
  }

  public Project setPackaging(String packaging) {
    this.packaging = packaging;
    return this;
  }

  /**
   * @return whether the current project is root project
   */
  public boolean isRoot() {
    return getParent() == null;
  }

  public Project getRoot() {
    return (parent == null ? this : parent.getRoot());
  }

  /**
   * @return whether the current project is a module
   */
  public boolean isModule() {
    return !isRoot();
  }

  /**
   * @return the type of analysis of the project
   */
  public AnalysisType getAnalysisType() {
    return analysisType;
  }

  public Project setAnalysisType(AnalysisType at) {
    this.analysisType = at;
    return this;
  }

  /**
   * whether it's the latest analysis done on this project (displayed in sonar dashboard) or an analysis on a past revision.
   * @since 2.0
   */
  public boolean isLatestAnalysis() {
    return latestAnalysis;
  }

  /**
   * For internal use only.
   */
  public Project setLatestAnalysis(boolean b) {
    this.latestAnalysis = b;
    return this;
  }

  /**
   * Used for Maven projects
   */
  public String getGroupId() {
    return pom.getGroupId();
  }

  /**
   * Used for Maven projects
   */
  public String getArtifactId() {
    return pom.getArtifactId();
  }

  /**
   * @return the project language
   */
  public Language getLanguage() {
    return language;
  }

  public Project setLanguage(Language language) {
    this.language = language;
    return this;
  }

  /**
   * @return the language key
   */
  public String getLanguageKey() {
    return languageKey;
  }

  /**
   * For internal use only.
   */
  public Project setLanguageKey(String languageKey) {
    this.languageKey = languageKey;
    return this;
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
  public String getScope() {
    return SCOPE_SET;
  }

  /**
   * @return the qualifier of the current object
   */
  public String getQualifier() {
    return isRoot() ? QUALIFIER_PROJECT : QUALIFIER_MODULE;
  }

  @Override
  public boolean matchFilePattern(String antPattern) {
    return false;
  }

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
    if (parent!=null) {
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
   * @return whether to use external source for rules configuration
   */
  public boolean getReuseExistingRulesConfig() {
    return (configuration != null && configuration.getBoolean(CoreProperties.REUSE_RULES_CONFIGURATION_PROPERTY, false));
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

  /**
   * Patterns of resource exclusion as defined in project settings page.
   */
  public String[] getExclusionPatterns() {
    return exclusionPatterns;
  }

  /**
   * Set exclusion patterns. Configuration is not saved, so this method must be used ONLY IN UNIT TESTS.
   */
  public Project setExclusionPatterns(String[] s) {
    this.exclusionPatterns = s;
    return this;
  }

  public ProjectFileSystem getFileSystem() {
    return fileSystem;
  }

  public Project setFileSystem(ProjectFileSystem fs) {
    this.fileSystem = fs;
    return this;
  }

  /**
   * @return the underlying maven project
   */
  public MavenProject getPom() {
    return pom;
  }

  /**
   * @return the project configuration
   */
  public Configuration getConfiguration() {
    return configuration;
  }

  /**
   * Sets the configuration
   *
   * @return the current object
   */
  public final Project setConfiguration(Configuration configuration) {
    this.configuration = configuration;
    return this;
  }

  public Object getProperty(String key) {
    return configuration != null ? configuration.getProperty(key) : null;
  }

  public static Project createFromMavenIds(String groupId, String artifactId) {
    return new Project(String.format("%s:%s", groupId, artifactId));
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("id", getId())
        .append("key", getKey())
        .append("qualifier", getQualifier())
        .toString();
  }
}
