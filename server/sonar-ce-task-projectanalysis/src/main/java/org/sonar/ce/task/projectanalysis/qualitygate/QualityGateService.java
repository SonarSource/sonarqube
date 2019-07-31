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
package org.sonar.ce.task.projectanalysis.qualitygate;

import java.util.Optional;
import org.sonar.ce.task.projectanalysis.analysis.Organization;
import org.sonar.server.project.Project;

public interface QualityGateService {

  /**
   * Retrieve the {@link QualityGate} from the database with the specified id, if it exists.
   */
  Optional<QualityGate> findById(long id);

  /**
   * Retrieve the {@link QualityGate} from the database using organization.
   * @throws IllegalStateException if database is corrupted and default gate can't be found.
   */
  QualityGate findDefaultQualityGate(Organization organizationDto);

  /**
   * Retrieve the {@link QualityGate} from the database associated with project.
   */
  Optional<QualityGate> findQualityGate(Project project);

}
