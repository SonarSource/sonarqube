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
package org.sonar.api.batch.fs.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputModule;

/**
 * @since 5.2
 */
@Immutable
public class DefaultInputModule extends DefaultInputComponent implements InputModule {
  private final File baseDir;
  private final File workDir;
  private final String name;
  private final String version;
  private final String originalName;
  private final String originalVersion;
  private final String description;
  private final String keyWithBranch;
  private final String branch;
  private final List<String> sources;
  private final List<String> tests;
  private final Map<String, String> properties;

  private final String moduleKey;
  private final ProjectDefinition definition;

  /**
   * For testing only!
   */
  public DefaultInputModule(String moduleKey) {
    this(ProjectDefinition.create().setKey(moduleKey), TestInputFileBuilder.nextBatchId());
  }
  
  /**
   * For testing only!
   */
  public DefaultInputModule(ProjectDefinition definition) {
    this(definition, TestInputFileBuilder.nextBatchId());
  }

  public DefaultInputModule(ProjectDefinition definition, int batchId) {
    super(batchId);
    this.baseDir = definition.getBaseDir();
    this.workDir = definition.getWorkDir();
    this.name = definition.getName();
    this.originalName = definition.getOriginalName();
    this.version = definition.getVersion();
    this.originalVersion = definition.getOriginalVersion();
    this.description = definition.getDescription();
    this.keyWithBranch = definition.getKeyWithBranch();
    this.branch = definition.getBranch();
    this.sources = Collections.unmodifiableList(new ArrayList<>(definition.sources()));
    this.tests = Collections.unmodifiableList(new ArrayList<>(definition.tests()));
    this.properties = Collections.unmodifiableMap(new HashMap<>(definition.properties()));

    this.definition = definition;
    this.moduleKey = definition.getKey();
  }

  /**
   * Module key without branch
   */
  @Override
  public String key() {
    return moduleKey;
  }

  @Override
  public boolean isFile() {
    return false;
  }

  public ProjectDefinition definition() {
    return definition;
  }
  
  public File getBaseDir() {
    return baseDir;
  }

  public File getWorkDir() {
    return workDir;
  }

  public String getKeyWithBranch() {
    return keyWithBranch;
  }

  @CheckForNull
  public String getBranch() {
    return branch;
  }

  public Map<String, String> properties() {
    return properties;
  }

  @CheckForNull
  public String getOriginalVersion() {
    return originalVersion;
  }

  public String getVersion() {
    return version;
  }

  @CheckForNull
  public String getOriginalName() {
    return originalName;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  /**
   * @return Source files and folders.
   */
  public List<String> sources() {
    return sources;
  }

  public List<String> tests() {
    return tests;
  }

}
