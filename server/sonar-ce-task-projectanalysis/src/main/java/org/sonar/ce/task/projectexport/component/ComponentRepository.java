/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectexport.component;

import java.util.Set;
import org.sonar.db.component.ComponentQualifiers;

/**
 * Holds the references of components which are present in the dump.
 */
public interface ComponentRepository {

  /**
   * @throws IllegalStateException if there is no ref for the specified Uuid in the repository
   */
  long getRef(String uuid);

  /**
   * Uuids of the components of type FILE (ie. Qualifiers = {@link ComponentQualifiers#FILE}) known to
   * the repository.
   */
  Set<String> getFileUuids();
}
