/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
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

  private LoggingConfiguration loggingConfig;
  private List<Object> components;
  private Map<String, String> globalProperties = new HashMap<>();

  private Batch(Builder builder) {
    components = new ArrayList<>();
    components.addAll(builder.components);
    if (builder.environment != null) {
      components.add(builder.environment);
    }
    if (builder.globalProperties != null) {
      globalProperties.putAll(builder.globalProperties);
    }
    if (builder.isEnableLoggingConfiguration()) {
      loggingConfig = new LoggingConfiguration(builder.environment).setProperties(globalProperties);

      if (builder.logOutput != null) {
        loggingConfig.setLogOutput(builder.logOutput);
      }
    }
  }

  public LoggingConfiguration getLoggingConfiguration() {
    return loggingConfig;
  }

  public synchronized Batch execute() {
    return doExecute(this.globalProperties, this.components);
  }

  public synchronized Batch doExecute(Map<String, String> scannerProperties, List<Object> components) {
    configureLogging();
    try {
      GlobalContainer.create(scannerProperties, components).execute();
    } catch (RuntimeException e) {
      throw handleException(e);
    }
    return this;
  }

  /**
   * @since 4.4
   * @deprecated since 6.6 use {@link #execute()}
   */
  @Deprecated
  public synchronized Batch start() {
    return this;
  }

  /**
   * @since 4.4
   * @deprecated since 6.6 use {@link #execute()}
   */
  @Deprecated
  public Batch executeTask(Map<String, String> analysisProperties, Object... components) {
    Map<String, String> mergedProps = new HashMap<>(this.globalProperties);
    mergedProps.putAll(analysisProperties);
    List<Object> mergedComponents = new ArrayList<>(this.components);
    mergedComponents.addAll(Arrays.asList(components));
    return doExecute(mergedProps, mergedComponents);
  }

  private RuntimeException handleException(RuntimeException t) {
    if (loggingConfig.isVerbose()) {
      return t;
    }

    Throwable y = t;
    do {
      if (y instanceof MessageException) {
        return (MessageException) y;
      }
      y = y.getCause();
    } while (y != null);

    return t;
  }

  /**
   * @since 4.4
   * @deprecated since 6.6 use {@link #execute()}
   */
  @Deprecated
  public synchronized void stop() {
  }

  private void configureLogging() {
    if (loggingConfig != null) {
      loggingConfig.setProperties(globalProperties);
      LoggingConfigurator.apply(loggingConfig);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Map<String, String> globalProperties;
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

    public Builder setGlobalProperties(Map<String, String> globalProperties) {
      this.globalProperties = globalProperties;
      return this;
    }

    /**
     * @deprecated since 6.6 use {@link #setGlobalProperties(Map)}
     */
    @Deprecated
    public Builder setBootstrapProperties(Map<String, String> bootstrapProperties) {
      this.globalProperties = bootstrapProperties;
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
