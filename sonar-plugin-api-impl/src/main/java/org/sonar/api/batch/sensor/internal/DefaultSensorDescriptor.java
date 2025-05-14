/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.api.batch.sensor.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;

public class DefaultSensorDescriptor implements SensorDescriptor {
  public static final Set<String> HARDCODED_INDEPENDENT_FILE_SENSORS = Collections.unmodifiableSet(Stream.of(
          "CSS Metrics",
          "CSS Rules",
          "HTML",
          "XML Sensor"
  ).collect(Collectors.toSet()));

  private String name;
  private String[] languages = new String[0];
  private InputFile.Type type = null;
  private String[] ruleRepositories = new String[0];
  private boolean global = false;
  private Predicate<Configuration> configurationPredicate;
  private boolean processesFilesIndependently = false;
  private boolean processesHiddenFiles = false;

  public String name() {
    return name;
  }

  public Collection<String> languages() {
    return Arrays.asList(languages);
  }

  @Nullable
  public InputFile.Type type() {
    return type;
  }

  public Collection<String> ruleRepositories() {
    return Arrays.asList(ruleRepositories);
  }

  public Predicate<Configuration> configurationPredicate() {
    return configurationPredicate;
  }

  public boolean isGlobal() {
    return global;
  }

  public boolean isProcessesFilesIndependently() {
    return processesFilesIndependently;
  }

  public boolean isProcessesHiddenFiles() {
    return processesHiddenFiles;
  }

  @Override
  public DefaultSensorDescriptor name(String name) {
    // TODO: Remove this hardcoded list once all plugins will implement the new API "processFilesIndependently"
    if (HARDCODED_INDEPENDENT_FILE_SENSORS.contains(name)) {
      processesFilesIndependently = true;
    }
    this.name = name;
    return this;
  }

  @Override
  public DefaultSensorDescriptor onlyOnLanguage(String languageKey) {
    return onlyOnLanguages(languageKey);
  }

  @Override
  public DefaultSensorDescriptor onlyOnLanguages(String... languageKeys) {
    this.languages = languageKeys;
    return this;
  }

  @Override
  public DefaultSensorDescriptor onlyOnFileType(InputFile.Type type) {
    this.type = type;
    return this;
  }

  @Override
  public DefaultSensorDescriptor createIssuesForRuleRepository(String... repositoryKey) {
    return createIssuesForRuleRepositories(repositoryKey);
  }

  @Override
  public DefaultSensorDescriptor createIssuesForRuleRepositories(String... repositoryKeys) {
    this.ruleRepositories = repositoryKeys;
    return this;
  }

  @Override
  public SensorDescriptor global() {
    this.global = true;
    return this;
  }

  @Override
  public SensorDescriptor onlyWhenConfiguration(Predicate<Configuration> configurationPredicate) {
    this.configurationPredicate = configurationPredicate;
    return this;
  }

  @Override
  public SensorDescriptor processesFilesIndependently() {
    this.processesFilesIndependently = true;
    return this;
  }

  @Override
  public SensorDescriptor processesHiddenFiles() {
    this.processesHiddenFiles = true;
    return this;
  }
}
