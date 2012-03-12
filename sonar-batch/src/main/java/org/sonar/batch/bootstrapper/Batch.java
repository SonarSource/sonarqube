/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.batch.bootstrapper;

import com.google.common.collect.Lists;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.batch.bootstrap.BootstrapModule;
import org.sonar.batch.bootstrap.Module;
import org.sonar.core.PicoUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Entry point for batch bootstrappers.
 *
 * @since 2.14
 */
public final class Batch {

  private LoggingConfiguration logging;
  private List components;
  private ProjectReactor projectReactor;

  private Batch(Builder builder) {
    components = Lists.newArrayList();
    components.addAll(builder.components);
    components.add(builder.environment);
    projectReactor = builder.projectReactor;
    if (builder.isEnableLoggingConfiguration()) {
      logging = LoggingConfiguration.create().setProperties((Map) projectReactor.getRoot().getProperties());
    }
  }

  public LoggingConfiguration getLoggingConfiguration() {
    return logging;
  }

  public Batch execute() {
    configureLogging();
    startBatch();
    return this;
  }

  private void configureLogging() {
    if (logging != null) {
      logging.configure();
    }
  }

  private void startBatch() {
    Module bootstrapModule = new BootstrapModule(projectReactor, components.toArray(new Object[components.size()])).init();
    try {
      bootstrapModule.start();
    } catch (RuntimeException e) {
      PicoUtils.handleStartupException(e, LoggerFactory.getLogger(getClass()));
    } finally {
      try {
        bootstrapModule.stop();
      } catch (Exception e) {
        // never throw exceptions in a finally block
      }
    }
  }


  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ProjectReactor projectReactor;
    private EnvironmentInformation environment;
    private List components = Lists.newArrayList();
    private boolean enableLoggingConfiguration = true;

    private Builder() {
    }

    public Builder setProjectReactor(ProjectReactor projectReactor) {
      this.projectReactor = projectReactor;
      return this;
    }

    public Builder setEnvironment(EnvironmentInformation env) {
      this.environment = env;
      return this;
    }

    public Builder setComponents(List l) {
      this.components = l;
      return this;
    }

    public Builder addComponents(Object... components) {
      this.components.addAll(Arrays.asList(components));
      return this;
    }

    public Builder addComponent(Object component) {
      this.components.add(component);
      return this;
    }

    public boolean isEnableLoggingConfiguration() {
      return enableLoggingConfiguration;
    }

    /**
     * Logback is configured by default. It can be disabled, but n this case the batch bootstrapper must provide its
     * own implementation of SLF4J.
     */
    public Builder setEnableLoggingConfiguration(boolean b) {
      this.enableLoggingConfiguration = b;
      return this;
    }

    public Batch build() {
      if (projectReactor == null) {
        throw new IllegalStateException("ProjectReactor is not set");
      }
      if (environment == null) {
        throw new IllegalStateException("EnvironmentInfo is not set");
      }
      if (components == null) {
        throw new IllegalStateException("Batch components are not set");
      }
      return new Batch(this);
    }
  }
}