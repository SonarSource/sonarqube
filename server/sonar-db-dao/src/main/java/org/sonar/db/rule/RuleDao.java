/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleQuery;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class RuleDao implements Dao {

  public Optional<RuleDto> selectByKey(DbSession session, RuleKey key) {
    return Optional.fromNullable(mapper(session).selectByKey(key));
  }

  public RuleDto selectOrFailByKey(DbSession session, RuleKey key) {
    RuleDto rule = mapper(session).selectByKey(key);
    if (rule == null) {
      throw new RowNotFoundException(String.format("Rule with key '%s' does not exist", key));
    }
    return rule;
  }

  /**
   * Retrieves a Rule by its id.
   *
   * Used by Views.
   */
  public Optional<RuleDto> selectById(long id, DbSession session) {
    return Optional.fromNullable(mapper(session).selectById(id));
  }

  public List<RuleDto> selectByIds(DbSession session, List<Integer> ids) {
    return executeLargeInputs(ids, mapper(session)::selectByIds);
  }

  /**
   * Select rules by keys, whatever their status. Returns an empty list
   * if the list of {@code keys} is empty, without any db round trip.
   */
  public List<RuleDto> selectByKeys(DbSession session, Collection<RuleKey> keys) {
    return executeLargeInputs(keys, mapper(session)::selectByKeys);
  }

  public void selectEnabled(DbSession session, ResultHandler resultHandler) {
    mapper(session).selectEnabled(resultHandler);
  }

  public List<RuleDto> selectAll(DbSession session) {
    return mapper(session).selectAll();
  }

  public List<RuleDto> selectByQuery(DbSession session, RuleQuery ruleQuery) {
    return mapper(session).selectByQuery(ruleQuery);
  }

  public void insert(DbSession session, RuleDto dto) {
    RuleDefinitionDto ruleDefinitionDto = definitionOf(dto);
    mapper(session).insert(ruleDefinitionDto);
    dto.setId(ruleDefinitionDto.getId());
    // FIXME it doesn't make sense to insert metadata when creating a new rule in table RULES unless it is a custom rule
    mapper(session).updateMetadata(metadataOf(dto));
  }

  public void update(DbSession session, RuleDto dto) {
    mapper(session).updateDefinition(definitionOf(dto).setId(dto.getId()));
    RuleMetadataDto ruleMetadata = metadataOf(dto);
    mapper(session).updateMetadata(ruleMetadata);
  }

  private static RuleMetadataDto metadataOf(RuleDto dto) {
    return new RuleMetadataDto()
        .setRuleId(dto.getId())
        .setNoteData(dto.getNoteData())
        .setNoteUserLogin(dto.getNoteUserLogin())
        .setNoteCreatedAt(dto.getNoteCreatedAt())
        .setNoteUpdatedAt(dto.getNoteUpdatedAt())
        .setRemediationFunction(dto.getRemediationFunction())
        .setRemediationGapMultiplier(dto.getRemediationGapMultiplier())
        .setRemediationBaseEffort(dto.getRemediationBaseEffort())
        .setTags(dto.getTags())
        .setUpdatedAt(dto.getUpdatedAt());
  }

  private static RuleDefinitionDto definitionOf(RuleDto dto) {
    return new RuleDefinitionDto()
      .setRepositoryKey(dto.getRepositoryKey())
      .setRuleKey(dto.getRuleKey())
      .setDescription(dto.getDescription())
      .setDescriptionFormat(dto.getDescriptionFormat())
      .setStatus(dto.getStatus())
      .setName(dto.getName())
      .setConfigKey(dto.getConfigKey())
      .setSeverity(dto.getSeverity())
      .setIsTemplate(dto.isTemplate())
      .setLanguage(dto.getLanguage())
      .setTemplateId(dto.getTemplateId())
      .setDefaultRemediationFunction(dto.getDefaultRemediationFunction())
      .setDefaultRemediationGapMultiplier(dto.getDefaultRemediationGapMultiplier())
      .setDefaultRemediationBaseEffort(dto.getDefaultRemediationBaseEffort())
      .setGapDescription(dto.getGapDescription())
      .setSystemTags(dto.getSystemTags())
      .setType(dto.getType())
      .setCreatedAt(dto.getCreatedAt())
      .setUpdatedAt(dto.getUpdatedAt());
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

  public List<RuleParamDto> selectRuleParamsByRuleKeys(DbSession session, List<RuleKey> ruleKeys) {
    return executeLargeInputs(ruleKeys, mapper(session)::selectParamsByRuleKeys);
  }

  public List<RuleParamDto> selectRuleParamsByRuleIds(DbSession dbSession, List<Integer> ruleIds) {
    return executeLargeInputs(ruleIds, mapper(dbSession)::selectParamsByRuleIds);
  }

  public void insertRuleParam(DbSession session, RuleDto rule, RuleParamDto param) {
    checkNotNull(rule.getId(), "Rule id must be set");
    param.setRuleId(rule.getId());
    mapper(session).insertParameter(param);
  }

  public RuleParamDto updateRuleParam(DbSession session, RuleDto rule, RuleParamDto param) {
    checkNotNull(rule.getId(), "Rule id must be set");
    checkNotNull(param.getId(), "Rule parameter is not yet persisted must be set");
    param.setRuleId(rule.getId());
    mapper(session).updateParameter(param);
    return param;
  }

  public void deleteRuleParam(DbSession session, int ruleParameterId) {
    mapper(session).deleteParameter(ruleParameterId);
  }

}
