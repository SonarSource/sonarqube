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
package org.sonar.db.rule;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleQuery;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.es.RuleExtensionId;
import org.sonar.db.organization.OrganizationDto;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeInputsWithoutOutput;
import static org.sonar.db.DatabaseUtils.executeLargeUpdates;

public class RuleDao implements Dao {

  public Optional<RuleDto> selectByKey(DbSession session, String organizationUuid, RuleKey key) {
    RuleDto res = mapper(session).selectByKey(organizationUuid, key);
    ensureOrganizationIsSet(organizationUuid, res);
    return ofNullable(res);
  }

  public RuleDto selectOrFailByKey(DbSession session, RuleKey key) {
    RuleDefinitionDto ruleDefinitionDto = selectOrFailDefinitionByKey(session, key);
    return new RuleDto(ruleDefinitionDto, new RuleMetadataDto());
  }

  public Optional<RuleDefinitionDto> selectDefinitionByKey(DbSession session, RuleKey key) {
    return ofNullable(mapper(session).selectDefinitionByKey(key));
  }

  public Optional<RuleMetadataDto> selectMetadataByKey(DbSession session, RuleKey key, String organizationUuid) {
    return ofNullable(mapper(session).selectMetadataByKey(key, organizationUuid));
  }

  public RuleDto selectOrFailByKey(DbSession session, OrganizationDto organization, RuleKey key) {
    RuleDto rule = mapper(session).selectByKey(organization.getUuid(), key);
    if (rule == null) {
      throw new RowNotFoundException(String.format("Rule with key '%s' does not exist", key));
    }
    ensureOrganizationIsSet(organization.getUuid(), rule);
    return rule;
  }

  public RuleDefinitionDto selectOrFailDefinitionByKey(DbSession session, RuleKey key) {
    RuleDefinitionDto rule = mapper(session).selectDefinitionByKey(key);
    if (rule == null) {
      throw new RowNotFoundException(String.format("Rule with key '%s' does not exist", key));
    }
    return rule;
  }

  public Optional<RuleDto> selectById(long id, String organizationUuid, DbSession session) {
    RuleDto res = mapper(session).selectById(organizationUuid, id);
    ensureOrganizationIsSet(organizationUuid, res);
    return ofNullable(res);
  }

  public Optional<RuleDefinitionDto> selectDefinitionById(long id, DbSession session) {
    return ofNullable(mapper(session).selectDefinitionById(id));
  }

  public List<RuleDto> selectByIds(DbSession session, String organizationUuid, List<Integer> ids) {
    if (ids.isEmpty()) {
      return emptyList();
    }
    return ensureOrganizationIsSet(
      organizationUuid,
      executeLargeInputs(ids, chunk -> mapper(session).selectByIds(organizationUuid, chunk)));
  }

  public List<RuleDefinitionDto> selectDefinitionByIds(DbSession session, Collection<Integer> ids) {
    if (ids.isEmpty()) {
      return emptyList();
    }
    return executeLargeInputs(ids, mapper(session)::selectDefinitionByIds);
  }

  public List<RuleDto> selectByKeys(DbSession session, OrganizationDto organization, Collection<RuleKey> keys) {
    if (keys.isEmpty()) {
      return emptyList();
    }
    return ensureOrganizationIsSet(organization.getUuid(),
      executeLargeInputs(keys, chunk -> mapper(session).selectByKeys(organization.getUuid(), chunk)));
  }

  public List<RuleDto> selectByKeys(DbSession session, String organizationUuid, Collection<RuleKey> keys) {
    if (keys.isEmpty()) {
      return emptyList();
    }
    return ensureOrganizationIsSet(organizationUuid,
      executeLargeInputs(keys, chunk -> mapper(session).selectByKeys(organizationUuid, chunk)));
  }

  public List<RuleDefinitionDto> selectDefinitionByKeys(DbSession session, Collection<RuleKey> keys) {
    if (keys.isEmpty()) {
      return emptyList();
    }
    return executeLargeInputs(keys, mapper(session)::selectDefinitionByKeys);
  }

  public void selectEnabled(DbSession session, ResultHandler<RuleDefinitionDto> resultHandler) {
    mapper(session).selectEnabled(resultHandler);
  }

  public List<RuleDto> selectAll(DbSession session, String organizationUuid) {
    return ensureOrganizationIsSet(organizationUuid, mapper(session).selectAll(organizationUuid));
  }

  public List<RuleDefinitionDto> selectAllDefinitions(DbSession session) {
    return mapper(session).selectAllDefinitions();
  }

  public List<RuleDto> selectByTypeAndLanguages(DbSession session, String organizationUuid, List<Integer> types, List<String> languages) {
    return ensureOrganizationIsSet(organizationUuid,
      executeLargeInputs(languages, chunk -> mapper(session).selectByTypeAndLanguages(organizationUuid, types, chunk)));
  }

  public List<RuleDto> selectByQuery(DbSession session, String organizationUuid, RuleQuery ruleQuery) {
    return ensureOrganizationIsSet(organizationUuid, mapper(session).selectByQuery(organizationUuid, ruleQuery));
  }

  private static void ensureOrganizationIsSet(String organizationUuid, @Nullable RuleDto res) {
    if (res != null) {
      res.setOrganizationUuid(organizationUuid);
    }
  }

  private static List<RuleDto> ensureOrganizationIsSet(String organizationUuid, List<RuleDto> res) {
    res.forEach(dto -> ensureOrganizationIsSet(organizationUuid, dto));
    return res;
  }

  public void insert(DbSession session, RuleDefinitionDto dto) {
    mapper(session).insertDefinition(dto);
  }

  public void insert(DbSession session, RuleMetadataDto dto) {
    mapper(session).insertMetadata(dto);
  }

  public void update(DbSession session, RuleDefinitionDto dto) {
    mapper(session).updateDefinition(dto);
  }

  public void insertOrUpdate(DbSession session, RuleMetadataDto dto) {
    if (mapper(session).countMetadata(dto) > 0) {
      mapper(session).updateMetadata(dto);
    } else {
      mapper(session).insertMetadata(dto);
    }
  }

  public void scrollIndexingRuleExtensionsByIds(DbSession dbSession, Collection<RuleExtensionId> ruleExtensionIds, Consumer<RuleExtensionForIndexingDto> consumer) {
    RuleMapper mapper = mapper(dbSession);

    executeLargeInputsWithoutOutput(ruleExtensionIds,
      pageOfRuleExtensionIds -> mapper
        .selectIndexingRuleExtensionsByIds(pageOfRuleExtensionIds)
        .forEach(consumer));
  }

  public void scrollIndexingRuleExtensions(DbSession dbSession, Consumer<RuleExtensionForIndexingDto> consumer) {
    mapper(dbSession).scrollIndexingRuleExtensions(context -> {
      RuleExtensionForIndexingDto dto = context.getResultObject();
      consumer.accept(dto);
    });
  }

  public void scrollIndexingRulesByKeys(DbSession dbSession, Collection<Integer> ruleIds, Consumer<RuleForIndexingDto> consumer) {
    RuleMapper mapper = mapper(dbSession);

    executeLargeInputsWithoutOutput(ruleIds,
      pageOfRuleIds -> mapper
        .selectIndexingRulesByIds(pageOfRuleIds)
        .forEach(consumer));
  }

  public void scrollIndexingRules(DbSession dbSession, Consumer<RuleForIndexingDto> consumer) {
    mapper(dbSession).scrollIndexingRules(context -> {
      RuleForIndexingDto dto = context.getResultObject();
      consumer.accept(dto);
    });
  }

  private static RuleMapper mapper(DbSession session) {
    return session.getMapper(RuleMapper.class);
  }

  /**
   * RuleParams
   */

  public List<RuleParamDto> selectRuleParamsByRuleKey(DbSession session, RuleKey key) {
    return mapper(session).selectParamsByRuleKey(key);
  }

  public List<RuleParamDto> selectRuleParamsByRuleKeys(DbSession session, Collection<RuleKey> ruleKeys) {
    return executeLargeInputs(ruleKeys, mapper(session)::selectParamsByRuleKeys);
  }

  public List<RuleParamDto> selectRuleParamsByRuleIds(DbSession dbSession, Collection<Integer> ruleIds) {
    return executeLargeInputs(ruleIds, mapper(dbSession)::selectParamsByRuleIds);
  }

  public void insertRuleParam(DbSession session, RuleDefinitionDto rule, RuleParamDto param) {
    checkNotNull(rule.getId(), "Rule id must be set");
    param.setRuleId(rule.getId());
    mapper(session).insertParameter(param);
  }

  public RuleParamDto updateRuleParam(DbSession session, RuleDefinitionDto rule, RuleParamDto param) {
    checkNotNull(rule.getId(), "Rule id must be set");
    checkNotNull(param.getId(), "Rule parameter is not yet persisted must be set");
    param.setRuleId(rule.getId());
    mapper(session).updateParameter(param);
    return param;
  }

  public void deleteRuleParam(DbSession session, int ruleParameterId) {
    mapper(session).deleteParameter(ruleParameterId);
  }

  public Set<DeprecatedRuleKeyDto> selectAllDeprecatedRuleKeys(DbSession session) {
    return mapper(session).selectAllDeprecatedRuleKeys();
  }

  public void deleteDeprecatedRuleKeys(DbSession dbSession, Collection<String> uuids) {
    if (uuids.isEmpty()) {
      return;
    }
    executeLargeUpdates(uuids, mapper(dbSession)::deleteDeprecatedRuleKeys);
  }

  public void insert(DbSession dbSession, DeprecatedRuleKeyDto deprecatedRuleKey) {
    mapper(dbSession).insertDeprecatedRuleKey(deprecatedRuleKey);
  }
}
