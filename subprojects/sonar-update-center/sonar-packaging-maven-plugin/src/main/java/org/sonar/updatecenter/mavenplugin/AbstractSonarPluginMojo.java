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
package org.sonar.updatecenter.mavenplugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class for Sonar-plugin-packaging related tasks.
 * 
 * @author Evgeny Mandrikov
 */
public abstract class AbstractSonarPluginMojo extends AbstractMojo {
  public static final String SONAR_GROUPID = "org.codehaus.sonar";
  public static final String SONAR_PLUGIN_API_ARTIFACTID = "sonar-plugin-api";
  public static final String SONAR_PLUGIN_API_TYPE = "jar";

  /**
   * The Maven project.
   * 
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

  /**
   * Directory containing the generated JAR.
   * 
   * @parameter expression="${project.build.directory}"
   * @required
   */
  private File outputDirectory;

  /**
   * Directory containing the classes and resource files that should be packaged into the JAR.
   * 
   * @parameter expression="${project.build.outputDirectory}"
   * @required
   */
  private File classesDirectory;

  /**
   * The directory where the app is built.
   * 
   * @parameter expression="${project.build.directory}/${project.build.finalName}"
   * @required
   */
  private File appDirectory;

  /**
   * Name of the generated JAR.
   * 
   * @parameter alias="jarName" expression="${jar.finalName}" default-value="${project.build.finalName}"
   * @required
   */
  private String finalName;

  /**
   * Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.
   * 
   * @parameter
   */
  private String classifier;

  /**
   * @component
   */
  protected MavenProjectHelper projectHelper;

  /**
   * Plugin key.
   * 
   * @parameter expression="${sonar.pluginKey}" default-value="${project.artifactId}"
   */
  private String pluginKey;

  /**
   * @parameter expression="${sonar.pluginTermsConditionsUrl}"
   */
  private String pluginTermsConditionsUrl;

  /**
   * Name of plugin class.
   * 
   * @parameter expression="${sonar.pluginClass}"
   * @required
   */
  private String pluginClass;

  /**
   * @parameter expression="${sonar.pluginName}" default-value="${project.name}"
   */
  private String pluginName;

  /**
   * @parameter default-value="${project.description}"
   */
  private String pluginDescription;

  /**
   * @parameter default-value="${project.url}"
   */
  private String pluginUrl;

  /**
   * @parameter default-value="${project.issueManagement.url}"
   */
  private String pluginIssueTrackerUrl;

  /**
   * @parameter
   * @since 0.3
   */
  private boolean useChildFirstClassLoader = false;

  /**
   * @parameter expression="${sonar.skipDependenciesPackaging}"
   */
  private boolean skipDependenciesPackaging = false;

  protected final MavenProject getProject() {
    return project;
  }

  protected final File getOutputDirectory() {
    return outputDirectory;
  }

  /**
   * @return the main classes directory, so it's used as the root of the jar.
   */
  protected final File getClassesDirectory() {
    return classesDirectory;
  }

  public File getAppDirectory() {
    return appDirectory;
  }

  protected final String getFinalName() {
    return finalName;
  }

  protected final String getClassifier() {
    return classifier;
  }

  public String getPluginKey() {
    return pluginKey;
  }

  protected final String getPluginClass() {
    return pluginClass;
  }

  protected final String getPluginName() {
    return pluginName;
  }

  protected final String getPluginDescription() {
    return pluginDescription;
  }

  protected final String getPluginUrl() {
    return pluginUrl;
  }

  protected String getPluginTermsConditionsUrl() {
    return pluginTermsConditionsUrl;
  }

  protected String getPluginIssueTrackerUrl() {
    return pluginIssueTrackerUrl;
  }

  public boolean isUseChildFirstClassLoader() {
    return useChildFirstClassLoader;
  }

  protected boolean isSkipDependenciesPackaging() {
    return skipDependenciesPackaging;
  }

  @SuppressWarnings( { "unchecked" })
  protected Set<Artifact> getDependencyArtifacts() {
    return getProject().getDependencyArtifacts();
  }

  protected Set<Artifact> getDependencyArtifacts(String scope) {
    Set<Artifact> result = new HashSet<Artifact>();
    for (Artifact dep : getDependencyArtifacts()) {
      if (scope.equals(dep.getScope())) {
        result.add(dep);
      }
    }
    return result;
  }

  @SuppressWarnings( { "unchecked" })
  protected Set<Artifact> getIncludedArtifacts() {
    Set<Artifact> result = new HashSet<Artifact>();
    Set<Artifact> artifacts = getProject().getArtifacts();
    ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);
    for (Artifact artifact : artifacts) {
      if (filter.include(artifact)) {
        result.add(artifact);
      }
    }
    return result;
  }

  protected final Artifact getSonarPluginApiArtifact() {
    Set<Artifact> dependencies = getDependencyArtifacts();
    if (dependencies != null) {
      for (Artifact dep : dependencies) {
        if (SONAR_GROUPID.equals(dep.getGroupId())
            && SONAR_PLUGIN_API_ARTIFACTID.equals(dep.getArtifactId())
            && SONAR_PLUGIN_API_TYPE.equals(dep.getType())) {
          return dep;
        }
      }
    }
    return null;
  }

  protected String getMessage(String title, List<String> ids) {
    StringBuilder message = new StringBuilder();
    message.append(title);
    message.append("\n\n");
    for (String id : ids) {
      message.append("\t").append(id).append("\n");
    }
    return message.toString();
  }
}
