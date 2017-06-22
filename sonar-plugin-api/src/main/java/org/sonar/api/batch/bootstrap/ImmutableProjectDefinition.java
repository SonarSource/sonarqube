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
package org.sonar.api.batch.bootstrap;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * @since 6.5
 */
@Immutable
public class ImmutableProjectDefinition {
  private ImmutableProjectDefinition parent;
  private List<ImmutableProjectDefinition> subProjects;

  private final File baseDir;
  private final File workDir;
  private final String name;
  private final String version;
  private final String originalName;
  private final String originalVersion;
  private final String description;
  private final String key;
  private final String keyWithBranch;
  private final String branch;
  private final List<String> sources;
  private final List<String> tests;
  private final Map<String, String> properties;

  /**
   * This constructor will also construct recursively parent and child modules for immutability.
   */
  public ImmutableProjectDefinition(ProjectDefinition builder) {
    this(builder, true);
  }

  private ImmutableProjectDefinition(ProjectDefinition builder, boolean buildHierarchy) {
    this.baseDir = builder.getBaseDir();
    this.workDir = builder.getWorkDir();
    this.name = builder.getName();
    this.originalName = builder.getOriginalName();
    this.version = builder.getVersion();
    this.originalVersion = builder.getOriginalVersion();
    this.description = builder.getDescription();
    this.keyWithBranch = builder.getKeyWithBranch();
    this.branch = builder.getBranch();
    this.sources = Collections.unmodifiableList(new ArrayList<>(builder.sources()));
    this.tests = Collections.unmodifiableList(new ArrayList<>(builder.tests()));
    this.key = builder.getKey();
    this.properties = Collections.unmodifiableMap(new HashMap<>(builder.properties()));

    if (buildHierarchy) {
      subProjects = Collections.unmodifiableList(builder.getSubProjects().stream()
        .map(proj -> createSubProject(proj, this))
        .collect(Collectors.toList()));
      parent = createParent(builder.getParent(), this);
    }
  }

  private static ImmutableProjectDefinition createSubProject(ProjectDefinition subProject, ImmutableProjectDefinition immutableParent) {
    ImmutableProjectDefinition immutableSubProject = new ImmutableProjectDefinition(subProject, false);
    immutableSubProject.parent = immutableParent;
    immutableSubProject.subProjects = subProject.getSubProjects().stream()
      .map(proj -> createSubProject(proj, immutableSubProject))
      .collect(Collectors.toList());
    return immutableSubProject;
  }

  @CheckForNull
  private static ImmutableProjectDefinition createParent(@Nullable ProjectDefinition parent, ImmutableProjectDefinition immutableSubProject) {
    if (parent == null) {
      return null;
    }
    ImmutableProjectDefinition immutableParent = new ImmutableProjectDefinition(parent, false);
    immutableParent.subProjects = new ArrayList<>();
    immutableParent.subProjects.add(immutableSubProject);
    immutableParent.subProjects.addAll(parent.getSubProjects().stream()
      .filter(proj -> !proj.getKey().equals(immutableSubProject.getKey()))
      .map(proj -> createSubProject(proj, immutableParent))
      .collect(Collectors.toList()));
    immutableParent.subProjects = Collections.unmodifiableList(immutableParent.subProjects);
    immutableParent.parent = createParent(parent.getParent(), immutableParent);
    return immutableParent;
  }

  public File getBaseDir() {
    return baseDir;
  }

  public File getWorkDir() {
    return workDir;
  }

  public String getKey() {
    return key;
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

  @CheckForNull
  public ImmutableProjectDefinition getParent() {
    return parent;
  }

  /**
   * @since 2.8
   */
  public List<ImmutableProjectDefinition> getSubProjects() {
    return subProjects;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ImmutableProjectDefinition that = (ImmutableProjectDefinition) o;
    return !((key != null) ? !key.equals(that.getKey()) : (that.getKey() != null));

  }

  @Override
  public int hashCode() {
    return key != null ? key.hashCode() : 0;
  }

}
