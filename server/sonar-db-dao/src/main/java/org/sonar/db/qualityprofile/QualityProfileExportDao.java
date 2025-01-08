/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.qualityprofile;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class QualityProfileExportDao implements Dao {

  public List<ExportRuleDto> selectRulesByProfile(DbSession dbSession, QProfileDto profile) {
    List<ExportRuleDto> exportRules = mapper(dbSession).selectByProfileUuid(profile.getKee());

    Map<String, ExportRuleDto> exportRulesByUuid = exportRules.stream().collect(Collectors.toMap(ExportRuleDto::getActiveRuleUuid, x -> x));
    Map<String, List<ExportRuleParamDto>> rulesParams = selectParamsByActiveRuleUuids(dbSession, exportRulesByUuid.keySet());

    rulesParams.forEach((uuid, rules) -> exportRulesByUuid.get(uuid).setParams(rules));
    return exportRules;
  }

  private static Map<String, List<ExportRuleParamDto>> selectParamsByActiveRuleUuids(DbSession dbSession, Collection<String> activeRuleUuids) {
    return executeLargeInputs(activeRuleUuids, uuids -> mapper(dbSession).selectParamsByActiveRuleUuids(uuids))
      .stream()
      .collect(Collectors.groupingBy(ExportRuleParamDto::getActiveRuleUuid));
  }

  private static QualityProfileExportMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(QualityProfileExportMapper.class);
  }
}
