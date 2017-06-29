/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.batch.bootstrapper;

import com.google.common.base.Throwables;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.bootstrap.GlobalContainer;

/**
 * Entry point for SonarQube Scanner API 2.1+.
 *
 * @since 2.14
 */
public final class Batch {

  private boolean started = false;
  private LoggingConfiguration loggingConfig;
  private List<Object> components;
  private Map<String, String> bootstrapProperties = new HashMap<>();
  private GlobalContainer bootstrapContainer;

  private Batch(Builder builder) {
    components = new ArrayList<>();
    components.addAll(builder.components);
    if (builder.environment != null) {
      components.add(builder.environment);
    }
    if (builder.bootstrapProperties != null) {
      bootstrapProperties.putAll(builder.bootstrapProperties);
    }
    if (builder.isEnableLoggingConfiguration()) {
      loggingConfig = new LoggingConfiguration(builder.environment).setProperties(bootstrapProperties);

      if (builder.logOutput != null) {
        loggingConfig.setLogOutput(builder.logOutput);
      }
    }
  }

  public LoggingConfiguration getLoggingConfiguration() {
    return loggingConfig;
  }

  /**
   * @deprecated since 4.4 use {@link #start()}, {@link #executeTask(Map)} and then {@link #stop()}
   */
  @Deprecated
  public synchronized Batch execute() {
    configureLogging();
    start();
    boolean threw = true;
    try {
      executeTask(bootstrapProperties);
      threw = false;
    } finally {
      doStop(threw);
    }

    return this;
  }

  /**
   * @since 4.4
   */
  public synchronized Batch start() {
    return start(false);
  }

  public synchronized Batch start(boolean preferCache) {
    if (started) {
      throw new IllegalStateException("Batch is already started");
    }

    configureLogging();
    try {
      bootstrapContainer = GlobalContainer.create(bootstrapProperties, components);
      bootstrapContainer.startComponents();
    } catch (RuntimeException e) {
      throw handleException(e);
    }
    this.started = true;

    return this;
  }

  /**
   * @since 4.4
   */
  public Batch executeTask(Map<String, String> analysisProperties, Object... components) {
    checkStarted();
    configureTaskLogging(analysisProperties);
    try {
      bootstrapContainer.executeTask(analysisProperties, components);
    } catch (RuntimeException e) {
      throw handleException(e);
    }
    return this;
  }

  /**
   * @since 5.2
   */
  public Batch executeTask(Map<String, String> analysisProperties, IssueListener issueListener) {
    checkStarted();
    configureTaskLogging(analysisProperties);
    try {
      bootstrapContainer.executeTask(analysisProperties, components, issueListener);
    } catch (RuntimeException e) {
      throw handleException(e);
    }
    return this;
  }

  private void checkStarted() {
    if (!started) {
      throw new IllegalStateException("Scanner engine is not started. Unable to execute task.");
    }
  }

  private RuntimeException handleException(RuntimeException t) {
    if (loggingConfig.isVerbose()) {
      return t;
    }

    for (Throwable y : Throwables.getCausalChain(t)) {
      if (y instanceof MessageException) {
        return (MessageException) y;
      }
    }

    return t;
  }

  /**
   * @since 5.2
   * @deprecated since 5.6
   */
  @Deprecated
  public Batch syncProject(String projectKey) {
    checkStarted();
    return this;
  }

  /**
   * @since 4.4
   */
  public synchronized void stop() {
    doStop(false);
  }

  private void doStop(boolean swallowException) {
    checkStarted();
    configureLogging();
    try {
      bootstrapContainer.stopComponents(swallowException);
    } catch (RuntimeException e) {
      throw handleException(e);
    }
    this.started = false;
  }

  private void configureLogging() {
    if (loggingConfig != null) {
      loggingConfig.setProperties(bootstrapProperties);
      LoggingConfigurator.apply(loggingConfig);
    }
  }

  private void configureTaskLogging(Map<String, String> taskProperties) {
    if (loggingConfig != null) {
      loggingConfig.setProperties(taskProperties, bootstrapProperties);
      LoggingConfigurator.apply(loggingConfig);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Map<String, String> bootstrapProperties;
    private EnvironmentInformation environment;
    private List<Object> components = new ArrayList<>();
    private boolean enableLoggingConfiguration = true;
    private LogOutput logOutput;

    private Builder() {
    }

    public Builder setEnvironment(EnvironmentInformation env) {
      this.environment = env;
      return this;
    }

    public Builder setComponents(List<Object> l) {
      this.components = l;
      return this;
    }

    public Builder setLogOutput(@Nullable LogOutput logOutput) {
      this.logOutput = logOutput;
      return this;
    }

    /**
     * @deprecated since 3.7 use {@link #setBootstrapProperties(Map)}
     */
    @Deprecated
    public Builder setGlobalProperties(Map<String, String> globalProperties) {
      this.bootstrapProperties = globalProperties;
      return this;
    }

    public Builder setBootstrapProperties(Map<String, String> bootstrapProperties) {
      this.bootstrapProperties = bootstrapProperties;
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
      if (components == null) {
        throw new IllegalStateException("Batch components are not set");
      }
      return new Batch(this);
    }
  }
}
