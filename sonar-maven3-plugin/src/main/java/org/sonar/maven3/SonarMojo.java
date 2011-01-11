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
package org.sonar.maven3;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.apache.commons.configuration.*;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.slf4j.LoggerFactory;
import org.sonar.api.platform.Environment;
import org.sonar.batch.Batch;
import org.sonar.batch.MavenReactor;

import java.io.InputStream;

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

  public void execute() throws MojoExecutionException, MojoFailureException {
    initLogging();
    executeBatch();
  }

  private void executeBatch() throws MojoExecutionException {
    MavenReactor reactor = new MavenReactor(session);
    Batch batch = new Batch(getInitialConfiguration(),
        reactor, session, project, getLog(), lifecycleExecutor, artifactFactory,
        localRepository, artifactMetadataSource, artifactCollector, dependencyTreeBuilder,
        projectBuilder, Environment.MAVEN3, Maven3PluginExecutor.class);
    batch.execute();
  }

  private void initLogging() throws MojoExecutionException {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    JoranConfigurator jc = new JoranConfigurator();
    jc.setContext(context);
    context.reset();
    InputStream input = getClass().getResourceAsStream("/org/sonar/batch/logback.xml");
    System.setProperty("ROOT_LOGGER_LEVEL", getLog().isDebugEnabled() ? "DEBUG" : "INFO");
    try {
      jc.doConfigure(input);

    } catch (JoranException e) {
      throw new MojoExecutionException("can not initialize logging", e);

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  private Configuration getInitialConfiguration() {
    CompositeConfiguration configuration = new CompositeConfiguration();
    configuration.addConfiguration(new SystemConfiguration());
    configuration.addConfiguration(new EnvironmentConfiguration());
    configuration.addConfiguration(new MapConfiguration(project.getModel().getProperties()));
    return configuration;
  }
}
