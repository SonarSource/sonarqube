/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.maven;

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
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.batch.Batch;
import org.sonar.batch.MavenProjectConverter;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.core.config.Logback;

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

  /**
   * @parameter expression="${sonar.verbose}"  default-value="false"
   */
  private boolean verbose;

  public void execute() throws MojoExecutionException, MojoFailureException {
    configureLogback();
    executeBatch();
  }

  private void executeBatch() throws MojoExecutionException {
    ProjectDefinition def = MavenProjectConverter.convert(session.getSortedProjects(), project);
    ProjectReactor reactor = new ProjectReactor(def);

    Batch batch = Batch.create(reactor, null,
        session, getLog(), lifecycleExecutor, pluginManager, artifactFactory,
        localRepository, artifactMetadataSource, artifactCollector, dependencyTreeBuilder,
        projectBuilder, getEnvironmentInformation(), Maven2PluginExecutor.class);
    batch.execute();
  }

  private EnvironmentInformation getEnvironmentInformation() {
    String mavenVersion = runtimeInformation.getApplicationVersion().toString();
    return new EnvironmentInformation("Maven", mavenVersion);
  }

  private void configureLogback() {
    boolean debugMode = (verbose || getLog().isDebugEnabled());

    // this system property is required by the logback configuration
    System.setProperty("ROOT_LOGGER_LEVEL", debugMode ? "DEBUG" : "INFO");
    Logback.configure("/org/sonar/maven/logback.xml");
  }
}
