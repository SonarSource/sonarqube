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
package org.sonar.db.alm;

import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;

public interface AlmAppInstallMapper {

  @CheckForNull
  AlmAppInstallDto selectByOrganizationAlmId(@Param("alm") String alm, @Param("organizationAlmId") String organizationAlmId);

  @CheckForNull
  AlmAppInstallDto selectByInstallationId(@Param("alm") String alm, @Param("installId") String installId);

  @CheckForNull
  AlmAppInstallDto selectByUuid(@Param("uuid") String uuid);

  @CheckForNull
  AlmAppInstallDto selectByOrganizationUuid(@Param("organizationUuid") String organizationUuid);

  List<AlmAppInstallDto> selectUnboundByUserExternalId(@Param("userExternalId") String userExternalId);

  void insert(@Param("uuid") String uuid, @Param("alm") String alm, @Param("organizationAlmId") String organizationAlmId,
    @Nullable @Param("isOwnerUser") Boolean isOwnerUser, @Param("installId") String installId, @Nullable @Param("userExternalId") String userExternalId, @Param("now") long now);

  int update(@Param("alm") String alm, @Param("organizationAlmId") String organizationAlmId,
    @Nullable @Param("isOwnerUser") Boolean isOwnerUser, @Param("installId") String installId, @Nullable @Param("userExternalId") String userExternalId, @Param("now") long now);

  void delete(@Param("alm") String alm, @Param("organizationAlmId") String organizationAlmId);

  List<AlmAppInstallDto> selectByOrganizationUuids(@Param("organizationUuids") Collection<String> organizationUuids);
}
