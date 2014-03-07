/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.rule;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.RemediationFunction;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.server.rule.RuleDefinitions;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.check.Cardinality;
import org.sonar.core.debt.db.CharacteristicDao;
import org.sonar.core.debt.db.CharacteristicDto;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.rule.*;
import org.sonar.server.qualityprofile.ProfilesManager;
import org.sonar.server.startup.RegisterDebtCharacteristicModel;

import javax.annotation.CheckForNull;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Register rules at server startup
 *
 * @since 4.2
 */
public class RuleRegistration implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger(RuleRegistration.class);

  private final RuleDefinitionsLoader defLoader;
  private final ProfilesManager profilesManager;
  private final RuleRegistry ruleRegistry;
  private final ESRuleTags esRuleTags;
  private final MyBatis myBatis;
  private final RuleDao ruleDao;
  private final RuleTagDao ruleTagDao;
  private final RuleTagOperations ruleTagOperations;
  private final ActiveRuleDao activeRuleDao;
  private final CharacteristicDao characteristicDao;
  private final System2 system = System2.INSTANCE;

  /**
   * @param registerTechnicalDebtModel used only to be started after init of the technical debt model
   */
  public RuleRegistration(RuleDefinitionsLoader defLoader, ProfilesManager profilesManager,
                          RuleRegistry ruleRegistry, ESRuleTags esRuleTags, RuleTagOperations ruleTagOperations,
                          MyBatis myBatis, RuleDao ruleDao, RuleTagDao ruleTagDao, ActiveRuleDao activeRuleDao, CharacteristicDao characteristicDao,
                          RegisterDebtCharacteristicModel registerTechnicalDebtModel) {
    this.defLoader = defLoader;
    this.profilesManager = profilesManager;
    this.ruleRegistry = ruleRegistry;
    this.esRuleTags = esRuleTags;
    this.ruleTagOperations = ruleTagOperations;
    this.myBatis = myBatis;
    this.ruleDao = ruleDao;
    this.ruleTagDao = ruleTagDao;
    this.activeRuleDao = activeRuleDao;
    this.characteristicDao = characteristicDao;
  }

  @Override
  public void start() {
    TimeProfiler profiler = new TimeProfiler().start("Register rules");
    SqlSession sqlSession = myBatis.openSession();
    try {
      RuleDefinitions.Context context = defLoader.load();
      Buffer buffer = new Buffer(system.now());
      List<CharacteristicDto> characteristicDtos = characteristicDao.selectCharacteristics();
      selectRulesFromDb(buffer, sqlSession);
      enableRuleDefinitions(context, buffer, characteristicDtos, sqlSession);
      List<RuleDto> removedRules = processRemainingDbRules(buffer, sqlSession);
      removeActiveRulesOnStillExistingRepositories(removedRules, context);
      index(buffer);
      ruleTagOperations.deleteUnusedTags(sqlSession);
      sqlSession.commit();

    } finally {
      sqlSession.close();
      profiler.stop();
    }
  }

  @Override
  public void stop() {
    // nothing
  }

  private void selectRulesFromDb(Buffer buffer, SqlSession sqlSession) {
    for (RuleDto ruleDto : ruleDao.selectNonManual(sqlSession)) {
      buffer.add(ruleDto);
      buffer.markUnprocessed(ruleDto);
    }
    for (RuleParamDto paramDto : ruleDao.selectParameters(sqlSession)) {
      buffer.add(paramDto);
    }
    for (RuleTagDto tagDto : ruleTagDao.selectAll(sqlSession)) {
      buffer.add(tagDto);
    }
    for (RuleRuleTagDto tagDto : ruleDao.selectTags(sqlSession)) {
      buffer.add(tagDto);
    }
  }

  private void enableRuleDefinitions(RuleDefinitions.Context context, Buffer buffer, List<CharacteristicDto> characteristicDtos, SqlSession sqlSession) {
    for (RuleDefinitions.Repository repoDef : context.repositories()) {
      enableRepository(buffer, sqlSession, repoDef, characteristicDtos);
    }
    for (RuleDefinitions.ExtendedRepository extendedRepoDef : context.extendedRepositories()) {
      if (context.repository(extendedRepoDef.key()) == null) {
        LOG.warn(String.format("Extension is ignored, repository %s does not exist", extendedRepoDef.key()));
      } else {
        enableRepository(buffer, sqlSession, extendedRepoDef, characteristicDtos);
      }
    }
  }

  private void enableRepository(Buffer buffer, SqlSession sqlSession, RuleDefinitions.ExtendedRepository repoDef, List<CharacteristicDto> characteristicDtos) {
    int count = 0;
    for (RuleDefinitions.Rule ruleDef : repoDef.rules()) {
      RuleDto dto = buffer.rule(RuleKey.of(ruleDef.repository().key(), ruleDef.key()));
      if (dto == null) {
        dto = enableAndInsert(buffer, sqlSession, ruleDef, characteristicDtos);
      } else {
        enableAndUpdate(buffer, sqlSession, ruleDef, dto, characteristicDtos);
      }
      buffer.markProcessed(dto);
      count++;
      if (count % 100 == 0) {
        sqlSession.commit();
      }
    }
    sqlSession.commit();
  }

  private RuleDto enableAndInsert(Buffer buffer, SqlSession sqlSession, RuleDefinitions.Rule ruleDef, List<CharacteristicDto> characteristicDtos) {
    RemediationFunction remediationFunction = ruleDef.remediationFunction();

    RuleDto ruleDto = new RuleDto()
      .setCardinality(ruleDef.template() ? Cardinality.MULTIPLE : Cardinality.SINGLE)
      .setConfigKey(ruleDef.internalKey())
      .setDescription(ruleDef.htmlDescription())
      .setLanguage(ruleDef.repository().language())
      .setName(ruleDef.name())
      .setRepositoryKey(ruleDef.repository().key())
      .setRuleKey(ruleDef.key())
      .setSeverity(ruleDef.severity())
      .setCreatedAt(buffer.now())
      .setUpdatedAt(buffer.now())
      .setStatus(ruleDef.status().name());

    CharacteristicDto characteristic = findCharacteristic(characteristicDtos, ruleDef);
    if (characteristic != null) {
      ruleDto.setDefaultCharacteristicId(characteristic.getId())
        .setDefaultRemediationFunction(remediationFunction != null ? remediationFunction.name() : null)
        .setDefaultRemediationFactor(ruleDef.remediationFactor())
        .setDefaultRemediationOffset(ruleDef.remediationOffset())
        .setEffortToFixL10nKey(ruleDef.effortToFixL10nKey());
    }

    ruleDao.insert(ruleDto, sqlSession);
    buffer.add(ruleDto);

    for (RuleDefinitions.Param param : ruleDef.params()) {
      RuleParamDto paramDto = new RuleParamDto()
        .setRuleId(ruleDto.getId())
        .setDefaultValue(param.defaultValue())
        .setDescription(param.description())
        .setName(param.name())
        .setType(param.type().toString());
      ruleDao.insert(paramDto, sqlSession);
      buffer.add(paramDto);
    }
    mergeTags(buffer, sqlSession, ruleDef, ruleDto);
    return ruleDto;
  }

  private void enableAndUpdate(Buffer buffer, SqlSession sqlSession, RuleDefinitions.Rule ruleDef, RuleDto dto, List<CharacteristicDto> characteristicDtos) {
    if (mergeRule(buffer, ruleDef, dto, characteristicDtos)) {
      ruleDao.update(dto);
    }
    mergeParams(buffer, sqlSession, ruleDef, dto);
    mergeTags(buffer, sqlSession, ruleDef, dto);
    buffer.markProcessed(dto);
  }

  private boolean mergeRule(Buffer buffer, RuleDefinitions.Rule def, RuleDto dto, List<CharacteristicDto> characteristicDtos) {
    boolean changed = false;
    if (!StringUtils.equals(dto.getName(), def.name())) {
      dto.setName(def.name());
      changed = true;
    }
    if (!StringUtils.equals(dto.getDescription(), def.htmlDescription())) {
      dto.setDescription(def.htmlDescription());
      changed = true;
    }
    if (!StringUtils.equals(dto.getConfigKey(), def.internalKey())) {
      dto.setConfigKey(def.internalKey());
      changed = true;
    }
    String severity = def.severity();
    if (!ObjectUtils.equals(dto.getSeverityString(), severity)) {
      dto.setSeverity(severity);
      changed = true;
    }
    Cardinality cardinality = def.template() ? Cardinality.MULTIPLE : Cardinality.SINGLE;
    if (!cardinality.equals(dto.getCardinality())) {
      dto.setCardinality(cardinality);
      changed = true;
    }
    String status = def.status().name();
    if (!StringUtils.equals(dto.getStatus(), status)) {
      dto.setStatus(status);
      changed = true;
    }
    if (!StringUtils.equals(dto.getLanguage(), def.repository().language())) {
      dto.setLanguage(def.repository().language());
      changed = true;
    }
    changed = mergeDebtDefinitions(def, dto, characteristicDtos) || changed;
    if (changed) {
      dto.setUpdatedAt(buffer.now());
    }
    return changed;
  }

  private boolean mergeDebtDefinitions(RuleDefinitions.Rule def, RuleDto dto, List<CharacteristicDto> characteristicDtos) {
    boolean changed = false;

    CharacteristicDto characteristic = findCharacteristic(characteristicDtos, def);
    // Debt definitions are set to null if the characteristic is null or unknown
    Integer characteristicId = characteristic != null ? characteristic.getId() : null;
    RemediationFunction remediationFunction = characteristic != null ? def.remediationFunction() : null;
    String remediationFactor = characteristic != null ? def.remediationFactor() : null;
    String remediationOffset = characteristic != null ? def.remediationOffset() : null;
    String effortToFixL10nKey = characteristic != null ? def.effortToFixL10nKey() : null;

    if (!ObjectUtils.equals(dto.getDefaultCharacteristicId(), characteristicId)) {
      dto.setDefaultCharacteristicId(characteristicId);
      changed = true;
    }
    String remediationFunctionString = remediationFunction != null ? remediationFunction.name() : null;
    if (!StringUtils.equals(dto.getDefaultRemediationFunction(), remediationFunctionString)) {
      dto.setDefaultRemediationFunction(remediationFunctionString);
      changed = true;
    }
    if (!StringUtils.equals(dto.getDefaultRemediationFactor(), remediationFactor)) {
      dto.setDefaultRemediationFactor(remediationFactor);
      changed = true;
    }
    if (!StringUtils.equals(dto.getDefaultRemediationOffset(), remediationOffset)) {
      dto.setDefaultRemediationOffset(remediationOffset);
      changed = true;
    }
    if (!StringUtils.equals(dto.getEffortToFixL10nKey(), effortToFixL10nKey)) {
      dto.setEffortToFixL10nKey(effortToFixL10nKey);
      changed = true;
    }
    return changed;
  }

  private void mergeParams(Buffer buffer, SqlSession sqlSession, RuleDefinitions.Rule ruleDef, RuleDto dto) {
    Collection<RuleParamDto> paramDtos = buffer.paramsForRuleId(dto.getId());
    Set<String> persistedParamKeys = Sets.newHashSet();
    for (RuleParamDto paramDto : paramDtos) {
      RuleDefinitions.Param paramDef = ruleDef.param(paramDto.getName());
      if (paramDef == null) {
        activeRuleDao.deleteParametersWithParamId(paramDto.getId(), sqlSession);
        ruleDao.deleteParam(paramDto, sqlSession);
      } else {
        // TODO validate that existing active rules still match constraints
        // TODO store param name
        if (mergeParam(paramDto, paramDef)) {
          ruleDao.update(paramDto, sqlSession);
        }
        persistedParamKeys.add(paramDto.getName());
      }
    }
    for (RuleDefinitions.Param param : ruleDef.params()) {
      if (!persistedParamKeys.contains(param.key())) {
        RuleParamDto paramDto = new RuleParamDto()
          .setRuleId(dto.getId())
          .setName(param.key())
          .setDescription(param.description())
          .setDefaultValue(param.defaultValue())
          .setType(param.type().toString());
        ruleDao.insert(paramDto, sqlSession);
        buffer.add(paramDto);
      }
    }
  }

  private boolean mergeParam(RuleParamDto paramDto, RuleDefinitions.Param paramDef) {
    boolean changed = false;
    if (!StringUtils.equals(paramDto.getType(), paramDef.type().toString())) {
      paramDto.setType(paramDef.type().toString());
      changed = true;
    }
    if (!StringUtils.equals(paramDto.getDefaultValue(), paramDef.defaultValue())) {
      paramDto.setDefaultValue(paramDef.defaultValue());
      changed = true;
    }
    if (!StringUtils.equals(paramDto.getDescription(), paramDef.description())) {
      paramDto.setDescription(paramDef.description());
      changed = true;
    }
    return changed;
  }

  private void mergeTags(Buffer buffer, SqlSession sqlSession, RuleDefinitions.Rule ruleDef, RuleDto dto) {
    Set<String> existingSystemTags = Sets.newHashSet();

    Collection<RuleRuleTagDto> tagDtos = ImmutableList.copyOf(buffer.tagsForRuleId(dto.getId()));
    for (RuleRuleTagDto tagDto : tagDtos) {
      String tag = tagDto.getTag();

      if (tagDto.getType() == RuleTagType.SYSTEM) {
        // tag previously declared by plugin
        if (!ruleDef.tags().contains(tag)) {
          // not declared anymore
          ruleDao.deleteTag(tagDto, sqlSession);
          buffer.remove(tagDto);
        } else {
          existingSystemTags.add(tagDto.getTag());
        }
      } else {
        // tags created by end-users
        if (ruleDef.tags().contains(tag)) {
          long tagId = getOrCreateReferenceTagId(buffer, tag, sqlSession);
          tagDto.setId(tagId);
          tagDto.setType(RuleTagType.SYSTEM);
          ruleDao.update(tagDto, sqlSession);
          existingSystemTags.add(tag);
        }
      }
    }

    for (String tag : ruleDef.tags()) {
      if (!existingSystemTags.contains(tag)) {
        long tagId = getOrCreateReferenceTagId(buffer, tag, sqlSession);
        RuleRuleTagDto newTagDto = new RuleRuleTagDto()
          .setRuleId(dto.getId())
          .setTagId(tagId)
          .setTag(tag)
          .setType(RuleTagType.SYSTEM);
        ruleDao.insert(newTagDto, sqlSession);
        buffer.add(newTagDto);
      }
    }
  }

  private long getOrCreateReferenceTagId(Buffer buffer, String tag, SqlSession sqlSession) {
    // End-user tag is converted to system tag
    long tagId = 0L;
    if (buffer.referenceTagExists(tag)) {
      tagId = buffer.referenceTagIdForValue(tag);
    } else {
      RuleTagDto newRuleTag = new RuleTagDto().setTag(tag);
      ruleTagDao.insert(newRuleTag, sqlSession);
      buffer.add(newRuleTag);
      tagId = newRuleTag.getId();
    }
    return tagId;
  }

  private List<RuleDto> processRemainingDbRules(Buffer buffer, SqlSession sqlSession) {
    List<RuleDto> removedRules = newArrayList();
    for (Integer unprocessedRuleId : buffer.unprocessedRuleIds) {
      RuleDto ruleDto = buffer.rulesById.get(unprocessedRuleId);
      boolean toBeRemoved = true;
      // Update copy of template rules from template
      if (ruleDto.getParentId() != null) {
        RuleDto parent = buffer.rulesById.get(ruleDto.getParentId());
        if (parent != null && !parent.getStatus().equals(Rule.STATUS_REMOVED)) {
          // TODO merge params and tags ?
          ruleDto.setLanguage(parent.getLanguage());
          ruleDto.setStatus(parent.getStatus());
          ruleDto.setDefaultCharacteristicId(parent.getDefaultCharacteristicId());
          ruleDto.setDefaultRemediationFunction(parent.getDefaultRemediationFunction());
          ruleDto.setDefaultRemediationFactor(parent.getDefaultRemediationFactor());
          ruleDto.setDefaultRemediationOffset(parent.getDefaultRemediationOffset());
          ruleDto.setEffortToFixL10nKey(parent.getEffortToFixL10nKey());
          ruleDto.setUpdatedAt(buffer.now());
          ruleDao.update(ruleDto, sqlSession);
          toBeRemoved = false;
        }
      }
      if (toBeRemoved) {
        // TODO log repository key
        LOG.info("Disable rule " + ruleDto.getRuleKey());
        ruleDto.setStatus(Rule.STATUS_REMOVED);
        ruleDto.setUpdatedAt(buffer.now());
        for (RuleRuleTagDto removed : buffer.tagsByRuleId.removeAll(ruleDto.getId())) {
          ruleDao.deleteTag(removed, sqlSession);
        }
        ruleDao.update(ruleDto, sqlSession);
        removedRules.add(ruleDto);
        if (removedRules.size() % 100 == 0) {
          sqlSession.commit();
        }
      }
    }
    sqlSession.commit();

    return removedRules;
  }

  /**
   * SONAR-4642
   * <p/>
   * Remove active rules on repositories that still exists.
   * <p/>
   * For instance, if the javascript repository do not provide anymore some rules, active rules related to this rules will be removed.
   * But if the javascript repository do not exists anymore, then related active rules will not be removed.
   * <p/>
   * The side effect of this approach is that extended repositories will not be managed the same way.
   * If an extended repository do not exists anymore, then related active rules will be removed.
   */
  private void removeActiveRulesOnStillExistingRepositories(List<RuleDto> removedRules, RuleDefinitions.Context context) {
    List<String> repositoryKeys = newArrayList(Iterables.transform(context.repositories(), new Function<RuleDefinitions.Repository, String>() {
      @Override
      public String apply(RuleDefinitions.Repository input) {
        return input.key();
      }
    }
    ));

    for (RuleDto rule : removedRules) {
      // SONAR-4642 Remove active rules only when repository still exists
      if (repositoryKeys.contains(rule.getRepositoryKey())) {
        profilesManager.removeActivatedRules(rule.getId());
      }
    }
  }

  private void index(Buffer buffer) {
    ruleRegistry.bulkRegisterRules(buffer.rulesById.values(), buffer.paramsByRuleId, buffer.tagsByRuleId);
    esRuleTags.putAllTags(buffer.referenceTagsByTagValue.values());
  }

  static class Buffer {
    private Date now;
    private List<Integer> unprocessedRuleIds = newArrayList();
    private Map<RuleKey, RuleDto> rulesByKey = Maps.newHashMap();
    private Map<Integer, RuleDto> rulesById = Maps.newHashMap();
    private Multimap<Integer, RuleParamDto> paramsByRuleId = ArrayListMultimap.create();
    private Multimap<Integer, RuleRuleTagDto> tagsByRuleId = ArrayListMultimap.create();
    private Map<String, RuleTagDto> referenceTagsByTagValue = Maps.newHashMap();

    Buffer(long now) {
      this.now = new Date(now);
    }

    Date now() {
      return now;
    }

    void add(RuleDto rule) {
      rulesById.put(rule.getId(), rule);
      rulesByKey.put(RuleKey.of(rule.getRepositoryKey(), rule.getRuleKey()), rule);
    }

    void add(RuleTagDto tag) {
      referenceTagsByTagValue.put(tag.getTag(), tag);
    }

    void add(RuleParamDto param) {
      paramsByRuleId.put(param.getRuleId(), param);
    }

    void add(RuleRuleTagDto tag) {
      tagsByRuleId.put(tag.getRuleId(), tag);
    }

    void remove(RuleRuleTagDto tag) {
      tagsByRuleId.remove(tag.getRuleId(), tag);
    }

    @CheckForNull
    RuleDto rule(RuleKey key) {
      return rulesByKey.get(key);
    }

    Collection<RuleParamDto> paramsForRuleId(Integer ruleId) {
      return paramsByRuleId.get(ruleId);
    }

    Collection<RuleRuleTagDto> tagsForRuleId(Integer ruleId) {
      return tagsByRuleId.get(ruleId);
    }

    boolean referenceTagExists(String tagValue) {
      return referenceTagsByTagValue.containsKey(tagValue);
    }

    Long referenceTagIdForValue(String tagValue) {
      return referenceTagsByTagValue.get(tagValue).getId();
    }

    void markUnprocessed(RuleDto ruleDto) {
      unprocessedRuleIds.add(ruleDto.getId());
    }

    void markProcessed(RuleDto ruleDto) {
      unprocessedRuleIds.remove(ruleDto.getId());
    }
  }

  @CheckForNull
  private CharacteristicDto findCharacteristic(List<CharacteristicDto> characteristicDtos, RuleDefinitions.Rule ruleDef) {
    final String key = ruleDef.characteristicKey();
    if (key == null) {
      // Rule is not linked to a characteristic, nothing to do
      return null;
    }
    CharacteristicDto characteristicDto = Iterables.find(characteristicDtos, new Predicate<CharacteristicDto>() {
      @Override
      public boolean apply(CharacteristicDto input) {
        String characteristicKey = input.getKey();
        return characteristicKey != null && characteristicKey.equals(key);
      }
    }, null);

    if (characteristicDto == null) {
      LOG.warn(String.format("Characteristic '%s' has not been found, Technical debt definitions on rule '%s:%s' will be ignored",
        key, ruleDef.repository().name(), ruleDef.key()));
    } else if (characteristicDto.getParentId() == null) {
      LOG.error(String.format("Rule '%s:%s' should not be linked on the root characteristic '%s'. Technical debt definitions on this rule wll be ignored", key,
        ruleDef.repository().name(), ruleDef.key()));
      return null;
    }
    return characteristicDto;
  }
}
