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
package org.sonar.db.qualitygate;

import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;

public interface QualityGateMapper {

  void insertQualityGate(QualityGateDto qualityGate);

  void insertOrgQualityGate(@Param("uuid") String uuid, @Param("organizationUuid") String organizationUuid, @Param("qualityGateUuid") String qualityGateUuuid);

  List<QualityGateDto> selectAll(@Param("organizationUuid") String organizationUuid);

  QualityGateDto selectByName(String name);

  QualityGateDto selectBuiltIn();

  void delete(String uuid);

  void deleteByUuids(@Param("uuids") Collection<String> uuids);

  void update(QualityGateDto qGate);

  void deleteOrgQualityGatesByQualityGateUuid(String uuid);

  void ensureOneBuiltInQualityGate(String builtInQualityName);

  void selectQualityGateFindings(String qualityGateUuid, ResultHandler<QualityGateFindingDto> handler);

  QualityGateDto selectByUuid(String uuid);

  QualityGateDto selectByUuidAndOrganization(@Param("qualityGateUuid") String qualityGateUuid, @Param("organizationUuid") String organizationUuid);

  QualityGateDto selectByNameAndOrganization(@Param("name") String name, @Param("organizationUuid") String organizationUuid);

  QualityGateDto selectDefault(@Param("organizationUuid") String organizationUuid);

  QualityGateDto selectByProjectUuid(@Param("projectUuid") String projectUuid);
}
