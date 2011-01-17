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
package org.sonar.batch;

import org.sonar.api.project.ProjectDirectory;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class DefaultProjectDirectory implements ProjectDirectory {

  private Kind kind;
  private File location;
  private File outputLocation;

  public Kind getKind() {
    return kind;
  }

  public void setKind(Kind kind) {
    this.kind = kind;
  }

  public File getLocation() {
    return location;
  }

  public void setLocation(File location) {
    this.location = location;
  }

  public File getOutputLocation() {
    return outputLocation;
  }

  public void setOutputLocation(File outputLocation) {
    this.outputLocation = outputLocation;
  }

  public List<String> getInclusionPatterns() {
    // TODO see example in ProjectDirectory
    return Collections.emptyList();
  }

  public List<String> getExclusionPatterns() {
    // TODO see example in ProjectDirectory
    return Collections.emptyList();
  }

}
