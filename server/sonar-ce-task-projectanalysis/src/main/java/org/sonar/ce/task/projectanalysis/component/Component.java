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
package org.sonar.ce.task.projectanalysis.component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;

public interface Component {
  enum Type {
    PROJECT(0), DIRECTORY(2), FILE(3), VIEW(0), SUBVIEW(1), PROJECT_VIEW(2);

    private static final Set<Type> REPORT_TYPES = EnumSet.of(PROJECT, DIRECTORY, FILE);
    private static final Set<Type> VIEWS_TYPES = EnumSet.of(VIEW, SUBVIEW, PROJECT_VIEW);

    private final int depth;

    Type(int depth) {
      this.depth = depth;
    }

    public int getDepth() {
      return depth;
    }

    public boolean isDeeperThan(Type otherType) {
      return this.getDepth() > otherType.getDepth();
    }

    public boolean isHigherThan(Type otherType) {
      return this.getDepth() < otherType.getDepth();
    }

    public boolean isReportType() {
      return REPORT_TYPES.contains(this);
    }

    public boolean isViewsType() {
      return VIEWS_TYPES.contains(this);
    }
  }

  /**
   * On a long-living branch, CHANGED and ADDED are relative to the previous analysis.
   * On a short-living branch and pull request, these are relative to the base branch.
   */
  enum Status {
    UNAVAILABLE, SAME, CHANGED, ADDED
  }

  Type getType();

  Status getStatus();

  /**
   * Returns the component uuid
   */
  String getUuid();

  /**
   * Returns the component key <b>as defined in database</b>
   * It may differ from keys listed in scanner report
   * when analyzing a branch.
   */
  String getDbKey();

  /**
   * Returns the key as it will be displayed in the ui.
   * If legacy branch feature is used, the key will contain the branch name
   * If new branch feature is used, the key will not contain the branch name
   */
  String getKey();

  /**
   * The component long name. For files and directories this is the project relative path.
   */
  String getName();

  /**
   * The component short name. For files and directories this is the parent relative path (ie filename for files). For projects and view this is the same as {@link #getName()}
   */
  String getShortName();

  /**
   * The optional description of the component.
   */
  @CheckForNull
  String getDescription();

  List<Component> getChildren();

  /**
   * Returns the attributes specific to components of type {@link Type#PROJECT}.
   *
   * @throws IllegalStateException when the component's type is not {@link Type#PROJECT}.
   */
  ProjectAttributes getProjectAttributes();

  /**
   * Returns the attributes specific to components of type {@link Type#PROJECT}, {@link Type#MODULE},
   * {@link Type#DIRECTORY} or {@link Type#FILE}.
   *
   * @throws IllegalStateException when the component's type is neither {@link Type#PROJECT}, {@link Type#MODULE},
   *         {@link Type#DIRECTORY} nor {@link Type#FILE}.
   */
  ReportAttributes getReportAttributes();

  /**
   * The attributes of the Component if it's type is File.
   *
   * @throws IllegalStateException if the Component's type is not {@link Type#FILE}
   */
  FileAttributes getFileAttributes();

  /**
   * The attributes of the Component if it's type is {@link Type#PROJECT_VIEW}.
   *
   * @throws IllegalStateException if the Component's type is not {@link Type#PROJECT_VIEW}
   */
  ProjectViewAttributes getProjectViewAttributes();

  /**
   * The attributes of the Component if it's type is {@link Type#SUBVIEW}.
   *
   * @throws IllegalStateException if the Component's type is not {@link Type#SUBVIEW}
   */
  SubViewAttributes getSubViewAttributes();

  /**
   * The attributes of the Component if it's type is {@link Type#VIEW}.
   *
   * @throws IllegalStateException if the Component's type is not {@link Type#VIEW}
   */
  ViewAttributes getViewAttributes();
}
