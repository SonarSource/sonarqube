/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.core.rule.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.rule.index.RuleIndexer;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.sonar.api.rule.RuleStatus.READY;
import static org.sonar.db.rule.RuleDto.Scope.ALL;

public class AdHocRuleCreator {

  private static final int MAX_LENGTH_AD_HOC_NAME = 200;
  private static final int MAX_LENGTH_AD_HOC_DESC = 16_777_215;
  private final DbClient dbClient;
  private final System2 system2;
  private final RuleIndexer ruleIndexer;
  private final UuidFactory uuidFactory;

  public AdHocRuleCreator(DbClient dbClient, System2 system2, RuleIndexer ruleIndexer, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.system2 = system2;
    this.ruleIndexer = ruleIndexer;
    this.uuidFactory = uuidFactory;
  }

  /**
   * Persists a new add hoc rule in the DB and indexes it.
   * @return the rule that was inserted in the DB, which <b>includes the generated ID</b>. 
   */
  public RuleDto persistAndIndex(DbSession dbSession, NewAdHocRule adHoc) {
    RuleDao dao = dbClient.ruleDao();
    long now = system2.now();

    RuleDto ruleDtoToUpdate = findOrCreateRuleDto(dbSession, adHoc, dao, now);

    boolean changed = false;
    if (adHoc.hasDetails()) {
      if (!Objects.equals(ruleDtoToUpdate.getAdHocName(), adHoc.getName())) {
        ruleDtoToUpdate.setAdHocName(substring(adHoc.getName(), 0, MAX_LENGTH_AD_HOC_NAME));
        changed = true;
      }
      if (!Objects.equals(ruleDtoToUpdate.getAdHocDescription(), adHoc.getDescription())) {
        ruleDtoToUpdate.setAdHocDescription(substring(adHoc.getDescription(), 0, MAX_LENGTH_AD_HOC_DESC));
        changed = true;
      }
      if (!Objects.equals(ruleDtoToUpdate.getAdHocSeverity(), adHoc.getSeverity())) {
        ruleDtoToUpdate.setAdHocSeverity(adHoc.getSeverity());
        changed = true;
      }
      RuleType ruleType = requireNonNull(adHoc.getRuleType(), "Rule type should not be null");
      if (!Objects.equals(ruleDtoToUpdate.getAdHocType(), ruleType.getDbConstant())) {
        ruleDtoToUpdate.setAdHocType(ruleType);
        changed = true;
      }
    }

    CleanCodeAttribute cleanCodeAttribute = adHoc.getCleanCodeAttribute();
    if (!Objects.equals(ruleDtoToUpdate.getCleanCodeAttribute(), cleanCodeAttribute)) {
      ruleDtoToUpdate.setCleanCodeAttribute(cleanCodeAttribute);
      changed = true;
    }

    Map<SoftwareQuality, Severity> currentImpacts = ruleDtoToUpdate.getDefaultImpacts().stream()
      .collect(Collectors.toMap(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity));
    if (!Objects.equals(currentImpacts, adHoc.getDefaultImpacts())) {
      ruleDtoToUpdate.replaceAllDefaultImpacts(adHoc.getDefaultImpacts().entrySet().stream()
        .map(i -> createImpactDto(i.getKey(), i.getValue()))
        .toList());
      changed = true;
    }

    if (changed) {
      ruleDtoToUpdate.setUpdatedAt(now);
      dao.update(dbSession, ruleDtoToUpdate);
    }

    RuleDto ruleDto = dao.selectOrFailByKey(dbSession, adHoc.getKey());
    ruleIndexer.commitAndIndex(dbSession, ruleDto.getUuid());
    return ruleDto;
  }

  private static ImpactDto createImpactDto(SoftwareQuality softwareQuality, Severity severity) {
    return new ImpactDto().setSoftwareQuality(softwareQuality).setSeverity(severity);
  }

  private RuleDto findOrCreateRuleDto(DbSession dbSession, NewAdHocRule adHoc, RuleDao dao, long now) {
    Optional<RuleDto> existingRuleDtoOpt = dbClient.ruleDao().selectByKey(dbSession, adHoc.getKey());
    if (existingRuleDtoOpt.isEmpty()) {
      RuleDto ruleDto = new RuleDto()
        .setUuid(uuidFactory.create())
        .setRuleKey(adHoc.getKey())
        .setIsExternal(true)
        .setIsAdHoc(true)
        .setName(adHoc.getEngineId() + ":" + adHoc.getRuleId())
        .setScope(ALL)
        .setStatus(READY)
        .setCreatedAt(now)
        .setUpdatedAt(now);
      dao.insert(dbSession, ruleDto);
      return ruleDto;
    } else {
      RuleDto ruleDto = existingRuleDtoOpt.get();
      Preconditions.checkState(ruleDto.isExternal() && ruleDto.isAdHoc());
      return ruleDto;
    }
  }

}
