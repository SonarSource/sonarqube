/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.qualityprofile;

import java.util.Collection;
import javax.annotation.Nullable;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.builtin.QProfileName;

/**
 * Create, delete and set as default profile.
 */
public interface QProfileFactory {

  QProfileDto getOrCreateCustom(DbSession dbSession, OrganizationDto organization, QProfileName name);

  /**
   * Create the quality profile in DB with the specified name.
   *
   * @throws BadRequestException if a quality profile with the specified name already exists
   */
  QProfileDto checkAndCreateCustom(DbSession dbSession, OrganizationDto organization, QProfileName name);

  QProfileDto createCustom(DbSession dbSession, OrganizationDto organization, QProfileName name, @Nullable String parentKey);

  /**
   * Deletes the specified profiles from database and Elasticsearch.
   * All information related to custom profiles are deleted. Only association
   * with built-in profiles are deleted.
   * The profiles marked as "default" are deleted too. Deleting a parent profile
   * does not delete descendants if the latter are not listed.
   */
  void delete(DbSession dbSession, Collection<QProfileDto> profiles);
}
