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
package org.sonar.server.rule;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.Startable;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleScope;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.rule.DeprecatedRuleKeyDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleDto.Format;
import org.sonar.db.rule.RuleDto.Scope;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleRepositoryDto;
import org.sonar.server.es.metadata.MetadataIndex;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.QProfileRules;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndexer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.intersection;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

/**
 * Register rules at server startup
 */
public class RegisterRules implements Startable {

  private static final Logger LOG = Loggers.get(RegisterRules.class);

  private final RuleDefinitionsLoader defLoader;
  private final QProfileRules qProfileRules;
  private final DbClient dbClient;
  private final RuleIndexer ruleIndexer;
  private final ActiveRuleIndexer activeRuleIndexer;
  private final Languages languages;
  private final System2 system2;
  private final WebServerRuleFinder webServerRuleFinder;
  private final UuidFactory uuidFactory;
  private final MetadataIndex metadataIndex;
  private final RuleDescriptionSectionsGeneratorResolver ruleDescriptionSectionsGeneratorResolver;


  public RegisterRules(RuleDefinitionsLoader defLoader, QProfileRules qProfileRules, DbClient dbClient, RuleIndexer ruleIndexer,
    ActiveRuleIndexer activeRuleIndexer, Languages languages, System2 system2,
    WebServerRuleFinder webServerRuleFinder, UuidFactory uuidFactory, MetadataIndex metadataIndex,
    RuleDescriptionSectionsGeneratorResolver ruleDescriptionSectionsGeneratorResolver) {
    this.defLoader = defLoader;
    this.qProfileRules = qProfileRules;
    this.dbClient = dbClient;
    this.ruleIndexer = ruleIndexer;
    this.activeRuleIndexer = activeRuleIndexer;
    this.languages = languages;
    this.system2 = system2;
    this.webServerRuleFinder = webServerRuleFinder;
    this.uuidFactory = uuidFactory;
    this.metadataIndex = metadataIndex;
    this.ruleDescriptionSectionsGeneratorResolver = ruleDescriptionSectionsGeneratorResolver;
  }

  @Override
  public void start() {
    Profiler profiler = Profiler.create(LOG).startInfo("Register rules");
    try (DbSession dbSession = dbClient.openSession(true)) {
      RulesDefinition.Context ruleDefinitionContext = defLoader.load();
      List<RulesDefinition.Repository> repositories = ruleDefinitionContext.repositories();
      RegisterRulesContext registerRulesContext = createRegisterRulesContext(dbSession);

      verifyRuleKeyConsistency(repositories, registerRulesContext);

      for (RulesDefinition.ExtendedRepository repoDef : repositories) {
        if (languages.get(repoDef.language()) != null) {
          registerRules(registerRulesContext, repoDef.rules(), dbSession);
          dbSession.commit();
        }
      }
      processRemainingDbRules(registerRulesContext, dbSession);
      List<ActiveRuleChange> changes = removeActiveRulesOnStillExistingRepositories(dbSession, registerRulesContext, repositories);
      dbSession.commit();

      persistRepositories(dbSession, ruleDefinitionContext.repositories());
      // FIXME lack of resiliency, active rules index is corrupted if rule index fails
      // to be updated. Only a single DB commit should be executed.
      ruleIndexer.commitAndIndex(dbSession, registerRulesContext.getAllModified().map(RuleDto::getUuid).collect(toSet()));
      changes.forEach(arChange -> dbClient.qProfileChangeDao().insert(dbSession, arChange.toDto(null)));
      activeRuleIndexer.commitAndIndex(dbSession, changes);
      registerRulesContext.getRenamed().forEach(e -> LOG.info("Rule {} re-keyed to {}", e.getValue(), e.getKey().getKey()));
      profiler.stopDebug();

      if (!registerRulesContext.hasDbRules()) {
        Stream.concat(ruleIndexer.getIndexTypes().stream(), activeRuleIndexer.getIndexTypes().stream())
          .forEach(t -> metadataIndex.setInitialized(t, true));
      }

      webServerRuleFinder.startCaching();
    }
  }

  private RegisterRulesContext createRegisterRulesContext(DbSession dbSession) {
    Map<RuleKey, RuleDto> allRules = dbClient.ruleDao().selectAll(dbSession).stream()
      .collect(uniqueIndex(RuleDto::getKey));
    Map<String, Set<SingleDeprecatedRuleKey>> existingDeprecatedKeysById = loadDeprecatedRuleKeys(dbSession);
    Map<String, List<RuleParamDto>> ruleParamsByRuleUuid = loadAllRuleParameters(dbSession);
    return new RegisterRulesContext(allRules, existingDeprecatedKeysById, ruleParamsByRuleUuid);
  }

  private Map<String, List<RuleParamDto>> loadAllRuleParameters(DbSession dbSession) {
    return dbClient.ruleDao().selectAllRuleParams(dbSession).stream()
      .collect(Collectors.groupingBy(RuleParamDto::getRuleUuid));
  }

  private Map<String, Set<SingleDeprecatedRuleKey>> loadDeprecatedRuleKeys(DbSession dbSession) {
    return dbClient.ruleDao().selectAllDeprecatedRuleKeys(dbSession).stream()
      .map(SingleDeprecatedRuleKey::from)
      .collect(Collectors.groupingBy(SingleDeprecatedRuleKey::getRuleUuid, Collectors.toSet()));
  }

  private static class RegisterRulesContext {
    // initial immutable data
    private final Map<RuleKey, RuleDto> dbRules;
    private final Set<RuleDto> known;
    private final Map<String, Set<SingleDeprecatedRuleKey>> dbDeprecatedKeysByUuid;
    private final Map<String, List<RuleParamDto>> ruleParamsByRuleUuid;
    private final Map<RuleKey, RuleDto> dbRulesByDbDeprecatedKey;
    // mutable data
    private final Set<RuleDto> created = new HashSet<>();
    private final Map<RuleDto, RuleKey> renamed = new HashMap<>();
    private final Set<RuleDto> updated = new HashSet<>();
    private final Set<RuleDto> unchanged = new HashSet<>();
    private final Set<RuleDto> removed = new HashSet<>();

    private RegisterRulesContext(Map<RuleKey, RuleDto> dbRules, Map<String, Set<SingleDeprecatedRuleKey>> dbDeprecatedKeysByUuid,
      Map<String, List<RuleParamDto>> ruleParamsByRuleUuid) {
      this.dbRules = ImmutableMap.copyOf(dbRules);
      this.known = ImmutableSet.copyOf(dbRules.values());
      this.dbDeprecatedKeysByUuid = dbDeprecatedKeysByUuid;
      this.ruleParamsByRuleUuid = ruleParamsByRuleUuid;
      this.dbRulesByDbDeprecatedKey = buildDbRulesByDbDeprecatedKey(dbDeprecatedKeysByUuid, dbRules);
    }

    private static Map<RuleKey, RuleDto> buildDbRulesByDbDeprecatedKey(Map<String, Set<SingleDeprecatedRuleKey>> dbDeprecatedKeysByUuid,
      Map<RuleKey, RuleDto> dbRules) {
      Map<String, RuleDto> dbRulesByRuleUuid = dbRules.values().stream()
        .collect(uniqueIndex(RuleDto::getUuid));

      Map<RuleKey, RuleDto> rulesByKey = new LinkedHashMap<>();
      for (Map.Entry<String, Set<SingleDeprecatedRuleKey>> entry : dbDeprecatedKeysByUuid.entrySet()) {
        String ruleUuid = entry.getKey();
        RuleDto rule = dbRulesByRuleUuid.get(ruleUuid);
        if (rule == null) {
          LOG.warn("Could not retrieve rule with uuid %s referenced by a deprecated rule key. " +
              "The following deprecated rule keys seem to be referencing a non-existing rule",
            ruleUuid, entry.getValue());
        } else {
          entry.getValue().forEach(d -> rulesByKey.put(d.getOldRuleKeyAsRuleKey(), rule));
        }
      }
      return unmodifiableMap(rulesByKey);
    }

    private boolean hasDbRules() {
      return !dbRules.isEmpty();
    }

    private Optional<RuleDto> getDbRuleFor(RulesDefinition.Rule ruleDef) {
      RuleKey ruleKey = RuleKey.of(ruleDef.repository().key(), ruleDef.key());
      Optional<RuleDto> res = Stream.concat(Stream.of(ruleKey), ruleDef.deprecatedRuleKeys().stream())
        .map(dbRules::get)
        .filter(Objects::nonNull)
        .findFirst();
      // may occur in case of plugin downgrade
      if (res.isEmpty()) {
        return Optional.ofNullable(dbRulesByDbDeprecatedKey.get(ruleKey));
      }
      return res;
    }

    private Map<RuleKey, SingleDeprecatedRuleKey> getDbDeprecatedKeysByOldRuleKey() {
      return dbDeprecatedKeysByUuid.values().stream()
        .flatMap(Collection::stream)
        .collect(uniqueIndex(SingleDeprecatedRuleKey::getOldRuleKeyAsRuleKey));
    }

    private Set<SingleDeprecatedRuleKey> getDBDeprecatedKeysFor(RuleDto rule) {
      return dbDeprecatedKeysByUuid.getOrDefault(rule.getUuid(), emptySet());
    }

    private List<RuleParamDto> getRuleParametersFor(String ruleUuid) {
      return ruleParamsByRuleUuid.getOrDefault(ruleUuid, emptyList());
    }

    private Stream<RuleDto> getRemaining() {
      Set<RuleDto> res = new HashSet<>(dbRules.values());
      res.removeAll(unchanged);
      res.removeAll(renamed.keySet());
      res.removeAll(updated);
      res.removeAll(removed);
      return res.stream();
    }

    private Stream<RuleDto> getRemoved() {
      return removed.stream();
    }

    public Stream<Map.Entry<RuleDto, RuleKey>> getRenamed() {
      return renamed.entrySet().stream();
    }

    private Stream<RuleDto> getAllModified() {
      return Stream.of(
          created.stream(),
          updated.stream(),
          removed.stream(),
          renamed.keySet().stream())
        .flatMap(s -> s);
    }

    private boolean isCreated(RuleDto ruleDto) {
      return created.contains(ruleDto);
    }

    private boolean isRenamed(RuleDto ruleDto) {
      return renamed.containsKey(ruleDto);
    }

    private boolean isUpdated(RuleDto ruleDto) {
      return updated.contains(ruleDto);
    }

    private void created(RuleDto ruleDto) {
      checkState(!known.contains(ruleDto), "known RuleDto can't be created");
      created.add(ruleDto);
    }

    private void renamed(RuleDto ruleDto) {
      ensureKnown(ruleDto);
      renamed.put(ruleDto, ruleDto.getKey());
    }

    private void updated(RuleDto ruleDto) {
      ensureKnown(ruleDto);
      updated.add(ruleDto);
    }

    private void removed(RuleDto ruleDto) {
      ensureKnown(ruleDto);
      removed.add(ruleDto);
    }

    private void unchanged(RuleDto ruleDto) {
      ensureKnown(ruleDto);
      unchanged.add(ruleDto);
    }

    private void ensureKnown(RuleDto ruleDto) {
      checkState(known.contains(ruleDto), "unknown RuleDto");
    }
  }

  private void persistRepositories(DbSession dbSession, List<RulesDefinition.Repository> repositories) {
    List<String> keys = repositories.stream().map(RulesDefinition.Repository::key).collect(toList(repositories.size()));
    Set<String> existingKeys = dbClient.ruleRepositoryDao().selectAllKeys(dbSession);

    Map<Boolean, List<RuleRepositoryDto>> dtos = repositories.stream()
      .map(r -> new RuleRepositoryDto(r.key(), r.language(), r.name()))
      .collect(Collectors.groupingBy(i -> existingKeys.contains(i.getKey())));

    dbClient.ruleRepositoryDao().update(dbSession, dtos.getOrDefault(true, emptyList()));
    dbClient.ruleRepositoryDao().insert(dbSession, dtos.getOrDefault(false, emptyList()));
    dbClient.ruleRepositoryDao().deleteIfKeyNotIn(dbSession, keys);
    dbSession.commit();
  }

  @Override
  public void stop() {
    // nothing
  }

  private void registerRules(RegisterRulesContext context, List<RulesDefinition.Rule> ruleDefs, DbSession session) {
    Map<RulesDefinition.Rule, RuleDto> dtos = new LinkedHashMap<>(ruleDefs.size());

    for (RulesDefinition.Rule ruleDef : ruleDefs) {
      RuleKey ruleKey = RuleKey.of(ruleDef.repository().key(), ruleDef.key());
      RuleDto ruleDto = findOrCreateRuleDto(context, session, ruleDef);
      dtos.put(ruleDef, ruleDto);

      // we must detect renaming __before__ we modify the DTO
      if (!ruleDto.getKey().equals(ruleKey)) {
        context.renamed(ruleDto);
        ruleDto.setRuleKey(ruleKey);
      }

      if (anyMerge(ruleDef, ruleDto)) {
        context.updated(ruleDto);
      }

      if (context.isUpdated(ruleDto) || context.isRenamed(ruleDto)) {
        LOG.info(format("Update Rule: language=%s key=%s", ruleDto.getLanguage(), ruleDto.getKey()));
        update(session, ruleDto);
      } else if (!context.isCreated(ruleDto)) {
        context.unchanged(ruleDto);
      }
    }

    for (Map.Entry<RulesDefinition.Rule, RuleDto> e : dtos.entrySet()) {
      mergeParams(context, e.getKey(), e.getValue(), session);
      updateDeprecatedKeys(context, e.getKey(), e.getValue(), session);
    }
  }

  @Nonnull
  private RuleDto findOrCreateRuleDto(RegisterRulesContext context, DbSession session, RulesDefinition.Rule ruleDef) {
    return context.getDbRuleFor(ruleDef)
      .orElseGet(() -> {
        RuleDto newRule = createRuleDto(ruleDef, session);
        context.created(newRule);
        return newRule;
      });
  }

  private RuleDto createRuleDto(RulesDefinition.Rule ruleDef, DbSession session) {
    RuleDto ruleDto = new RuleDto()
      .setUuid(uuidFactory.create())
      .setRuleKey(RuleKey.of(ruleDef.repository().key(), ruleDef.key()))
      .setPluginKey(ruleDef.pluginKey())
      .setIsTemplate(ruleDef.template())
      .setConfigKey(ruleDef.internalKey())
      .setLanguage(ruleDef.repository().language())
      .setName(ruleDef.name())
      .setSeverity(ruleDef.severity())
      .setStatus(ruleDef.status())
      .setGapDescription(ruleDef.gapDescription())
      .setSystemTags(ruleDef.tags())
      .setSecurityStandards(ruleDef.securityStandards())
      .setType(RuleType.valueOf(ruleDef.type().name()))
      .setScope(toDtoScope(ruleDef.scope()))
      .setIsExternal(ruleDef.repository().isExternal())
      .setIsAdHoc(false)
      .setCreatedAt(system2.now())
      .setUpdatedAt(system2.now())
      .setEducationPrinciples(ruleDef.educationPrincipleKeys());

    if (isNotEmpty(ruleDef.htmlDescription())) {
      ruleDto.setDescriptionFormat(Format.HTML);
    } else if (isNotEmpty(ruleDef.markdownDescription())) {
      ruleDto.setDescriptionFormat(Format.MARKDOWN);
    }

    generateRuleDescriptionSections(ruleDef)
      .forEach(ruleDto::addRuleDescriptionSectionDto);

    DebtRemediationFunction debtRemediationFunction = ruleDef.debtRemediationFunction();
    if (debtRemediationFunction != null) {
      ruleDto.setDefRemediationFunction(debtRemediationFunction.type().name());
      ruleDto.setDefRemediationGapMultiplier(debtRemediationFunction.gapMultiplier());
      ruleDto.setDefRemediationBaseEffort(debtRemediationFunction.baseEffort());
      ruleDto.setGapDescription(ruleDef.gapDescription());
    }

    dbClient.ruleDao().insert(session, ruleDto);
    return ruleDto;
  }

  private Set<RuleDescriptionSectionDto> generateRuleDescriptionSections(RulesDefinition.Rule ruleDef) {
    RuleDescriptionSectionsGenerator descriptionSectionGenerator = ruleDescriptionSectionsGeneratorResolver.getRuleDescriptionSectionsGenerator(ruleDef);
    return descriptionSectionGenerator.generateSections(ruleDef);
  }

  private static Scope toDtoScope(RuleScope scope) {
    switch (scope) {
      case ALL:
        return Scope.ALL;
      case MAIN:
        return Scope.MAIN;
      case TEST:
        return Scope.TEST;
      default:
        throw new IllegalArgumentException("Unknown rule scope: " + scope);
    }
  }

  private boolean anyMerge(RulesDefinition.Rule ruleDef, RuleDto ruleDto) {
    boolean ruleMerged = mergeRule(ruleDef, ruleDto);
    boolean debtDefinitionsMerged = mergeDebtDefinitions(ruleDef, ruleDto);
    boolean tagsMerged = mergeTags(ruleDef, ruleDto);
    boolean securityStandardsMerged = mergeSecurityStandards(ruleDef, ruleDto);
    boolean educationPrinciplesMerged = mergeEducationPrinciples(ruleDef, ruleDto);
    return ruleMerged || debtDefinitionsMerged || tagsMerged || securityStandardsMerged || educationPrinciplesMerged;
  }

  private boolean mergeRule(RulesDefinition.Rule def, RuleDto dto) {
    boolean changed = false;
    if (!Objects.equals(dto.getName(), def.name())) {
      dto.setName(def.name());
      changed = true;
    }
    if (mergeDescription(def, dto)) {
      changed = true;
    }
    if (!Objects.equals(dto.getPluginKey(), def.pluginKey())) {
      dto.setPluginKey(def.pluginKey());
      changed = true;
    }
    if (!Objects.equals(dto.getConfigKey(), def.internalKey())) {
      dto.setConfigKey(def.internalKey());
      changed = true;
    }
    String severity = def.severity();
    if (!Objects.equals(dto.getSeverityString(), severity)) {
      dto.setSeverity(severity);
      changed = true;
    }
    boolean isTemplate = def.template();
    if (isTemplate != dto.isTemplate()) {
      dto.setIsTemplate(isTemplate);
      changed = true;
    }
    if (def.status() != dto.getStatus()) {
      dto.setStatus(def.status());
      changed = true;
    }
    if (!Objects.equals(dto.getScope().name(), def.scope().name())) {
      dto.setScope(toDtoScope(def.scope()));
      changed = true;
    }
    if (!Objects.equals(dto.getLanguage(), def.repository().language())) {
      dto.setLanguage(def.repository().language());
      changed = true;
    }
    RuleType type = RuleType.valueOf(def.type().name());
    if (!Objects.equals(dto.getType(), type.getDbConstant())) {
      dto.setType(type);
      changed = true;
    }
    if (dto.isAdHoc()) {
      dto.setIsAdHoc(false);
      changed = true;
    }
    return changed;
  }

  private boolean mergeDescription(RulesDefinition.Rule rule, RuleDto ruleDto) {
    Set<RuleDescriptionSectionDto> newRuleDescriptionSectionDtos = generateRuleDescriptionSections(rule);
    if (ruleDescriptionSectionsUnchanged(ruleDto, newRuleDescriptionSectionDtos)) {
      return false;
    }
    ruleDto.replaceRuleDescriptionSectionDtos(newRuleDescriptionSectionDtos);
    if (containsHtmlDescription(rule)) {
      ruleDto.setDescriptionFormat(Format.HTML);
      return true;
    } else if (isNotEmpty(rule.markdownDescription())) {
      ruleDto.setDescriptionFormat(Format.MARKDOWN);
      return true;
    }
    return false;
  }

  private static boolean containsHtmlDescription(RulesDefinition.Rule rule) {
    return isNotEmpty(rule.htmlDescription()) || !rule.ruleDescriptionSections().isEmpty();
  }

  private static boolean ruleDescriptionSectionsUnchanged(RuleDto ruleDto, Set<RuleDescriptionSectionDto> newRuleDescriptionSectionDtos) {
    if (ruleDto.getRuleDescriptionSectionDtos().size() != newRuleDescriptionSectionDtos.size()) {
      return false;
    }
    return ruleDto.getRuleDescriptionSectionDtos().stream()
      .allMatch(sectionDto -> contains(newRuleDescriptionSectionDtos, sectionDto));
  }

  private static boolean contains(Set<RuleDescriptionSectionDto> sectionDtos, RuleDescriptionSectionDto sectionDto) {
    return sectionDtos.stream()
      .filter(s -> s.getKey().equals(sectionDto.getKey()) && s.getContent().equals(sectionDto.getContent()))
      .anyMatch(s -> Objects.equals(s.getContext(), sectionDto.getContext()));
  }

  private static boolean mergeDebtDefinitions(RulesDefinition.Rule def, RuleDto dto) {
    // Debt definitions are set to null if the sub-characteristic and the remediation function are null
    DebtRemediationFunction debtRemediationFunction = def.debtRemediationFunction();
    boolean hasDebt = debtRemediationFunction != null;
    if (hasDebt) {
      return mergeDebtDefinitions(dto,
        debtRemediationFunction.type().name(),
        debtRemediationFunction.gapMultiplier(),
        debtRemediationFunction.baseEffort(),
        def.gapDescription());
    }
    return mergeDebtDefinitions(dto, null, null, null, null);
  }

  private static boolean mergeDebtDefinitions(RuleDto dto, @Nullable String remediationFunction,
    @Nullable String remediationCoefficient, @Nullable String remediationOffset, @Nullable String effortToFixDescription) {
    boolean changed = false;

    if (!Objects.equals(dto.getDefRemediationFunction(), remediationFunction)) {
      dto.setDefRemediationFunction(remediationFunction);
      changed = true;
    }
    if (!Objects.equals(dto.getDefRemediationGapMultiplier(), remediationCoefficient)) {
      dto.setDefRemediationGapMultiplier(remediationCoefficient);
      changed = true;
    }
    if (!Objects.equals(dto.getDefRemediationBaseEffort(), remediationOffset)) {
      dto.setDefRemediationBaseEffort(remediationOffset);
      changed = true;
    }
    if (!Objects.equals(dto.getGapDescription(), effortToFixDescription)) {
      dto.setGapDescription(effortToFixDescription);
      changed = true;
    }
    return changed;
  }

  private void mergeParams(RegisterRulesContext context, RulesDefinition.Rule ruleDef, RuleDto rule, DbSession session) {
    List<RuleParamDto> paramDtos = context.getRuleParametersFor(rule.getUuid());
    Map<String, RuleParamDto> existingParamsByName = new HashMap<>();

    Profiler profiler = Profiler.create(Loggers.get(getClass()));
    for (RuleParamDto paramDto : paramDtos) {
      RulesDefinition.Param paramDef = ruleDef.param(paramDto.getName());
      if (paramDef == null) {
        profiler.start();
        dbClient.activeRuleDao().deleteParamsByRuleParam(session, paramDto);
        profiler.stopDebug(format("Propagate deleted param with name %s to active rules of rule %s", paramDto.getName(), rule.getKey()));
        dbClient.ruleDao().deleteRuleParam(session, paramDto.getUuid());
      } else {
        if (mergeParam(paramDto, paramDef)) {
          dbClient.ruleDao().updateRuleParam(session, rule, paramDto);
        }
        existingParamsByName.put(paramDto.getName(), paramDto);
      }
    }

    // Create newly parameters
    for (RulesDefinition.Param param : ruleDef.params()) {
      RuleParamDto paramDto = existingParamsByName.get(param.key());
      if (paramDto != null) {
        continue;
      }
      paramDto = RuleParamDto.createFor(rule)
        .setName(param.key())
        .setDescription(param.description())
        .setDefaultValue(param.defaultValue())
        .setType(param.type().toString());
      dbClient.ruleDao().insertRuleParam(session, rule, paramDto);
      if (StringUtils.isEmpty(param.defaultValue())) {
        continue;
      }
      // Propagate the default value to existing active rule parameters
      profiler.start();
      for (ActiveRuleDto activeRule : dbClient.activeRuleDao().selectByRuleUuid(session, rule.getUuid())) {
        ActiveRuleParamDto activeParam = ActiveRuleParamDto.createFor(paramDto).setValue(param.defaultValue());
        dbClient.activeRuleDao().insertParam(session, activeRule, activeParam);
      }
      profiler.stopDebug(format("Propagate new param with name %s to active rules of rule %s", paramDto.getName(), rule.getKey()));
    }
  }

  private static boolean mergeParam(RuleParamDto paramDto, RulesDefinition.Param paramDef) {
    boolean changed = false;
    if (!Objects.equals(paramDto.getType(), paramDef.type().toString())) {
      paramDto.setType(paramDef.type().toString());
      changed = true;
    }
    if (!Objects.equals(paramDto.getDefaultValue(), paramDef.defaultValue())) {
      paramDto.setDefaultValue(paramDef.defaultValue());
      changed = true;
    }
    if (!Objects.equals(paramDto.getDescription(), paramDef.description())) {
      paramDto.setDescription(paramDef.description());
      changed = true;
    }
    return changed;
  }

  private void updateDeprecatedKeys(RegisterRulesContext context, RulesDefinition.Rule ruleDef, RuleDto rule, DbSession dbSession) {

    Set<SingleDeprecatedRuleKey> deprecatedRuleKeysFromDefinition = SingleDeprecatedRuleKey.from(ruleDef);
    Set<SingleDeprecatedRuleKey> deprecatedRuleKeysFromDB = context.getDBDeprecatedKeysFor(rule);

    // DeprecatedKeys that must be deleted
    List<String> uuidsToBeDeleted = difference(deprecatedRuleKeysFromDB, deprecatedRuleKeysFromDefinition).stream()
      .map(SingleDeprecatedRuleKey::getUuid)
      .collect(toList());

    dbClient.ruleDao().deleteDeprecatedRuleKeys(dbSession, uuidsToBeDeleted);

    // DeprecatedKeys that must be created
    Sets.SetView<SingleDeprecatedRuleKey> deprecatedRuleKeysToBeCreated = difference(deprecatedRuleKeysFromDefinition, deprecatedRuleKeysFromDB);

    deprecatedRuleKeysToBeCreated
      .forEach(r -> dbClient.ruleDao().insert(dbSession, new DeprecatedRuleKeyDto()
        .setUuid(uuidFactory.create())
        .setRuleUuid(rule.getUuid())
        .setOldRepositoryKey(r.getOldRepositoryKey())
        .setOldRuleKey(r.getOldRuleKey())
        .setCreatedAt(system2.now())));
  }

  private static boolean mergeTags(RulesDefinition.Rule ruleDef, RuleDto dto) {
    boolean changed = false;

    if (RuleStatus.REMOVED == ruleDef.status()) {
      dto.setSystemTags(emptySet());
      changed = true;
    } else if (dto.getSystemTags().size() != ruleDef.tags().size() ||
      !dto.getSystemTags().containsAll(ruleDef.tags())) {
      dto.setSystemTags(ruleDef.tags());
      changed = true;
    }
    return changed;
  }

  private static boolean mergeSecurityStandards(RulesDefinition.Rule ruleDef, RuleDto dto) {
    boolean changed = false;

    if (RuleStatus.REMOVED == ruleDef.status()) {
      dto.setSecurityStandards(emptySet());
      changed = true;
    } else if (dto.getSecurityStandards().size() != ruleDef.securityStandards().size() ||
      !dto.getSecurityStandards().containsAll(ruleDef.securityStandards())) {
      dto.setSecurityStandards(ruleDef.securityStandards());
      changed = true;
    }
    return changed;
  }

  private static boolean mergeEducationPrinciples(RulesDefinition.Rule ruleDef, RuleDto dto) {
    boolean changed = false;
    if (dto.getEducationPrinciples().size() != ruleDef.educationPrincipleKeys().size() ||
      !dto.getEducationPrinciples().containsAll(ruleDef.educationPrincipleKeys())) {
      dto.setEducationPrinciples(ruleDef.educationPrincipleKeys());
      changed = true;
    }
    return changed;
  }

  private void processRemainingDbRules(RegisterRulesContext recorder, DbSession dbSession) {
    // custom rules check status of template, so they must be processed at the end
    List<RuleDto> customRules = new ArrayList<>();

    recorder.getRemaining().forEach(rule -> {
      if (rule.isCustomRule()) {
        customRules.add(rule);
      } else if (!rule.isAdHoc() && rule.getStatus() != RuleStatus.REMOVED) {
        removeRule(dbSession, recorder, rule);
      }
    });

    for (RuleDto customRule : customRules) {
      String templateUuid = customRule.getTemplateUuid();
      checkNotNull(templateUuid, "Template uuid of the custom rule '%s' is null", customRule);
      Optional<RuleDto> template = dbClient.ruleDao().selectByUuid(templateUuid, dbSession);
      if (template.isPresent() && template.get().getStatus() != RuleStatus.REMOVED) {
        if (updateCustomRuleFromTemplateRule(customRule, template.get())) {
          recorder.updated(customRule);
          update(dbSession, customRule);
        }
      } else {
        removeRule(dbSession, recorder, customRule);
      }
    }

    dbSession.commit();
  }

  private void removeRule(DbSession session, RegisterRulesContext recorder, RuleDto rule) {
    LOG.info(format("Disable rule %s", rule.getKey()));
    rule.setStatus(RuleStatus.REMOVED);
    rule.setSystemTags(emptySet());
    update(session, rule);
    // FIXME resetting the tags for all organizations must be handled a different way
    // rule.setTags(Collections.emptySet());
    // update(session, rule.getMetadata());
    recorder.removed(rule);
    if (recorder.getRemoved().count() % 100 == 0) {
      session.commit();
    }
  }

  private static boolean updateCustomRuleFromTemplateRule(RuleDto customRule, RuleDto templateRule) {
    boolean changed = false;
    if (!Objects.equals(customRule.getLanguage(), templateRule.getLanguage())) {
      customRule.setLanguage(templateRule.getLanguage());
      changed = true;
    }
    if (!Objects.equals(customRule.getConfigKey(), templateRule.getConfigKey())) {
      customRule.setConfigKey(templateRule.getConfigKey());
      changed = true;
    }
    if (!Objects.equals(customRule.getPluginKey(), templateRule.getPluginKey())) {
      customRule.setPluginKey(templateRule.getPluginKey());
      changed = true;
    }
    if (!Objects.equals(customRule.getDefRemediationFunction(), templateRule.getDefRemediationFunction())) {
      customRule.setDefRemediationFunction(templateRule.getDefRemediationFunction());
      changed = true;
    }
    if (!Objects.equals(customRule.getDefRemediationGapMultiplier(), templateRule.getDefRemediationGapMultiplier())) {
      customRule.setDefRemediationGapMultiplier(templateRule.getDefRemediationGapMultiplier());
      changed = true;
    }
    if (!Objects.equals(customRule.getDefRemediationBaseEffort(), templateRule.getDefRemediationBaseEffort())) {
      customRule.setDefRemediationBaseEffort(templateRule.getDefRemediationBaseEffort());
      changed = true;
    }
    if (!Objects.equals(customRule.getGapDescription(), templateRule.getGapDescription())) {
      customRule.setGapDescription(templateRule.getGapDescription());
      changed = true;
    }
    if (customRule.getStatus() != templateRule.getStatus()) {
      customRule.setStatus(templateRule.getStatus());
      changed = true;
    }
    if (!Objects.equals(customRule.getSeverityString(), templateRule.getSeverityString())) {
      customRule.setSeverity(templateRule.getSeverityString());
      changed = true;
    }
    if (!Objects.equals(customRule.getRepositoryKey(), templateRule.getRepositoryKey())) {
      customRule.setRepositoryKey(templateRule.getRepositoryKey());
      changed = true;
    }
    return changed;
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
  private List<ActiveRuleChange> removeActiveRulesOnStillExistingRepositories(DbSession dbSession, RegisterRulesContext recorder, List<RulesDefinition.Repository> context) {
    Set<String> existingAndRenamedRepositories = getExistingAndRenamedRepositories(recorder, context);
    List<ActiveRuleChange> changes = new ArrayList<>();
    Profiler profiler = Profiler.create(Loggers.get(getClass()));

    recorder.getRemoved()
      .filter(rule -> existingAndRenamedRepositories.contains(rule.getRepositoryKey()))
      .forEach(rule -> {
        // SONAR-4642 Remove active rules only when repository still exists
        profiler.start();
        changes.addAll(qProfileRules.deleteRule(dbSession, rule));
        profiler.stopDebug(format("Remove active rule for rule %s", rule.getKey()));
      });

    return changes;
  }

  private Set<String> getExistingAndRenamedRepositories(RegisterRulesContext recorder, Collection<RulesDefinition.Repository> context) {
    return Stream.concat(
        context.stream().map(RulesDefinition.ExtendedRepository::key),
        recorder.getRenamed().map(Map.Entry::getValue).map(RuleKey::repository))
      .collect(toSet());
  }

  private void update(DbSession session, RuleDto rule) {
    rule.setUpdatedAt(system2.now());
    dbClient.ruleDao().update(session, rule);
  }

  private static void verifyRuleKeyConsistency(List<RulesDefinition.Repository> repositories, RegisterRulesContext registerRulesContext) {
    List<RulesDefinition.Rule> definedRules = repositories.stream()
      .flatMap(r -> r.rules().stream())
      .collect(toList());

    Set<RuleKey> definedRuleKeys = definedRules.stream()
      .map(r -> RuleKey.of(r.repository().key(), r.key()))
      .collect(toSet());

    List<RuleKey> definedDeprecatedRuleKeys = definedRules.stream()
      .flatMap(r -> r.deprecatedRuleKeys().stream())
      .collect(toList());

    // Find duplicates in declared deprecated rule keys
    Set<RuleKey> duplicates = findDuplicates(definedDeprecatedRuleKeys);
    checkState(duplicates.isEmpty(), "The following deprecated rule keys are declared at least twice [%s]",
      lazyToString(() -> duplicates.stream().map(RuleKey::toString).collect(Collectors.joining(","))));

    // Find rule keys that are both deprecated and used
    Set<RuleKey> intersection = intersection(new HashSet<>(definedRuleKeys), new HashSet<>(definedDeprecatedRuleKeys)).immutableCopy();
    checkState(intersection.isEmpty(), "The following rule keys are declared both as deprecated and used key [%s]",
      lazyToString(() -> intersection.stream().map(RuleKey::toString).collect(Collectors.joining(","))));

    // Find incorrect usage of deprecated keys
    Map<RuleKey, SingleDeprecatedRuleKey> dbDeprecatedRuleKeysByOldRuleKey = registerRulesContext.getDbDeprecatedKeysByOldRuleKey();

    Set<String> incorrectRuleKeyMessage = definedRules.stream()
      .flatMap(r -> filterInvalidDeprecatedRuleKeys(dbDeprecatedRuleKeysByOldRuleKey, r))
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    checkState(incorrectRuleKeyMessage.isEmpty(), "An incorrect state of deprecated rule keys has been detected.\n %s",
      lazyToString(() -> String.join("\n", incorrectRuleKeyMessage)));
  }

  private static Stream<String> filterInvalidDeprecatedRuleKeys(Map<RuleKey, SingleDeprecatedRuleKey> dbDeprecatedRuleKeysByOldRuleKey, RulesDefinition.Rule rule) {
    return rule.deprecatedRuleKeys().stream()
      .map(rk -> {
        SingleDeprecatedRuleKey singleDeprecatedRuleKey = dbDeprecatedRuleKeysByOldRuleKey.get(rk);
        if (singleDeprecatedRuleKey == null) {
          // new deprecated rule key : OK
          return null;
        }
        RuleKey parentRuleKey = RuleKey.of(rule.repository().key(), rule.key());
        if (parentRuleKey.equals(singleDeprecatedRuleKey.getNewRuleKeyAsRuleKey())) {
          // same parent : OK
          return null;
        }
        if (rule.deprecatedRuleKeys().contains(singleDeprecatedRuleKey.getNewRuleKeyAsRuleKey())) {
          // the new rule is deprecating the old parentRuleKey : OK
          return null;
        }
        return format("The deprecated rule key [%s] was previously deprecated by [%s]. [%s] should be a deprecated key of [%s],",
          rk.toString(),
          singleDeprecatedRuleKey.getNewRuleKeyAsRuleKey().toString(),
          singleDeprecatedRuleKey.getNewRuleKeyAsRuleKey().toString(),
          RuleKey.of(rule.repository().key(), rule.key()).toString());
      });
  }

  private static Object lazyToString(Supplier<String> toString) {
    return new Object() {
      @Override
      public String toString() {
        return toString.get();
      }
    };
  }

  private static <T> Set<T> findDuplicates(Collection<T> list) {
    Set<T> duplicates = new HashSet<>();
    Set<T> uniques = new HashSet<>();

    list.forEach(t -> {
      if (!uniques.add(t)) {
        duplicates.add(t);
      }
    });

    return duplicates;
  }
}
