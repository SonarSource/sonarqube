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
package org.sonar.db.qualitygate;

import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface QualityGateMapper {

  void insertQualityGate(QualityGateDto qualityGate);

  void insertOrgQualityGate(@Param("uuid") String uuid, @Param("organizationUuid") String organizationUuid, @Param("qualityGateUuid") String qualityGateUuuid);

  List<QualityGateDto> selectAll(@Param("organizationUuid") String organizationUuid);

  QualityGateDto selectByName(String name);

  QualityGateDto selectById(long id);

  QGateWithOrgDto selectByUuidAndOrganization(@Param("qualityGateUuid") String qualityGateUuid, @Param("organizationUuid") String organizationUuid);

  QGateWithOrgDto selectByNameAndOrganization(@Param("name") String name, @Param("organizationUuid") String organizationUuid);

  QGateWithOrgDto selectByIdAndOrganization(@Param("id") long id, @Param("organizationUuid") String organizationUuid);

  QGateWithOrgDto selectDefault(@Param("organizationUuid") String organizationUuid);

  QualityGateDto selectBuiltIn();

  void delete(String uuid);

  void deleteByUuids(@Param("uuids") Collection<String> uuids);

  void deleteOrgQualityGatesByQualityGateUuid(String uuid);

  void deleteOrgQualityGatesByOrganization(@Param("organizationUuid") String organizationUuid);

  void update(QualityGateDto qGate);

  void ensureOneBuiltInQualityGate(String builtInQualityName);

  QualityGateDto selectByUuid(String uuid);

  QualityGateDto selectByProjectUuid(@Param("projectUuid") String projectUuid);
}
