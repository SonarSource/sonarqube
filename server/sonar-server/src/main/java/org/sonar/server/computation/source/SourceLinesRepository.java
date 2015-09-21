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

package org.sonar.server.computation.source;

import org.sonar.core.util.CloseableIterator;
import org.sonar.server.computation.component.Component;

public interface SourceLinesRepository {

  /**
   * Return lines from a given component. If file sources is not in the report then we read it from the database.
   *
   * @throws NullPointerException if argument is {@code null}
   * @throws IllegalArgumentException if component is not a {@link org.sonar.server.computation.component.Component.Type#FILE}
   * @throws IllegalStateException if the file has no source code in the report and in the database
   */
  CloseableIterator<String> readLines(Component component);
}
