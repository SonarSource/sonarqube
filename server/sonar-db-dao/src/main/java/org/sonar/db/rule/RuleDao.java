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

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleQuery;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.es.RuleExtensionId;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.issue.ImpactDto;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeInputsWithoutOutput;
import static org.sonar.db.DatabaseUtils.executeLargeUpdates;

public class RuleDao implements Dao {

  private static final String PERCENT_SIGN = "%";

  private final UuidFactory uuidFactory;

  public RuleDao(UuidFactory uuidFactory) {
    this.uuidFactory = uuidFactory;
  }

  public Optional<RuleDto> selectByKey(DbSession session, String organizationUuid, RuleKey key) {
    RuleDto res = mapper(session).selectByOrganizationAndKey(organizationUuid, key);
    ensureOrganizationIsSet(organizationUuid, res);
    return ofNullable(res);
  }

  public Optional<RuleDto> selectByKey(DbSession session, RuleKey key) {
    return Optional.ofNullable(mapper(session).selectByKey(key));
  }

  public RuleDto selectOrFailByKey(DbSession session, RuleKey key) {
    return Optional.ofNullable(mapper(session).selectByKey(key))
      .orElseThrow(() -> new RowNotFoundException(String.format("Rule with key '%s' does not exist", key)));
  }

  public RuleDto selectOrFailByKey(DbSession session, OrganizationDto organization, RuleKey key) {
    RuleDto rule =  Optional.ofNullable(mapper(session).selectByOrganizationAndKey(organization.getUuid(), key))
      .orElseThrow(() -> new RowNotFoundException(String.format("Rule with key '%s' does not exist", key)));
    ensureOrganizationIsSet(organization.getUuid(), rule);
    return rule;
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

  public List<RuleDto> selectByUuids(DbSession session, String organizationUuid, Collection<String> uuids) {
    if (uuids.isEmpty()) {
      return emptyList();
    }
    return ensureOrganizationIsSet(
            organizationUuid,
            executeLargeInputs(uuids, chunk -> mapper(session).selectByUuidsAndOrganization(organizationUuid, chunk)));
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

  public List<RuleDto> selectAll(DbSession session, String organizationUuid) {
    return ensureOrganizationIsSet(organizationUuid, mapper(session).selectAll(organizationUuid));
  }

  public List<RuleDto> selectAll(DbSession session) {
    return mapper(session).selectAllRules();
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
    insertOrUpdateRuleMetadata(session, ruleDto.getMetadata());
    updateRuleDescriptionSectionDtos(ruleDto, mapper);
    updateRuleDefaultImpacts(ruleDto, mapper);
    updateRuleTags(ruleDto, mapper);
  }

  public void insertShallow(DbSession session, RuleDto ruleDto) {
    checkNotNull(ruleDto.getUuid(), "RuleDto has no 'uuid'.");
    mapper(session).insertRule(ruleDto);
  }

  public void update(DbSession session, RuleDto ruleDto) {
    RuleMapper mapper = mapper(session);
    mapper.updateRule(ruleDto);
    insertOrUpdateRuleMetadata(session, ruleDto.getMetadata());
    updateRuleDescriptionSectionDtos(ruleDto, mapper);
    updateRuleDefaultImpacts(ruleDto, mapper);
    updateRuleTags(ruleDto, mapper);
  }

  public List<String> selectTags(DbSession session, String organizationUuid, @Nullable String query, Pagination pagination) {
    String queryUpgraded = toLowerCaseAndSurroundWithPercentSigns(query);
    return mapper(session).selectTags(organizationUuid, queryUpgraded, pagination);
  }

  private static void updateRuleDescriptionSectionDtos(RuleDto ruleDto, RuleMapper mapper) {
    mapper.deleteRuleDescriptionSection(ruleDto.getUuid());
    insertRuleDescriptionSectionDtos(ruleDto, mapper);
  }

  private static void insertRuleDescriptionSectionDtos(RuleDto ruleDto, RuleMapper mapper) {
    ruleDto.getRuleDescriptionSectionDtos()
      .forEach(section -> mapper.insertRuleDescriptionSection(ruleDto.getUuid(), section));
  }

  public void insertRuleDescriptionSections(DbSession session, String ruleUuid, Set<RuleDescriptionSectionDto> sections) {
    sections
      .forEach(section -> mapper(session).insertRuleDescriptionSection(ruleUuid, section));
  }

  private static void updateRuleDefaultImpacts(RuleDto ruleDto, RuleMapper mapper) {
    mapper.deleteRuleDefaultImpacts(ruleDto.getUuid());
    insertRuleDefaultImpacts(ruleDto, mapper);
  }

  private static void updateRuleTags(RuleDto ruleDto, RuleMapper mapper) {
    mapper.deleteRuleTags(ruleDto.getUuid());
    insertRuleTags(ruleDto, mapper);
  }

  private static void insertRuleDefaultImpacts(RuleDto ruleDto, RuleMapper mapper) {
    ruleDto.getDefaultImpacts()
      .forEach(impact -> mapper.insertRuleDefaultImpact(ruleDto.getUuid(), impact));
  }

  public void insertRuleDefaultImpacts(DbSession session, String ruleUuid, Set<ImpactDto> impacts) {
    impacts
      .forEach(impact -> mapper(session).insertRuleDefaultImpact(ruleUuid, impact));
  }

  private static void insertRuleTags(RuleDto ruleDto, RuleMapper mapper) {
    ruleDto.getSystemTags()
      .forEach(tag -> mapper.insertRuleTag(ruleDto.getUuid(), tag, true));
    ruleDto.getTags()
      .forEach(tag -> mapper.insertRuleTag(ruleDto.getUuid(), tag, false));
  }

  public void insertRuleTag(DbSession dbSession, String ruleUuid, Set<String> tags, boolean isSystemTag) {
    for (String tag : tags) {
      mapper(dbSession).insertRuleTag(ruleUuid, tag, isSystemTag);
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
    mapper(dbSession).selectIndexingRuleExtensions(context -> {
      RuleExtensionForIndexingDto dto = context.getResultObject();
      consumer.accept(dto);
    });
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
    executeLargeInputsWithoutOutput(mapper.selectAllRules(),
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
    } else {
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

  public long countByLanguage(DbSession dbSession, String language) {
    return mapper(dbSession).countByLanguage(language);
  }

  private static String toLowerCaseAndSurroundWithPercentSigns(@Nullable String query) {
    return isBlank(query) ? PERCENT_SIGN : (PERCENT_SIGN + query.toLowerCase(Locale.ENGLISH) + PERCENT_SIGN);
  }

  public RuleListResult selectRules(DbSession dbSession, RuleListQuery ruleListQuery, Pagination pagination) {
    return new RuleListResult(
      mapper(dbSession).selectRules(ruleListQuery, pagination),
      mapper(dbSession).countByQuery(ruleListQuery));
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

  @VisibleForTesting
  void insertOrUpdateRuleMetadata(DbSession session, RuleMetadataDto ruleMetadataDto) {
    // The Rule metadata can be added only on Org level.
    if (ruleMetadataDto.getOrganizationUuid() != null) {
      if (ruleMetadataDto.isUndefined()) {
        mapper(session).deleteMetadata(ruleMetadataDto);
      } else if (mapper(session).countMetadata(ruleMetadataDto) > 0) {
        mapper(session).updateMetadata(ruleMetadataDto);
      } else {
        mapper(session).insertMetadata(ruleMetadataDto);
      }
    }
  }
}
