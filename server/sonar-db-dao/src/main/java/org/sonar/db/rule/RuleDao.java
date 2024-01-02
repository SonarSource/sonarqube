/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleQuery;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeInputsWithoutOutput;
import static org.sonar.db.DatabaseUtils.executeLargeUpdates;

public class RuleDao implements Dao {

  private final UuidFactory uuidFactory;

  public RuleDao(UuidFactory uuidFactory) {
    this.uuidFactory = uuidFactory;
  }

  public Optional<RuleDto> selectByKey(DbSession session, RuleKey key) {
    return Optional.ofNullable(mapper(session).selectByKey(key));
  }

  public RuleDto selectOrFailByKey(DbSession session, RuleKey key) {
    return Optional.ofNullable(mapper(session).selectByKey(key))
      .orElseThrow(() -> new RowNotFoundException(String.format("Rule with key '%s' does not exist", key)));
  }

  public Optional<RuleDto> selectByUuid(String uuid, DbSession session) {
    return Optional.ofNullable(mapper(session).selectByUuid(uuid));
  }

  public List<RuleDto> selectByUuids(DbSession session, Collection<String> uuids) {
    if (uuids.isEmpty()) {
      return emptyList();
    }
    return executeLargeInputs(uuids, chunk -> mapper(session).selectByUuids(chunk));
  }

  public List<RuleDto> selectByKeys(DbSession session, Collection<RuleKey> keys) {
    if (keys.isEmpty()) {
      return emptyList();
    }
    return executeLargeInputs(keys, chunk -> mapper(session).selectByKeys(chunk));
  }

  public List<RuleDto> selectEnabled(DbSession session) {
    return mapper(session).selectEnabled();
  }

  public List<RuleDto> selectAll(DbSession session) {
    return mapper(session).selectAll();
  }

  public List<RuleDto> selectByTypeAndLanguages(DbSession session, List<Integer> types, List<String> languages) {
    return executeLargeInputs(languages, chunk -> mapper(session).selectByTypeAndLanguages(types, chunk));
  }

  public List<RuleDto> selectByLanguage(DbSession session, String language) {
    return mapper(session).selectByLanguage(language);
  }

  public List<RuleDto> selectByQuery(DbSession session, RuleQuery ruleQuery) {
    return mapper(session).selectByQuery(ruleQuery);
  }

  public void insert(DbSession session, RuleDto ruleDto) {
    checkNotNull(ruleDto.getUuid(), "RuleDto has no 'uuid'.");
    RuleMapper mapper = mapper(session);
    mapper.insertRule(ruleDto);
    updateRuleDescriptionSectionDtos(ruleDto, mapper);
  }

  public void update(DbSession session, RuleDto ruleDto) {
    RuleMapper mapper = mapper(session);
    mapper.updateRule(ruleDto);
    updateRuleDescriptionSectionDtos(ruleDto, mapper);
  }

  private static void updateRuleDescriptionSectionDtos(RuleDto ruleDto, RuleMapper mapper) {
    mapper.deleteRuleDescriptionSection(ruleDto.getUuid());
    insertRuleDescriptionSectionDtos(ruleDto, mapper);
  }

  private static void insertRuleDescriptionSectionDtos(RuleDto ruleDto, RuleMapper mapper) {
    ruleDto.getRuleDescriptionSectionDtos()
      .forEach(section -> mapper.insertRuleDescriptionSection(ruleDto.getUuid(), section));
  }

  public void scrollIndexingRuleExtensionsByIds(DbSession dbSession, Collection<String> ruleExtensionIds, Consumer<RuleExtensionForIndexingDto> consumer) {
    RuleMapper mapper = mapper(dbSession);

    executeLargeInputsWithoutOutput(ruleExtensionIds,
      pageOfRuleExtensionIds -> mapper
        .selectIndexingRuleExtensionsByIds(pageOfRuleExtensionIds)
        .forEach(consumer));
  }

  public void selectIndexingRulesByKeys(DbSession dbSession, Collection<String> ruleUuids, Consumer<RuleForIndexingDto> consumer) {
    RuleMapper mapper = mapper(dbSession);

    executeLargeInputsWithoutOutput(ruleUuids,
      pageOfRuleUuids -> {
        List<RuleDto> ruleDtos = mapper.selectByUuids(pageOfRuleUuids);
        processRuleDtos(ruleDtos, consumer, mapper);
      });
  }

  public void selectIndexingRules(DbSession dbSession, Consumer<RuleForIndexingDto> consumer) {
    RuleMapper mapper = mapper(dbSession);
    executeLargeInputsWithoutOutput(mapper.selectAll(),
      ruleDtos -> processRuleDtos(ruleDtos, consumer, mapper));
  }

  private static RuleForIndexingDto toRuleForIndexingDto(RuleDto r, Map<String, RuleDto> templateDtos) {
    RuleForIndexingDto ruleForIndexingDto = RuleForIndexingDto.fromRuleDto(r);
    if (templateDtos.containsKey(r.getTemplateUuid())) {
      ruleForIndexingDto.setTemplateRuleKey(templateDtos.get(r.getTemplateUuid()).getRuleKey());
      ruleForIndexingDto.setTemplateRepository(templateDtos.get(r.getTemplateUuid()).getRepositoryKey());
    }
    return ruleForIndexingDto;
  }

  private static void processRuleDtos(List<RuleDto> ruleDtos, Consumer<RuleForIndexingDto> consumer, RuleMapper mapper) {
    List<String> templateRuleUuids = ruleDtos.stream()
      .map(RuleDto::getTemplateUuid)
      .filter(Objects::nonNull)
      .toList();

    Map<String, RuleDto> templateDtos = findTemplateDtos(mapper, templateRuleUuids);
    ruleDtos.stream().map(r -> toRuleForIndexingDto(r, templateDtos)).forEach(consumer);
  }

  private static Map<String, RuleDto> findTemplateDtos(RuleMapper mapper, List<String> templateRuleUuids) {
    if (!templateRuleUuids.isEmpty()) {
      return mapper.selectByUuids(templateRuleUuids).stream().collect(toMap(RuleDto::getUuid, Function.identity()));
    }else{
      return Collections.emptyMap();
    }
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

  public List<RuleParamDto> selectAllRuleParams(DbSession session) {
    return mapper(session).selectAllRuleParams();
  }

  public List<RuleParamDto> selectRuleParamsByRuleUuids(DbSession dbSession, Collection<String> ruleUuids) {
    return executeLargeInputs(ruleUuids, mapper(dbSession)::selectParamsByRuleUuids);
  }

  public void insertRuleParam(DbSession session, RuleDto rule, RuleParamDto param) {
    checkNotNull(rule.getUuid(), "Rule uuid must be set");
    param.setRuleUuid(rule.getUuid());

    param.setUuid(uuidFactory.create());
    mapper(session).insertParameter(param);
  }

  public RuleParamDto updateRuleParam(DbSession session, RuleDto rule, RuleParamDto param) {
    checkNotNull(rule.getUuid(), "Rule uuid must be set");
    checkNotNull(param.getUuid(), "Rule parameter is not yet persisted must be set");
    param.setRuleUuid(rule.getUuid());
    mapper(session).updateParameter(param);
    return param;
  }

  public void deleteRuleParam(DbSession session, String ruleParameterUuid) {
    mapper(session).deleteParameter(ruleParameterUuid);
  }

  public Set<DeprecatedRuleKeyDto> selectAllDeprecatedRuleKeys(DbSession session) {
    return mapper(session).selectAllDeprecatedRuleKeys();
  }

  public Set<DeprecatedRuleKeyDto> selectDeprecatedRuleKeysByRuleUuids(DbSession session, Collection<String> ruleUuids) {
    return mapper(session).selectDeprecatedRuleKeysByRuleUuids(ruleUuids);
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
