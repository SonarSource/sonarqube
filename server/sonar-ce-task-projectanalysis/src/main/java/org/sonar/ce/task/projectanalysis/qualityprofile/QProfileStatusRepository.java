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
package org.sonar.ce.task.projectanalysis.qualityprofile;

import java.util.Optional;

public interface QProfileStatusRepository {

  Optional<Status> get(String qpKey);

  enum Status {
    /**
     * the QP was used in the last analysis but not anymore in the current one.
     */
    REMOVED,
    /**
     * the QP was not used in the last analysis
     */
    ADDED,
    /**
     * the QP was used in the last and current analysis and a rule has changed
     */
    UPDATED,
    /**
     * neither the QP or a rule has changed since last analysis
     */
    UNCHANGED
  }
}
