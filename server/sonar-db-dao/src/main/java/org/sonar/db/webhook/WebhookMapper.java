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
package org.sonar.db.webhook;

import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;

public interface WebhookMapper {

  @CheckForNull
  WebhookDto selectByUuid(@Param("webhookUuid") String webhookUuid);

  List<WebhookDto> selectForProjectUuidOrderedByName(@Param("projectUuid") String projectUuid);

  List<WebhookDto> selectForOrganizationUuidOrderedByName(@Param("organizationUuid") String organizationUuid);

  void insert(WebhookDto dto);

  void update(WebhookDto dto);

  void delete(@Param("uuid") String uuid);

  void deleteForOrganizationUuid(@Param("organizationUuid") String organizationUuid);

  void deleteForProjectUuid(@Param("projectUuid") String organizationUuid);
}
