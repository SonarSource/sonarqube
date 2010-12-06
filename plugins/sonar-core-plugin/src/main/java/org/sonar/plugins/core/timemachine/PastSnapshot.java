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
package org.sonar.plugins.core.timemachine;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.api.database.model.Snapshot;

import java.util.Date;

public class PastSnapshot {

  private int index;
  private String mode, modeParameter;
  private Snapshot projectSnapshot;

  public PastSnapshot(int index, String mode, Snapshot projectSnapshot) {
    this.index = index;
    this.mode = mode;
    this.projectSnapshot = projectSnapshot;
  }

  public int getIndex() {
    return index;
  }

  public Snapshot getProjectSnapshot() {
    return projectSnapshot;
  }

  public Date getDate() {
    return projectSnapshot.getCreatedAt();
  }

  public String getMode() {
    return mode;
  }

  public String getModeParameter() {
    return modeParameter;
  }

  public PastSnapshot setModeParameter(String s) {
    this.modeParameter = s;
    return this;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }
}
