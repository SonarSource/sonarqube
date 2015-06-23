/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.component;

import java.util.List;
import org.sonar.server.computation.step.FillComponentsStep;

public interface Component {
  enum Type {
    PROJECT(0), MODULE(1), DIRECTORY(2), FILE(3);

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
  }

  Type getType();

  /**
   * Return the component uuid only when {@link FillComponentsStep} has been executed, otherwise it will throw an exception.
   */
  String getUuid();

  /**
   * Return the component key only when {@link FillComponentsStep} has been executed, otherwise it will throw an exception.
   */
  String getKey();

  /**
   * The component ref in the batch report.
   */
  int getRef();

  /**
   * The component name as defined in the batch report.
   */
  String getName();

  /**
   * The project or module version as defined in the batch report.
   */
  String getVersion();

  /**
   * The attributes of the Component if it's type is File.
   *
   * @throws IllegalStateException if the Component's type is not {@link Type#FILE}
   */
  FileAttributes getFileAttributes();

  List<Component> getChildren();

}
