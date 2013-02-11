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
import com.google.common.collect.Maps;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.batch.bootstrap.BootstrapContainer;
import org.sonar.batch.bootstrap.GlobalBatchProperties;
import org.sonar.batch.bootstrap.Container;
import org.sonar.core.PicoUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Entry point for batch bootstrappers.
 *
 * @since 2.14
 */
public final class Batch {

  private LoggingConfiguration logging;
  private List<Object> components;
  private Map<String, String> globalProperties = Maps.newHashMap();
  private String taskCommand;
  private ProjectReactor projectReactor;

  private Batch(Builder builder) {
    components = Lists.newArrayList();
    components.addAll(builder.components);
    components.add(builder.environment);
    if (builder.globalProperties != null) {
      globalProperties.putAll(builder.globalProperties);
    }
    else {
      // For backward compatibility, previously all properties were set in root project
      globalProperties.putAll(Maps.fromProperties(builder.projectReactor.getRoot().getProperties()));
    }
    this.taskCommand = builder.taskCommand;
    projectReactor = builder.projectReactor;
    if (builder.isEnableLoggingConfiguration()) {
      logging = LoggingConfiguration.create().setProperties(globalProperties);
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
    Container bootstrapModule = null;
    try {
      bootstrapModule = new BootstrapContainer(new GlobalBatchProperties(globalProperties), taskCommand,
          projectReactor, components.toArray(new Object[components.size()]));
      bootstrapModule.init();
      bootstrapModule.start();
    } catch (RuntimeException e) {
      PicoUtils.propagateStartupException(e);
    } finally {
      try {
        if (bootstrapModule != null) {
          bootstrapModule.stop();
        }
      } catch (Exception e) {
        // never throw exceptions in a finally block
        LoggerFactory.getLogger(Batch.class).error("Error while stopping batch", e);
      }
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Map<String, String> globalProperties;
    private String taskCommand;
    private ProjectReactor projectReactor;
    private EnvironmentInformation environment;
    private List<Object> components = Lists.newArrayList();
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

    public Builder setComponents(List<Object> l) {
      this.components = l;
      return this;
    }

    public Builder setGlobalProperties(Map<String, String> globalProperties) {
      this.globalProperties = globalProperties;
      return this;
    }

    public Builder setTaskCommand(String taskCommand) {
      this.taskCommand = taskCommand;
      return this;
    }

    public Builder addComponents(Object... components) {
      Collections.addAll(this.components, components);
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
