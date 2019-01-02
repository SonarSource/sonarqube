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
package org.sonar.server.rule;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
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
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.rule.DeprecatedRuleKeyDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto.Format;
import org.sonar.db.rule.RuleDto.Scope;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleRepositoryDto;
import org.sonar.server.organization.OrganizationFlags;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.QProfileRules;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndexer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.intersection;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
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
  private final OrganizationFlags organizationFlags;
  private final WebServerRuleFinder webServerRuleFinder;
  private final UuidFactory uuidFactory;

  public RegisterRules(RuleDefinitionsLoader defLoader, QProfileRules qProfileRules, DbClient dbClient, RuleIndexer ruleIndexer,
    ActiveRuleIndexer activeRuleIndexer, Languages languages, System2 system2, OrganizationFlags organizationFlags,
    WebServerRuleFinder webServerRuleFinder, UuidFactory uuidFactory) {
    this.defLoader = defLoader;
    this.qProfileRules = qProfileRules;
    this.dbClient = dbClient;
    this.ruleIndexer = ruleIndexer;
    this.activeRuleIndexer = activeRuleIndexer;
    this.languages = languages;
    this.system2 = system2;
    this.organizationFlags = organizationFlags;
    this.webServerRuleFinder = webServerRuleFinder;
    this.uuidFactory = uuidFactory;
  }

  @Override
  public void start() {
    Profiler profiler = Profiler.create(LOG).startInfo("Register rules");
    try (DbSession dbSession = dbClient.openSession(false)) {
      RulesDefinition.Context ruleDefinitionContext = defLoader.load();
      List<RulesDefinition.ExtendedRepository> repositories = getRepositories(ruleDefinitionContext);
      RegisterRulesContext registerRulesContext = createRegisterRulesContext(dbSession);

      verifyRuleKeyConsistency(repositories, registerRulesContext);

      boolean orgsEnabled = organizationFlags.isEnabled(dbSession);
      for (RulesDefinition.ExtendedRepository repoDef : repositories) {
        if (languages.get(repoDef.language()) != null) {
          for (RulesDefinition.Rule ruleDef : repoDef.rules()) {
            if (noTemplateRuleWithOrganizationsEnabled(registerRulesContext, orgsEnabled, ruleDef)) {
              continue;
            }
            registerRule(registerRulesContext, ruleDef, dbSession);
          }
          dbSession.commit();
        }
      }
      processRemainingDbRules(registerRulesContext, dbSession);
      List<ActiveRuleChange> changes = removeActiveRulesOnStillExistingRepositories(dbSession, registerRulesContext, repositories);
      dbSession.commit();

      persistRepositories(dbSession, ruleDefinitionContext.repositories());
      // FIXME lack of resiliency, active rules index is corrupted if rule index fails
      // to be updated. Only a single DB commit should be executed.
      ruleIndexer.commitAndIndex(dbSession, registerRulesContext.getAllModified().map(RuleDefinitionDto::getId).collect(toSet()));
      activeRuleIndexer.commitAndIndex(dbSession, changes);
      registerRulesContext.getRenamed().forEach(e -> LOG.info("Rule {} re-keyed to {}", e.getValue(), e.getKey().getKey()));
      profiler.stopDebug();

      webServerRuleFinder.startCaching();
    }
  }

  private static List<RulesDefinition.ExtendedRepository> getRepositories(RulesDefinition.Context context) {
    List<RulesDefinition.ExtendedRepository> repositories = new ArrayList<>(context.repositories());
    for (RulesDefinition.ExtendedRepository extendedRepoDef : context.extendedRepositories()) {
      if (context.repository(extendedRepoDef.key()) == null) {
        LOG.warn(format("Extension is ignored, repository %s does not exist", extendedRepoDef.key()));
      } else {
        repositories.add(extendedRepoDef);
      }
    }
    return repositories;
  }

  private RegisterRulesContext createRegisterRulesContext(DbSession dbSession) {
    Map<RuleKey, RuleDefinitionDto> allRules = dbClient.ruleDao().selectAllDefinitions(dbSession)
      .stream()
      .collect(uniqueIndex(RuleDefinitionDto::getKey));
    Map<Integer, Set<SingleDeprecatedRuleKey>> existingDeprecatedKeysById = loadDeprecatedRuleKeys(dbSession);
    return new RegisterRulesContext(allRules, existingDeprecatedKeysById);
  }

  private Map<Integer, Set<SingleDeprecatedRuleKey>> loadDeprecatedRuleKeys(DbSession dbSession) {
    return dbClient.ruleDao().selectAllDeprecatedRuleKeys(dbSession)
      .stream()
      .map(SingleDeprecatedRuleKey::from)
      .collect(Collectors.groupingBy(SingleDeprecatedRuleKey::getRuleId, Collectors.toSet()));
  }

  private static boolean noTemplateRuleWithOrganizationsEnabled(RegisterRulesContext registerRulesContext, boolean orgsEnabled, RulesDefinition.Rule ruleDef) {
    if (!ruleDef.template() || !orgsEnabled) {
      return false;
    }

    Optional<RuleDefinitionDto> dbRule = registerRulesContext.getDbRuleFor(ruleDef);
    if (dbRule.isPresent() && dbRule.get().getStatus() == RuleStatus.REMOVED) {
      RuleDefinitionDto dto = dbRule.get();
      LOG.debug("Template rule {} kept removed, because organizations are enabled.", dto.getKey());
      registerRulesContext.removed(dto);
    } else {
      LOG.info("Template rule {} will not be imported, because organizations are enabled.", RuleKey.of(ruleDef.repository().key(), ruleDef.key()));
    }
    return true;
  }

  private static class RegisterRulesContext {
    // initial immutable data
    private final Map<RuleKey, RuleDefinitionDto> dbRules;
    private final Set<RuleDefinitionDto> known;
    private final Map<Integer, Set<SingleDeprecatedRuleKey>> dbDeprecatedKeysById;
    private final Map<RuleKey, RuleDefinitionDto> dbRulesByDbDeprecatedKey;
    // mutable data
    private final Set<RuleDefinitionDto> created = new HashSet<>();
    private final Map<RuleDefinitionDto, RuleKey> renamed = new HashMap<>();
    private final Set<RuleDefinitionDto> updated = new HashSet<>();
    private final Set<RuleDefinitionDto> unchanged = new HashSet<>();
    private final Set<RuleDefinitionDto> removed = new HashSet<>();

    private RegisterRulesContext(Map<RuleKey, RuleDefinitionDto> dbRules, Map<Integer, Set<SingleDeprecatedRuleKey>> dbDeprecatedKeysById) {
      this.dbRules = ImmutableMap.copyOf(dbRules);
      this.known = ImmutableSet.copyOf(dbRules.values());
      this.dbDeprecatedKeysById = dbDeprecatedKeysById;
      this.dbRulesByDbDeprecatedKey = buildDbRulesByDbDeprecatedKey(dbDeprecatedKeysById, dbRules);
    }

    private static Map<RuleKey, RuleDefinitionDto> buildDbRulesByDbDeprecatedKey(Map<Integer, Set<SingleDeprecatedRuleKey>> dbDeprecatedKeysById,
      Map<RuleKey, RuleDefinitionDto> dbRules) {
      Map<Integer, RuleDefinitionDto> dbRulesByRuleId = dbRules.values().stream()
        .collect(uniqueIndex(RuleDefinitionDto::getId));

      ImmutableMap.Builder<RuleKey, RuleDefinitionDto> builder = ImmutableMap.builder();
      for (Map.Entry<Integer, Set<SingleDeprecatedRuleKey>> entry : dbDeprecatedKeysById.entrySet()) {
        Integer ruleId = entry.getKey();
        RuleDefinitionDto rule = dbRulesByRuleId.get(ruleId);
        if (rule == null) {
          LOG.warn("Could not retrieve rule with id %s referenced by a deprecated rule key. " +
            "The following deprecated rule keys seem to be referencing a non-existing rule",
            ruleId, entry.getValue());
        } else {
          entry.getValue().forEach(d -> builder.put(d.getOldRuleKeyAsRuleKey(), rule));
        }
      }
      return builder.build();
    }

    private Optional<RuleDefinitionDto> getDbRuleFor(RulesDefinition.Rule ruleDef) {
      RuleKey ruleKey = RuleKey.of(ruleDef.repository().key(), ruleDef.key());
      Optional<RuleDefinitionDto> res = Stream.concat(Stream.of(ruleKey), ruleDef.deprecatedRuleKeys().stream())
        .map(dbRules::get)
        .filter(Objects::nonNull)
        .findFirst();
      // may occur in case of plugin downgrade
      if (!res.isPresent()) {
        return Optional.ofNullable(dbRulesByDbDeprecatedKey.get(ruleKey));
      }
      return res;
    }

    private ImmutableMap<RuleKey, SingleDeprecatedRuleKey> getDbDeprecatedKeysByOldRuleKey() {
      return dbDeprecatedKeysById.values().stream()
        .flatMap(Collection::stream)
        .collect(uniqueIndex(SingleDeprecatedRuleKey::getOldRuleKeyAsRuleKey));
    }

    private Set<SingleDeprecatedRuleKey> getDBDeprecatedKeysFor(RuleDefinitionDto rule) {
      return dbDeprecatedKeysById.getOrDefault(rule.getId(), emptySet());
    }

    private Stream<RuleDefinitionDto> getRemaining() {
      Set<RuleDefinitionDto> res = new HashSet<>(dbRules.values());
      res.removeAll(unchanged);
      res.removeAll(renamed.keySet());
      res.removeAll(updated);
      res.removeAll(removed);
      return res.stream();
    }

    private Stream<RuleDefinitionDto> getRemoved() {
      return removed.stream();
    }

    public Stream<Map.Entry<RuleDefinitionDto, RuleKey>> getRenamed() {
      return renamed.entrySet().stream();
    }

    private Stream<RuleDefinitionDto> getAllModified() {
      return Stream.of(
        created.stream(),
        updated.stream(),
        removed.stream(),
        renamed.keySet().stream())
        .flatMap(s -> s);
    }

    private boolean isCreated(RuleDefinitionDto ruleDefinition) {
      return created.contains(ruleDefinition);
    }

    private boolean isRenamed(RuleDefinitionDto ruleDefinition) {
      return renamed.containsKey(ruleDefinition);
    }

    private boolean isUpdated(RuleDefinitionDto ruleDefinition) {
      return updated.contains(ruleDefinition);
    }

    private void created(RuleDefinitionDto ruleDefinition) {
      checkState(!known.contains(ruleDefinition), "known RuleDefinitionDto can't be created");
      created.add(ruleDefinition);
    }

    private void renamed(RuleDefinitionDto ruleDefinition) {
      ensureKnown(ruleDefinition);
      renamed.put(ruleDefinition, ruleDefinition.getKey());
    }

    private void updated(RuleDefinitionDto ruleDefinition) {
      ensureKnown(ruleDefinition);
      updated.add(ruleDefinition);
    }

    private void removed(RuleDefinitionDto ruleDefinition) {
      ensureKnown(ruleDefinition);
      removed.add(ruleDefinition);
    }

    private void unchanged(RuleDefinitionDto ruleDefinition) {
      ensureKnown(ruleDefinition);
      unchanged.add(ruleDefinition);
    }

    private void ensureKnown(RuleDefinitionDto ruleDefinition) {
      checkState(known.contains(ruleDefinition), "unknown RuleDefinitionDto");
    }
  }

  private void persistRepositories(DbSession dbSession, List<RulesDefinition.Repository> repositories) {
    List<RuleRepositoryDto> dtos = repositories
      .stream()
      .map(r -> new RuleRepositoryDto(r.key(), r.language(), r.name()))
      .collect(toList(repositories.size()));
    List<String> keys = dtos.stream().map(RuleRepositoryDto::getKey).collect(toList(repositories.size()));
    dbClient.ruleRepositoryDao().insertOrUpdate(dbSession, dtos);
    dbClient.ruleRepositoryDao().deleteIfKeyNotIn(dbSession, keys);
    dbSession.commit();
  }

  @Override
  public void stop() {
    // nothing
  }

  private void registerRule(RegisterRulesContext context, RulesDefinition.Rule ruleDef, DbSession session) {
    RuleKey ruleKey = RuleKey.of(ruleDef.repository().key(), ruleDef.key());

    RuleDefinitionDto ruleDefinitionDto = context.getDbRuleFor(ruleDef)
      .orElseGet(() -> {
        RuleDefinitionDto newRule = createRuleDto(ruleDef, session);
        context.created(newRule);
        return newRule;
      });

    // we must detect renaming __before__ we modify the DTO
    if (!ruleDefinitionDto.getKey().equals(ruleKey)) {
      context.renamed(ruleDefinitionDto);
      ruleDefinitionDto.setRuleKey(ruleKey);
    }

    if (mergeRule(ruleDef, ruleDefinitionDto)) {
      context.updated(ruleDefinitionDto);
    }

    if (mergeDebtDefinitions(ruleDef, ruleDefinitionDto)) {
      context.updated(ruleDefinitionDto);
    }

    if (mergeTags(ruleDef, ruleDefinitionDto)) {
      context.updated(ruleDefinitionDto);
    }

    if (mergeSecurityStandards(ruleDef, ruleDefinitionDto)) {
      context.updated(ruleDefinitionDto);
    }

    if (context.isUpdated(ruleDefinitionDto) || context.isRenamed(ruleDefinitionDto)) {
      update(session, ruleDefinitionDto);
    } else if (!context.isCreated(ruleDefinitionDto)) {
      context.unchanged(ruleDefinitionDto);
    }

    mergeParams(ruleDef, ruleDefinitionDto, session);
    updateDeprecatedKeys(context, ruleDef, ruleDefinitionDto, session);
  }

  private RuleDefinitionDto createRuleDto(RulesDefinition.Rule ruleDef, DbSession session) {
    RuleDefinitionDto ruleDto = new RuleDefinitionDto()
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
      .setUpdatedAt(system2.now());
    if (ruleDef.htmlDescription() != null) {
      ruleDto.setDescription(ruleDef.htmlDescription());
      ruleDto.setDescriptionFormat(Format.HTML);
    } else {
      ruleDto.setDescription(ruleDef.markdownDescription());
      ruleDto.setDescriptionFormat(Format.MARKDOWN);
    }
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

  private static boolean mergeRule(RulesDefinition.Rule def, RuleDefinitionDto dto) {
    boolean changed = false;
    if (!StringUtils.equals(dto.getName(), def.name())) {
      dto.setName(def.name());
      changed = true;
    }
    if (mergeDescription(def, dto)) {
      changed = true;
    }
    if (!StringUtils.equals(dto.getPluginKey(), def.pluginKey())) {
      dto.setPluginKey(def.pluginKey());
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
    boolean isTemplate = def.template();
    if (isTemplate != dto.isTemplate()) {
      dto.setIsTemplate(isTemplate);
      changed = true;
    }
    if (def.status() != dto.getStatus()) {
      dto.setStatus(def.status());
      changed = true;
    }
    if (!StringUtils.equals(dto.getScope().name(), def.scope().name())) {
      dto.setScope(toDtoScope(def.scope()));
      changed = true;
    }
    if (!StringUtils.equals(dto.getLanguage(), def.repository().language())) {
      dto.setLanguage(def.repository().language());
      changed = true;
    }
    RuleType type = RuleType.valueOf(def.type().name());
    if (!ObjectUtils.equals(dto.getType(), type.getDbConstant())) {
      dto.setType(type);
      changed = true;
    }
    if (dto.isAdHoc()) {
      dto.setIsAdHoc(false);
      changed = true;
    }
    return changed;
  }

  private static boolean mergeDescription(RulesDefinition.Rule def, RuleDefinitionDto dto) {
    boolean changed = false;
    if (def.htmlDescription() != null && !StringUtils.equals(dto.getDescription(), def.htmlDescription())) {
      dto.setDescription(def.htmlDescription());
      dto.setDescriptionFormat(Format.HTML);
      changed = true;
    } else if (def.markdownDescription() != null && !StringUtils.equals(dto.getDescription(), def.markdownDescription())) {
      dto.setDescription(def.markdownDescription());
      dto.setDescriptionFormat(Format.MARKDOWN);
      changed = true;
    }
    return changed;
  }

  private static boolean mergeDebtDefinitions(RulesDefinition.Rule def, RuleDefinitionDto dto) {
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

  private static boolean mergeDebtDefinitions(RuleDefinitionDto dto, @Nullable String remediationFunction,
    @Nullable String remediationCoefficient, @Nullable String remediationOffset, @Nullable String effortToFixDescription) {
    boolean changed = false;

    if (!StringUtils.equals(dto.getDefRemediationFunction(), remediationFunction)) {
      dto.setDefRemediationFunction(remediationFunction);
      changed = true;
    }
    if (!StringUtils.equals(dto.getDefRemediationGapMultiplier(), remediationCoefficient)) {
      dto.setDefRemediationGapMultiplier(remediationCoefficient);
      changed = true;
    }
    if (!StringUtils.equals(dto.getDefRemediationBaseEffort(), remediationOffset)) {
      dto.setDefRemediationBaseEffort(remediationOffset);
      changed = true;
    }
    if (!StringUtils.equals(dto.getGapDescription(), effortToFixDescription)) {
      dto.setGapDescription(effortToFixDescription);
      changed = true;
    }
    return changed;
  }

  private void mergeParams(RulesDefinition.Rule ruleDef, RuleDefinitionDto rule, DbSession session) {
    List<RuleParamDto> paramDtos = dbClient.ruleDao().selectRuleParamsByRuleKey(session, rule.getKey());
    Map<String, RuleParamDto> existingParamsByName = Maps.newHashMap();

    Profiler profiler = Profiler.create(Loggers.get(getClass()));
    for (RuleParamDto paramDto : paramDtos) {
      RulesDefinition.Param paramDef = ruleDef.param(paramDto.getName());
      if (paramDef == null) {
        profiler.start();
        dbClient.activeRuleDao().deleteParamsByRuleParamOfAllOrganizations(session, paramDto);
        profiler.stopDebug(format("Propagate deleted param with name %s to active rules of rule %s", paramDto.getName(), rule.getKey()));
        dbClient.ruleDao().deleteRuleParam(session, paramDto.getId());
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
      for (ActiveRuleDto activeRule : dbClient.activeRuleDao().selectByRuleIdOfAllOrganizations(session, rule.getId())) {
        ActiveRuleParamDto activeParam = ActiveRuleParamDto.createFor(paramDto).setValue(param.defaultValue());
        dbClient.activeRuleDao().insertParam(session, activeRule, activeParam);
      }
      profiler.stopDebug(format("Propagate new param with name %s to active rules of rule %s", paramDto.getName(), rule.getKey()));
    }
  }

  private static boolean mergeParam(RuleParamDto paramDto, RulesDefinition.Param paramDef) {
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

  private void updateDeprecatedKeys(RegisterRulesContext context, RulesDefinition.Rule ruleDef, RuleDefinitionDto rule,
    DbSession dbSession) {

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
        .setRuleId(rule.getId())
        .setOldRepositoryKey(r.getOldRepositoryKey())
        .setOldRuleKey(r.getOldRuleKey())
        .setCreatedAt(system2.now())));
  }

  private static boolean mergeTags(RulesDefinition.Rule ruleDef, RuleDefinitionDto dto) {
    boolean changed = false;

    if (RuleStatus.REMOVED == ruleDef.status()) {
      dto.setSystemTags(emptySet());
      changed = true;
    } else if (dto.getSystemTags().size() != ruleDef.tags().size() ||
      !dto.getSystemTags().containsAll(ruleDef.tags())) {
      dto.setSystemTags(ruleDef.tags());
      // FIXME this can't be implemented easily with organization support: remove end-user tags that are now declared as system
      // RuleTagHelper.applyTags(dto, ImmutableSet.copyOf(dto.getTags()));
      changed = true;
    }
    return changed;
  }

  private static boolean mergeSecurityStandards(RulesDefinition.Rule ruleDef, RuleDefinitionDto dto) {
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

  private void processRemainingDbRules(RegisterRulesContext recorder, DbSession dbSession) {
    // custom rules check status of template, so they must be processed at the end
    List<RuleDefinitionDto> customRules = newArrayList();

    recorder.getRemaining().forEach(rule -> {
      if (rule.isCustomRule()) {
        customRules.add(rule);
      } else if (!rule.isAdHoc() && rule.getStatus() != RuleStatus.REMOVED) {
        removeRule(dbSession, recorder, rule);
      }
    });

    for (RuleDefinitionDto customRule : customRules) {
      Integer templateId = customRule.getTemplateId();
      checkNotNull(templateId, "Template id of the custom rule '%s' is null", customRule);
      Optional<RuleDefinitionDto> template = dbClient.ruleDao().selectDefinitionById(templateId, dbSession);
      if (template.isPresent() && template.get().getStatus() != RuleStatus.REMOVED) {
        if (updateCustomRuleFromTemplateRule(customRule, template.get())) {
          update(dbSession, customRule);
        }
      } else {
        removeRule(dbSession, recorder, customRule);
      }
    }

    dbSession.commit();
  }

  private void removeRule(DbSession session, RegisterRulesContext recorder, RuleDefinitionDto rule) {
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

  private static boolean updateCustomRuleFromTemplateRule(RuleDefinitionDto customRule, RuleDefinitionDto templateRule) {
    boolean changed = false;
    if (!StringUtils.equals(customRule.getLanguage(), templateRule.getLanguage())) {
      customRule.setLanguage(templateRule.getLanguage());
      changed = true;
    }
    if (!StringUtils.equals(customRule.getConfigKey(), templateRule.getConfigKey())) {
      customRule.setConfigKey(templateRule.getConfigKey());
      changed = true;
    }
    if (!StringUtils.equals(customRule.getPluginKey(), templateRule.getPluginKey())) {
      customRule.setPluginKey(templateRule.getPluginKey());
      changed = true;
    }
    if (!StringUtils.equals(customRule.getDefRemediationFunction(), templateRule.getDefRemediationFunction())) {
      customRule.setDefRemediationFunction(templateRule.getDefRemediationFunction());
      changed = true;
    }
    if (!StringUtils.equals(customRule.getDefRemediationGapMultiplier(), templateRule.getDefRemediationGapMultiplier())) {
      customRule.setDefRemediationGapMultiplier(templateRule.getDefRemediationGapMultiplier());
      changed = true;
    }
    if (!StringUtils.equals(customRule.getDefRemediationBaseEffort(), templateRule.getDefRemediationBaseEffort())) {
      customRule.setDefRemediationBaseEffort(templateRule.getDefRemediationBaseEffort());
      changed = true;
    }
    if (!StringUtils.equals(customRule.getGapDescription(), templateRule.getGapDescription())) {
      customRule.setGapDescription(templateRule.getGapDescription());
      changed = true;
    }
    if (customRule.getStatus() != templateRule.getStatus()) {
      customRule.setStatus(templateRule.getStatus());
      changed = true;
    }
    if (!StringUtils.equals(customRule.getSeverityString(), templateRule.getSeverityString())) {
      customRule.setSeverity(templateRule.getSeverityString());
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
  private List<ActiveRuleChange> removeActiveRulesOnStillExistingRepositories(DbSession dbSession, RegisterRulesContext recorder,
    List<RulesDefinition.ExtendedRepository> context) {
    List<String> repositoryKeys = context.stream()
      .map(RulesDefinition.ExtendedRepository::key)
      .collect(MoreCollectors.toList(context.size()));

    List<ActiveRuleChange> changes = new ArrayList<>();
    Profiler profiler = Profiler.create(Loggers.get(getClass()));
    recorder.getRemoved().forEach(rule -> {
      // SONAR-4642 Remove active rules only when repository still exists
      if (repositoryKeys.contains(rule.getRepositoryKey())) {
        profiler.start();
        changes.addAll(qProfileRules.deleteRule(dbSession, rule));
        profiler.stopDebug(format("Remove active rule for rule %s", rule.getKey()));
      }
    });
    return changes;
  }

  private void update(DbSession session, RuleDefinitionDto rule) {
    rule.setUpdatedAt(system2.now());
    dbClient.ruleDao().update(session, rule);
  }

  private static void verifyRuleKeyConsistency(List<RulesDefinition.ExtendedRepository> repositories, RegisterRulesContext registerRulesContext) {
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
    ImmutableMap<RuleKey, SingleDeprecatedRuleKey> dbDeprecatedRuleKeysByOldRuleKey = registerRulesContext.getDbDeprecatedKeysByOldRuleKey();

    Set<String> incorrectRuleKeyMessage = definedRules.stream()
      .flatMap(r -> filterInvalidDeprecatedRuleKeys(dbDeprecatedRuleKeysByOldRuleKey, r))
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    checkState(incorrectRuleKeyMessage.isEmpty(), "An incorrect state of deprecated rule keys has been detected.\n %s",
      lazyToString(() -> incorrectRuleKeyMessage.stream().collect(Collectors.joining("\n"))));
  }

  private static Stream<String> filterInvalidDeprecatedRuleKeys(ImmutableMap<RuleKey, SingleDeprecatedRuleKey> dbDeprecatedRuleKeysByOldRuleKey,
    RulesDefinition.Rule rule) {
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
        if (rule.deprecatedRuleKeys().contains(parentRuleKey)) {
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

    list.stream().forEach(t -> {
      if (!uniques.add(t)) {
        duplicates.add(t);
      }
    });

    return duplicates;
  }
}
