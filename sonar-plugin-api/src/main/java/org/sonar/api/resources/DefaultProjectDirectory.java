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
package org.sonar.api.resources;

import com.google.common.collect.Lists;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class DefaultProjectDirectory implements ProjectDirectory {

  private String nature;
  private File location;
  private File outputLocation;
  private List<String> inclusionPatterns;
  private List<String> exclusionPatterns;

  public String getNature() {
    return nature;
  }

  public DefaultProjectDirectory setNature(String nature) {
    this.nature = nature;
    return this;
  }

  public File getLocation() {
    return location;
  }

  public DefaultProjectDirectory setLocation(File location) {
    this.location = location;
    return this;
  }

  public File getOutputLocation() {
    return outputLocation;
  }

  public DefaultProjectDirectory setOutputLocation(File outputLocation) {
    this.outputLocation = outputLocation;
    return this;
  }

  public List<String> getInclusionPatterns() {
    if (inclusionPatterns == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(inclusionPatterns);
  }

  /**
   * @param pattern Ant-like inclusion pattern
   */
  public DefaultProjectDirectory addInclusionPattern(String pattern) {
    if (inclusionPatterns == null) {
      inclusionPatterns = Lists.newArrayList();
    }
    inclusionPatterns.add(pattern);
    return this;
  }

  public List<String> getExclusionPatterns() {
    if (exclusionPatterns == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(exclusionPatterns);
  }

  /**
   * @param pattern Ant-like exclusion pattern
   */
  public DefaultProjectDirectory addExclusionPattern(String pattern) {
    if (exclusionPatterns == null) {
      exclusionPatterns = Lists.newArrayList();
    }
    exclusionPatterns.add(pattern);
    return this;
  }
}
