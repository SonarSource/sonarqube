/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.maven;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.sonar.api.batch.maven.MavenUtils;
import org.sonar.batch.scan.maven.MavenProjectConverter;
import org.sonar.runner.api.EmbeddedRunner;
import org.sonar.runner.api.RunnerProperties;
import org.sonar.runner.api.ScanProperties;

import java.util.Map.Entry;
import java.util.Set;

/**
 * @goal sonar
 * @aggregator
 * @requiresDependencyResolution test
 */
public final class SonarMojo extends AbstractMojo {

  /**
   * @parameter expression="${session}"
   * @required
   * @readonly
   */
  private MavenSession session;

  /**
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  private MavenProject project;

  /**
   * @component
   * @required
   */
  private LifecycleExecutor lifecycleExecutor;

  /**
   * @component
   * @required
   */
  private PluginManager pluginManager;

  /**
   * The artifact factory to use.
   *
   * @component
   * @required
   * @readonly
   */
  private ArtifactFactory artifactFactory;

  /**
   * The artifact repository to use.
   *
   * @parameter expression="${localRepository}"
   * @required
   * @readonly
   */
  private ArtifactRepository localRepository;

  /**
   * The artifact metadata source to use.
   *
   * @component
   * @required
   * @readonly
   */
  private ArtifactMetadataSource artifactMetadataSource;

  /**
   * The artifact collector to use.
   *
   * @component
   * @required
   * @readonly
   */
  private ArtifactCollector artifactCollector;

  /**
   * The dependency tree builder to use.
   *
   * @component
   * @required
   * @readonly
   */
  private DependencyTreeBuilder dependencyTreeBuilder;

  /**
   * @component
   * @required
   * @readonly
   */
  private MavenProjectBuilder projectBuilder;

  /**
   * @component
   * @required
   * @readonly
   */
  private RuntimeInformation runtimeInformation;

  public void execute() throws MojoExecutionException, MojoFailureException {

    EmbeddedRunner runner = EmbeddedRunner.create()
        .setApp("Maven", getMavenVersion());
    // Workaround for SONARPLUGINS-2947
    // TODO remove when it will be fixed
    runner.setProperty("sonarRunner.userAgent", "Maven");
    runner.setProperty("sonarRunner.userAgentVersion", getMavenVersion());
    Set<Entry<Object, Object>> properties = project.getModel().getProperties().entrySet();
    for (Entry<Object, Object> entry : properties) {
      runner.setProperty(ObjectUtils.toString(entry.getKey()), ObjectUtils.toString(entry.getValue()));
    }
    String encoding = MavenUtils.getSourceEncoding(project);
    if (encoding != null) {
      runner.setProperty(ScanProperties.PROJECT_SOURCE_ENCODING, encoding);
    }
    runner.setProperty(ScanProperties.PROJECT_KEY, MavenProjectConverter.getSonarKey(project))
        .setProperty(RunnerProperties.WORK_DIR, MavenProjectConverter.getSonarWorkDir(project).getAbsolutePath())
        .setProperty(ScanProperties.PROJECT_BASEDIR, project.getBasedir().getAbsolutePath())
        .setProperty(ScanProperties.PROJECT_VERSION, StringUtils.defaultString(project.getVersion()))
        .setProperty(ScanProperties.PROJECT_NAME, StringUtils.defaultString(project.getName()))
        .setProperty(ScanProperties.PROJECT_DESCRIPTION, StringUtils.defaultString(project.getDescription()))
        .setProperty(ScanProperties.PROJECT_SOURCE_DIRS, ".")
        // Required to share ProjectBuilder extension between SonarMavenProjectBuilder and Sonar classloader
        .setUnmaskedPackages("org.sonar.api.batch.bootstrap")
        .addExtensions(session, getLog(), lifecycleExecutor, artifactFactory, localRepository, artifactMetadataSource, artifactCollector,
            dependencyTreeBuilder, projectBuilder, Maven2PluginExecutor.class, new SonarMaven2ProjectBuilder(session));
    if (getLog().isDebugEnabled()) {
      runner.setProperty("sonar.verbose", "true");
    }
    runner.execute();
  }

  private String getMavenVersion() {
    return runtimeInformation.getApplicationVersion().toString();
  }
}
