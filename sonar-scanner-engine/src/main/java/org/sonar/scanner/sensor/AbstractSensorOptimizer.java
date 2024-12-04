/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.scanner.sensor;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.scanner.config.DefaultConfiguration;

public abstract class AbstractSensorOptimizer {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractSensorOptimizer.class);

  private final FileSystem fs;
  private final ActiveRules activeRules;
  private final Configuration originalConfiguration;
  private final ConfigurationReadsInterceptor config;

  public AbstractSensorOptimizer(FileSystem fs, ActiveRules activeRules, Configuration config) {
    this.fs = fs;
    this.activeRules = activeRules;
    this.originalConfiguration = config;
    this.config = new ConfigurationReadsInterceptor(config);
  }

  /**
   * Decide if the given Sensor should be executed.
   */
  public boolean shouldExecute(DefaultSensorDescriptor descriptor) {
    if (!fsCondition(descriptor)) {
      LOG.debug("'{}' skipped because there is no related file in current project", descriptor.name());
      return false;
    }
    if (!activeRulesCondition(descriptor)) {
      LOG.debug("'{}' skipped because there is no related rule activated in the quality profile", descriptor.name());
      return false;
    }
    if (!settingsCondition(descriptor)) {
      String accessedConfiguration = config.getAccessedKeys().stream()
        .map(key -> "- " + key + ": " + getConfigurationValue(key).orElse("<empty>"))
        .collect(Collectors.joining("\n"));

      LOG.debug("""
        '{}' skipped because of missing configuration requirements.
        Accessed configuration:
        {}""", descriptor.name(), accessedConfiguration);
      return false;
    }
    return true;
  }

  private Optional<String> getConfigurationValue(String key) {
    if (originalConfiguration instanceof DefaultConfiguration configuration) {
      return Optional.ofNullable(configuration.getOriginalProperties().get(key));
    } else {
      return config.get(key);
    }
  }

  private boolean settingsCondition(DefaultSensorDescriptor descriptor) {
    if (descriptor.configurationPredicate() != null) {
      return descriptor.configurationPredicate().test(config);
    }
    return true;
  }

  private boolean activeRulesCondition(DefaultSensorDescriptor descriptor) {
    if (!descriptor.ruleRepositories().isEmpty()) {
      for (String repoKey : descriptor.ruleRepositories()) {
        if (!activeRules.findByRepository(repoKey).isEmpty()) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

  private boolean fsCondition(DefaultSensorDescriptor descriptor) {
    if (!descriptor.languages().isEmpty() || descriptor.type() != null) {
      FilePredicate langPredicate = descriptor.languages().isEmpty() ? fs.predicates().all() : fs.predicates().hasLanguages(descriptor.languages());

      FilePredicate typePredicate = descriptor.type() == null ? fs.predicates().all() : fs.predicates().hasType(descriptor.type());
      return fs.hasFiles(fs.predicates().and(langPredicate, typePredicate));
    }
    return true;
  }

  private static class ConfigurationReadsInterceptor implements Configuration {
    private final Configuration configuration;
    private final Set<String> accessedKeys;

    private ConfigurationReadsInterceptor(Configuration configuration) {
      this.configuration = configuration;
      this.accessedKeys = new HashSet<>();
    }

    Set<String> getAccessedKeys() {
      return accessedKeys;
    }

    @Override
    public boolean hasKey(String key) {
      boolean hasKey = configuration.hasKey(key);
      accessedKeys.add(key);
      return hasKey;
    }

    @Override
    public Optional<String> get(String key) {
      Optional<String> value = configuration.get(key);
      accessedKeys.add(key);
      return value;
    }

    @Override
    public String[] getStringArray(String key) {
      String[] values = configuration.getStringArray(key);
      accessedKeys.add(key);
      return values;
    }
  }
}
