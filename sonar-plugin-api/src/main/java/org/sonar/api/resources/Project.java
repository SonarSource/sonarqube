/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.resources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.maven.project.MavenProject;
import org.sonar.api.CoreProperties;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A class that manipulates Projects in the Sonar way.
 * 
 * @since 1.10
 */
public class Project extends Resource {

  public static final String SCOPE = Scopes.PROJECT;

  /**
   * @deprecated since version 1.11. Constant moved to CoreProperties
   */
  @Deprecated
  public static final String PARAM_REUSE_RULES_CONFIG = CoreProperties.REUSE_RULES_CONFIGURATION_PROPERTY;

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
  private Date analysisDate;
  private AnalysisType analysisType;
  private String analysisVersion;
  private boolean latestAnalysis;

  // modules tree
  private Project parent;
  private List<Project> modules = new ArrayList<Project>();

  public Project(String key) {
    setKey(key);
    setEffectiveKey(key);
  }

  public Project(String key, String branch, String name) {
    if (StringUtils.isNotBlank(branch)) {
      setKey(String.format("%s:%s", key, branch));
      this.name = String.format("%s %s", name, branch);
    } else {
      setKey(key);
      this.name = name;
    }
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

  /**
   * For internal use only.
   */
  public final Project setPom(MavenProject pom) {
    this.pom = pom;
    return this;
  }

  /**
   * @return the project's packaging
   * @deprecated in 2.8. See http://jira.codehaus.org/browse/SONAR-2341
   */
  @Deprecated
  public String getPackaging() {
    return packaging;
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

  /**
   * For internal use only.
   */
  public Project setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * For internal use only.
   * 
   * @deprecated in 2.8. See http://jira.codehaus.org/browse/SONAR-2341
   */
  @Deprecated
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
   * 
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
   * @return the project language
   */
  @Override
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
    return configuration.getString("sonar.language", Java.KEY);
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
   * @return whether to use external source for rules configuration
   * @deprecated since 2.5. See discussion from http://jira.codehaus.org/browse/SONAR-1873
   */
  @Deprecated
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
   *
   * @since 3.3 also applies exclusions in general settings page and global exclusions.
   * @deprecated replaced by {@link org.sonar.api.scan.filesystem.FileExclusions} in version 3.5
   */
  @Deprecated
  public String[] getExclusionPatterns() {
    return trimExclusions(ImmutableList.<String> builder()
      .add(configuration.getStringArray(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY))
      .add(configuration.getStringArray(CoreProperties.GLOBAL_EXCLUSIONS_PROPERTY)).build());
  }

  /**
   * Patterns of test exclusion as defined in project settings page.
   * Also applies exclusions in general settings page and global exclusions.
   *
   * @since 3.3
   * @deprecated replaced by {@link org.sonar.api.scan.filesystem.FileExclusions} in version 3.5
   */
  @Deprecated
  public String[] getTestExclusionPatterns() {
    String[] globalTestExclusions = configuration.getStringArray(CoreProperties.GLOBAL_TEST_EXCLUSIONS_PROPERTY);
    if (globalTestExclusions.length == 0) {
      globalTestExclusions = new String[] {CoreProperties.GLOBAL_TEST_EXCLUSIONS_DEFAULT};
    }

    return trimExclusions(ImmutableList.<String> builder()
        .add(configuration.getStringArray(CoreProperties.PROJECT_TEST_EXCLUSIONS_PROPERTY))
        .add(globalTestExclusions).build());
  }

  // http://jira.codehaus.org/browse/SONAR-2261 - exclusion must be trimmed
  private static String[] trimExclusions(List<String> exclusions) {
    List<String> trimmed = Lists.newArrayList();
    for (String exclusion : exclusions) {
      trimmed.add(StringUtils.trim(exclusion));
    }
    return trimmed.toArray(new String[trimmed.size()]);
  }

  /**
   * Set exclusion patterns. Configuration is not saved, so this method must be used ONLY IN UNIT TESTS.
   * @deprecated replaced by {@link org.sonar.api.scan.filesystem.FileExclusions} in version 3.5
   */
  @Deprecated
  public Project setExclusionPatterns(String[] s) {
    throw new UnsupportedOperationException("deprecated in 3.5");
  }

  /**
   * Note: it's better to get a reference on ProjectFileSystem as an IoC dependency (constructor parameter)
   * @deprecated replaced by {@link org.sonar.api.scan.filesystem.ModuleFileSystem} in 3.5
   */
  @Deprecated
  public ProjectFileSystem getFileSystem() {
    return fileSystem;
  }

  /**
   * For internal use only.
   * 
   * @deprecated since 2.6. See http://jira.codehaus.org/browse/SONAR-2126
   */
  @Deprecated
  public Project setFileSystem(ProjectFileSystem fs) {
    this.fileSystem = fs;
    return this;
  }

  /**
   * @deprecated since 2.5. See http://jira.codehaus.org/browse/SONAR-2011
   */
  @Deprecated
  public String getGroupId() {
    return pom.getGroupId();
  }

  /**
   * @deprecated since 2.5. See http://jira.codehaus.org/browse/SONAR-2011
   */
  @Deprecated
  public String getArtifactId() {
    return pom.getArtifactId();
  }

  /**
   * @return the underlying Maven project
   * @deprecated since 2.5. See http://jira.codehaus.org/browse/SONAR-2011 ,
   *             MavenProject can be retrieved as an IoC dependency
   */
  @Deprecated
  public MavenProject getPom() {
    return pom;
  }

  /**
   * @return the project configuration
   * @deprecated since 2.12. The component org.sonar.api.config.Settings must be used.
   */
  @Deprecated
  public Configuration getConfiguration() {
    return configuration;
  }

  /**
   * For internal use only.
   */
  public final Project setConfiguration(Configuration configuration) {
    this.configuration = configuration;
    return this;
  }

  /**
   * @deprecated since 3.6. Replaced by {@link org.sonar.api.config.Settings}.
   */
  @Deprecated
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
