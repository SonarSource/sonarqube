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
package org.sonar.api.batch.sensor.dependency;

import com.google.common.annotations.Beta;
import org.sonar.api.batch.fs.InputFile;

/**
 * Builder to create new Dependency.
 * Should not be implemented by client.
 * @since 5.1
 */
@Beta
public interface NewDependency {

  NewDependency from(InputFile from);

  NewDependency to(InputFile to);

  /**
   * Set the weight of the dependency. If not set default weight is 1.
   */
  NewDependency weight(int weight);

  /**
   * Save the dependency. It is not permitted so save several time a dependency between two same files.
   */
  void save();

}
