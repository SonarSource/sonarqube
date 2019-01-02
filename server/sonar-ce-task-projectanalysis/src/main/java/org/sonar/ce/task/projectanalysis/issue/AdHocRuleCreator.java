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
package org.sonar.ce.task.projectanalysis.issue;

import com.google.common.base.Preconditions;
import java.util.Objects;
import java.util.Optional;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleMetadataDto;
import org.sonar.server.rule.index.RuleIndexer;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.substring;
import static org.sonar.api.rule.RuleStatus.READY;
import static org.sonar.db.rule.RuleDto.Scope.ALL;

public class AdHocRuleCreator {

  private static final int MAX_LENGTH_AD_HOC_NAME = 200;
  private static final int MAX_LENGTH_AD_HOC_DESC = 16_777_215;
  private final DbClient dbClient;
  private final System2 system2;
  private final RuleIndexer ruleIndexer;

  public AdHocRuleCreator(DbClient dbClient, System2 system2, RuleIndexer ruleIndexer) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.ruleIndexer = ruleIndexer;
  }

  /**
   * Persists a new add hoc rule in the DB and indexes it.
   * @return the rule that was inserted in the DB, which <b>includes the generated ID</b>. 
   */
  public RuleDto persistAndIndex(DbSession dbSession, NewAdHocRule adHoc, OrganizationDto organizationDto) {
    RuleDao dao = dbClient.ruleDao();
    Optional<RuleDto> existingRuleDtoOpt = dao.selectByKey(dbSession, organizationDto, adHoc.getKey());
    RuleMetadataDto metadata;
    long now = system2.now();
    if (!existingRuleDtoOpt.isPresent()) {
      RuleDefinitionDto dto = new RuleDefinitionDto()
        .setRuleKey(adHoc.getKey())
        .setIsExternal(true)
        .setIsAdHoc(true)
        .setName(adHoc.getEngineId() + ":" + adHoc.getRuleId())
        .setScope(ALL)
        .setStatus(READY)
        .setCreatedAt(now)
        .setUpdatedAt(now);
      dao.insert(dbSession, dto);
      metadata = new RuleMetadataDto()
        .setRuleId(dto.getId())
        .setOrganizationUuid(organizationDto.getUuid());
    } else {
      // No need to update the rule, only org specific metadata
      RuleDto ruleDto = existingRuleDtoOpt.get();
      Preconditions.checkState(ruleDto.isExternal() && ruleDto.isAdHoc());
      metadata = ruleDto.getMetadata();
    }

    if (adHoc.hasDetails()) {
      boolean changed = false;
      if (!Objects.equals(metadata.getAdHocName(), adHoc.getName())) {
        metadata.setAdHocName(substring(adHoc.getName(), 0, MAX_LENGTH_AD_HOC_NAME));
        changed = true;
      }
      if (!Objects.equals(metadata.getAdHocDescription(), adHoc.getDescription())) {
        metadata.setAdHocDescription(substring(adHoc.getDescription(), 0, MAX_LENGTH_AD_HOC_DESC));
        changed = true;
      }
      if (!Objects.equals(metadata.getAdHocSeverity(), adHoc.getSeverity())) {
        metadata.setAdHocSeverity(adHoc.getSeverity());
        changed = true;
      }
      RuleType ruleType = requireNonNull(adHoc.getRuleType(), "Rule type should not be null");
      if (!Objects.equals(metadata.getAdHocType(), ruleType.getDbConstant())) {
        metadata.setAdHocType(ruleType);
        changed = true;
      }
      if (changed) {
        metadata.setUpdatedAt(now);
        metadata.setCreatedAt(now);
        dao.insertOrUpdate(dbSession, metadata);
      }

    }

    RuleDto ruleDto = dao.selectOrFailByKey(dbSession, organizationDto, adHoc.getKey());
    ruleIndexer.commitAndIndex(dbSession, ruleDto.getId());
    return ruleDto;
  }

}
