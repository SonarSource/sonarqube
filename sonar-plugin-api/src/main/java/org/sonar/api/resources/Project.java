/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.resources;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.component.Component;
import org.sonar.api.scan.filesystem.PathResolver;

/**
 * @since 1.10
 * @deprecated since 5.6 replaced by {@link InputModule}.
 */
@Deprecated
public class Project extends Resource implements Component {
  private final ProjectDefinition definition;

  public Project(DefaultInputModule module) {
    this(module.definition());
  }

  public Project(ProjectDefinition definition) {
    this.definition = definition;
    this.setKey(definition.getKey());
    this.setEffectiveKey(definition.getKeyWithBranch());
  }

  public ProjectDefinition definition() {
    return definition;
  }

  @Override
  public String key() {
    return definition.getKey();
  }

  @Override
  public String path() {
    ProjectDefinition parent = definition.getParent();
    if (parent == null) {
      return null;
    }
    return new PathResolver().relativePath(parent.getBaseDir(), definition.getBaseDir());
  }

  public String getBranch() {
    return definition.getBranch();
  }

  @CheckForNull
  public String getOriginalName() {
    String name = definition.getOriginalName();
    if (StringUtils.isNotEmpty(getBranch())) {
      name = name + " " + getBranch();
    }
    return name;
  }

  java.io.File getBaseDir() {
    return definition.getBaseDir();
  }

  @Override
  public String name() {
    String name = definition.getName();
    if (StringUtils.isNotEmpty(getBranch())) {
      name = name + " " + getBranch();
    }
    return name;
  }

  @Override
  public String longName() {
    return definition.getName();
  }

  @Override
  public String qualifier() {
    return getParent() == null ? Qualifiers.PROJECT : Qualifiers.MODULE;
  }

  @Override
  public String getName() {
    return name();
  }

  public boolean isRoot() {
    return getParent() == null;
  }

  public Project getRoot() {
    return getParent() == null ? this : getParent().getRoot();
  }

  /**
   * @return whether the current project is a module
   */
  public boolean isModule() {
    return !isRoot();
  }

  @Override
  public String getLongName() {
    return longName();
  }

  @Override
  public String getDescription() {
    return definition.getDescription();
  }

  /** 
   * @deprecated since 4.2 use {@link org.sonar.api.batch.fs.FileSystem#languages()}
   */
  @Override
  public Language getLanguage() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getScope() {
    return Scopes.PROJECT;
  }

  @Override
  public String getQualifier() {
    return qualifier();
  }

  @Override
  public Project getParent() {
    ProjectDefinition parent = definition.getParent();
    if (parent == null) {
      return null;
    }
    return new Project(parent);
  }

  /**
   * @return the list of modules
   */
  public List<Project> getModules() {
    return definition.getSubProjects().stream()
      .map(Project::new)
      .collect(Collectors.toList());
  }

  @Override
  public boolean matchFilePattern(String antPattern) {
    return false;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("id", getId())
      .append("key", key())
      .append("qualifier", getQualifier())
      .toString();
  }

}
