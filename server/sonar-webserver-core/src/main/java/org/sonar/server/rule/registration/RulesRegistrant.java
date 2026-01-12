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
package org.sonar.server.rule.registration;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.Startable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.qualityprofile.QProfileChangeDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleRepositoryDto;
import org.sonar.server.es.metadata.MetadataIndex;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.QProfileRules;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.PluginRuleUpdate;
import org.sonar.server.rule.RuleDefinitionsLoader;
import org.sonar.server.rule.WebServerRuleFinder;
import org.sonar.server.rule.index.RuleIndexer;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

/**
 * Registers rules at server startup
 */
public class RulesRegistrant implements Startable {

  private static final Logger LOG = Loggers.get(RulesRegistrant.class);

  private final RuleDefinitionsLoader defLoader;
  private final QProfileRules qProfileRules;
  private final DbClient dbClient;
  private final RuleIndexer ruleIndexer;
  private final ActiveRuleIndexer activeRuleIndexer;
  private final System2 system2;
  private final WebServerRuleFinder webServerRuleFinder;
  private final MetadataIndex metadataIndex;
  private final RulesKeyVerifier rulesKeyVerifier;
  private final StartupRuleUpdater startupRuleUpdater;
  private final NewRuleCreator newRuleCreator;
  private final QualityProfileChangesUpdater qualityProfileChangesUpdater;
  private final SonarQubeVersion sonarQubeVersion;
  private final ActiveRulesImpactInitializer activeRulesImpactInitializer;

  public RulesRegistrant(RuleDefinitionsLoader defLoader, QProfileRules qProfileRules, DbClient dbClient, RuleIndexer ruleIndexer,
    ActiveRuleIndexer activeRuleIndexer, System2 system2, WebServerRuleFinder webServerRuleFinder,
    MetadataIndex metadataIndex, RulesKeyVerifier rulesKeyVerifier, StartupRuleUpdater startupRuleUpdater,
    NewRuleCreator newRuleCreator, QualityProfileChangesUpdater qualityProfileChangesUpdater, SonarQubeVersion sonarQubeVersion,
    ActiveRulesImpactInitializer activeRulesImpactInitializer) {
    this.defLoader = defLoader;
    this.qProfileRules = qProfileRules;
    this.dbClient = dbClient;
    this.ruleIndexer = ruleIndexer;
    this.activeRuleIndexer = activeRuleIndexer;
    this.system2 = system2;
    this.webServerRuleFinder = webServerRuleFinder;
    this.metadataIndex = metadataIndex;
    this.rulesKeyVerifier = rulesKeyVerifier;
    this.startupRuleUpdater = startupRuleUpdater;
    this.newRuleCreator = newRuleCreator;
    this.qualityProfileChangesUpdater = qualityProfileChangesUpdater;
    this.sonarQubeVersion = sonarQubeVersion;
    this.activeRulesImpactInitializer = activeRulesImpactInitializer;
  }

  @Override
  public void start() {
    Profiler profiler = Profiler.create(LOG).startInfo("Register rules");
    try (DbSession dbSession = dbClient.openSession(true)) {
      List<RulesDefinition.Repository> repositories = defLoader.load().repositories();
      RulesRegistrationContext rulesRegistrationContext = RulesRegistrationContext.create(dbClient, dbSession);
      rulesKeyVerifier.verifyRuleKeyConsistency(repositories, rulesRegistrationContext);

      for (RulesDefinition.Repository repoDef : repositories) {
        if (repoDef.language() == null) {
          throw new IllegalStateException("Language is mandatory for repository " + repoDef.key());
        }
        Set<PluginRuleUpdate> pluginRuleUpdates = registerRules(rulesRegistrationContext, repoDef.rules(), dbSession);
        if (!repoDef.isExternal()) {
          // External rules are not part of quality profiles
          activeRulesImpactInitializer.createImpactsOnActiveRules(rulesRegistrationContext, repoDef, dbSession);
          qualityProfileChangesUpdater.createQprofileChangesForRuleUpdates(dbSession, pluginRuleUpdates);
        }
        dbSession.commit();
      }
      activeRulesImpactInitializer.markInitialPopulationDone();
      processRemainingDbRules(rulesRegistrationContext, dbSession);
      List<ActiveRuleChange> changes = removeActiveRulesOnStillExistingRepositories(dbSession, rulesRegistrationContext, repositories);
      dbSession.commit();

      persistRepositories(dbSession, repositories);
      // FIXME lack of resiliency, active rules index is corrupted if rule index fails
      // to be updated. Only a single DB commit should be executed.
      ruleIndexer.commitAndIndex(dbSession, rulesRegistrationContext.getAllModified().map(RuleDto::getUuid).collect(Collectors.toSet()));

      List<QProfileChangeDto> qProfileChangeDtos = changes.stream()
        .map(ActiveRuleChange::toSystemChangedDto)
        .peek(dto -> dto.setSqVersion(sonarQubeVersion.toString()))
        .toList();
      dbClient.qProfileChangeDao().bulkInsert(dbSession, qProfileChangeDtos);

      activeRuleIndexer.commitAndIndex(dbSession, changes);
      rulesRegistrationContext.getRenamed().forEach(e -> LOG.info("Rule {} re-keyed to {}", e.getValue(), e.getKey().getKey()));
      profiler.stopDebug();

      if (!rulesRegistrationContext.hasDbRules()) {
        Stream.concat(ruleIndexer.getIndexTypes().stream(), activeRuleIndexer.getIndexTypes().stream())
          .forEach(t -> metadataIndex.setInitialized(t, true));
      }

      webServerRuleFinder.startCaching();
    }
  }

  private void persistRepositories(DbSession dbSession, List<RulesDefinition.Repository> repositories) {
    List<String> keys = repositories.stream().map(RulesDefinition.Repository::key).toList();
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

  private Set<PluginRuleUpdate> registerRules(RulesRegistrationContext context, List<RulesDefinition.Rule> ruleDefs, DbSession session) {
    Map<RulesDefinition.Rule, RuleDto> dtos = LinkedHashMap.newLinkedHashMap(ruleDefs.size());
    Set<PluginRuleUpdate> pluginRuleUpdates = new HashSet<>();

    for (RulesDefinition.Rule ruleDef : ruleDefs) {
      RuleKey ruleKey = RuleKey.of(ruleDef.repository().key(), ruleDef.key());
      RuleDto ruleDto = context.getDbRuleFor(ruleDef).orElseGet(() -> newRuleCreator.createNewRule(context, ruleDef));
      dtos.put(ruleDef, ruleDto);

      // we must detect renaming __before__ we modify the DTO
      if (!ruleDto.getKey().equals(ruleKey)) {
        context.renamed(ruleDto);
        ruleDto.setRuleKey(ruleKey);
      }

      if (!context.isCreated(ruleDto)) {
        processRuleUpdates(context, pluginRuleUpdates, ruleDef, ruleDto);
      }

      if (!context.isUpdated(ruleDto) && !context.isRenamed(ruleDto) && !context.isCreated(ruleDto)) {
        context.unchanged(ruleDto);
      }
    }

    persistRules(context, session, dtos);
    return pluginRuleUpdates;
  }

  private void persistRules(RulesRegistrationContext context, DbSession session, Map<RulesDefinition.Rule, RuleDto> dtos) {
    Map<String, Set<String>> systemTags = new HashMap<>();
    Map<String, Set<String>> tags = new HashMap<>();
    Map<String, Set<RuleDescriptionSectionDto>> sections = new HashMap<>();
    Map<String, Set<ImpactDto>> impacts = new HashMap<>();

    for (Map.Entry<RulesDefinition.Rule, RuleDto> entry : dtos.entrySet()) {
      RulesDefinition.Rule ruleDef = entry.getKey();
      if (context.getDbRuleFor(ruleDef).isEmpty()) {
        RuleDto ruleDto = entry.getValue();
        dbClient.ruleDao().insertShallow(session, ruleDto);
        systemTags.put(ruleDto.getUuid(), ruleDto.getSystemTags());
        tags.put(ruleDto.getUuid(), ruleDto.getTags());
        sections.put(ruleDto.getUuid(), ruleDto.getRuleDescriptionSectionDtos());
        impacts.put(ruleDto.getUuid(), ruleDto.getDefaultImpacts());
      }
    }

    for (Map.Entry<String, Set<String>> entry : systemTags.entrySet()) {
      dbClient.ruleDao().insertRuleTag(session, entry.getKey(), entry.getValue(), true);
    }

    for (Map.Entry<String, Set<String>> entry : tags.entrySet()) {
      dbClient.ruleDao().insertRuleTag(session, entry.getKey(), entry.getValue(), false);
    }

    for (Map.Entry<String, Set<RuleDescriptionSectionDto>> entry : sections.entrySet()) {
      dbClient.ruleDao().insertRuleDescriptionSections(session, entry.getKey(), entry.getValue());
    }

    for (Map.Entry<String, Set<ImpactDto>> entry : impacts.entrySet()) {
      dbClient.ruleDao().insertRuleDefaultImpacts(session, entry.getKey(), entry.getValue());
    }

    for (RuleDto ruleDto : dtos.values()) {
      if (context.isUpdated(ruleDto) || context.isRenamed(ruleDto)) {
        update(session, ruleDto);
      }
    }

    for (Map.Entry<RulesDefinition.Rule, RuleDto> e : dtos.entrySet()) {
      startupRuleUpdater.mergeParams(context, e.getKey(), e.getValue(), session);
      startupRuleUpdater.updateDeprecatedKeys(context, e.getKey(), e.getValue(), session);
    }
  }

  private void processRuleUpdates(RulesRegistrationContext context, Set<PluginRuleUpdate> pluginRuleUpdates, RulesDefinition.Rule ruleDef, RuleDto ruleDto) {
    StartupRuleUpdater.RuleChange change = startupRuleUpdater.findChangesAndUpdateRule(ruleDef, ruleDto);
    if (change.hasRuleDefinitionChanged()) {
      context.updated(ruleDto);
      if (change.getPluginRuleUpdate() != null) {
        pluginRuleUpdates.add(change.getPluginRuleUpdate());
      }
    }
  }

  private void processRemainingDbRules(RulesRegistrationContext recorder, DbSession dbSession) {
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

  private void removeRule(DbSession session, RulesRegistrationContext recorder, RuleDto rule) {
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
   * But if the javascript repository does not exist anymore, then related active rules will not be removed.
   * <p/>
   * The side effect of this approach is that extended repositories will not be managed the same way.
   * If an extended repository does not exist anymore, then related active rules will be removed.
   */
  private List<ActiveRuleChange> removeActiveRulesOnStillExistingRepositories(DbSession dbSession, RulesRegistrationContext recorder, List<RulesDefinition.Repository> context) {
    Set<String> existingAndRenamedRepositories = getExistingAndRenamedRepositories(recorder, context);
    List<ActiveRuleChange> changes = new ArrayList<>();
    Profiler profiler = Profiler.create(LOG);

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

  private static Set<String> getExistingAndRenamedRepositories(RulesRegistrationContext recorder, Collection<RulesDefinition.Repository> context) {
    return Stream.concat(
      context.stream().map(RulesDefinition.ExtendedRepository::key),
      recorder.getRenamed().map(Map.Entry::getValue).map(RuleKey::repository))
      .collect(Collectors.toSet());
  }

  private void update(DbSession session, RuleDto rule) {
    rule.setUpdatedAt(system2.now());
    dbClient.ruleDao().update(session, rule);
  }

}
