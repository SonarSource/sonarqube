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
package org.sonar.server.qualityprofile;

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ExportRuleDto;
import org.sonar.db.qualityprofile.ExportRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.DeprecatedRuleKeyDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.common.rule.RuleCreator;
import org.sonar.server.common.rule.service.NewCustomRule;
import org.sonar.server.qualityprofile.builtin.QProfileName;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;
import static org.sonar.server.qualityprofile.QProfileUtils.parseImpactsToMap;

@ServerSide
public class QProfileBackuperImpl implements QProfileBackuper {

  private final DbClient db;
  private final QProfileReset profileReset;
  private final QProfileFactory profileFactory;
  private final RuleCreator ruleCreator;
  private final QProfileParser qProfileParser;

  public QProfileBackuperImpl(DbClient db, QProfileReset profileReset, QProfileFactory profileFactory,
    RuleCreator ruleCreator, QProfileParser qProfileParser) {
    this.db = db;
    this.profileReset = profileReset;
    this.profileFactory = profileFactory;
    this.ruleCreator = ruleCreator;
    this.qProfileParser = qProfileParser;
  }

  @Override
  public void backup(DbSession dbSession, QProfileDto profile, Writer writer) {
    List<ExportRuleDto> rulesToExport = db.qualityProfileExportDao().selectRulesByProfile(dbSession, profile);
    rulesToExport.sort(BackupActiveRuleComparator.INSTANCE);
    qProfileParser.writeXml(writer, profile, rulesToExport.iterator());
  }

  @Override
  public QProfileRestoreSummary copy(DbSession dbSession, QProfileDto from, QProfileDto to) {
    List<ExportRuleDto> rulesToExport = db.qualityProfileExportDao().selectRulesByProfile(dbSession, from);
    rulesToExport.sort(BackupActiveRuleComparator.INSTANCE);

    ImportedQProfile qProfile = toImportedQProfile(rulesToExport, to.getName(), to.getLanguage());
    return restore(dbSession, qProfile, name -> to);
  }

  private static ImportedQProfile toImportedQProfile(List<ExportRuleDto> exportRules, String profileName, String profileLang) {
    List<ImportedRule> importedRules = new ArrayList<>(exportRules.size());

    for (ExportRuleDto exportRuleDto : exportRules) {
      var ruleKey = exportRuleDto.getRuleKey();
      ImportedRule importedRule = new ImportedRule();
      importedRule.setName(exportRuleDto.getName());
      importedRule.setRepository(ruleKey.repository());
      importedRule.setKey(ruleKey.rule());
      importedRule.setSeverity(exportRuleDto.getSeverityString());
      importedRule.setImpacts(exportRuleDto.getImpacts() != null ? parseImpactsToMap(exportRuleDto.getImpacts()) : Map.of());
      if (exportRuleDto.isCustomRule()) {
        importedRule.setTemplate(exportRuleDto.getTemplateRuleKey().rule());
        importedRule.setDescription(exportRuleDto.getDescriptionOrThrow());
      }
      importedRule.setType(exportRuleDto.getRuleType().name());
      importedRule.setParameters(exportRuleDto.getParams().stream().collect(Collectors.toMap(ExportRuleParamDto::getKey, ExportRuleParamDto::getValue)));
      importedRules.add(importedRule);
    }

    return new ImportedQProfile(profileName, profileLang, importedRules);
  }

  @Override
  public QProfileRestoreSummary restore(DbSession dbSession, Reader backup, @Nullable String overriddenProfileName) {
    return restore(dbSession, backup, nameInBackup -> {
      QProfileName targetName = nameInBackup;
      if (overriddenProfileName != null) {
        targetName = new QProfileName(nameInBackup.getLanguage(), overriddenProfileName);
      }
      return profileFactory.getOrCreateCustom(dbSession, targetName);
    });
  }

  @Override
  public QProfileRestoreSummary restore(DbSession dbSession, Reader backup, QProfileDto profile) {
    return restore(dbSession, backup, nameInBackup -> {
      checkArgument(profile.getLanguage().equals(nameInBackup.getLanguage()),
        "Can't restore %s backup on %s profile with key [%s]. Languages are different.", nameInBackup.getLanguage(), profile.getLanguage(), profile.getKee());
      return profile;
    });
  }

  private QProfileRestoreSummary restore(DbSession dbSession, Reader backup, Function<QProfileName, QProfileDto> profileLoader) {
    ImportedQProfile qProfile = qProfileParser.readXml(backup);
    return restore(dbSession, qProfile, profileLoader);
  }

  private QProfileRestoreSummary restore(DbSession dbSession, ImportedQProfile qProfile, Function<QProfileName, QProfileDto> profileLoader) {
    QProfileName targetName = new QProfileName(qProfile.getProfileLang(), qProfile.getProfileName());
    QProfileDto targetProfile = profileLoader.apply(targetName);

    List<ImportedRule> importedRules = qProfile.getRules();

    Map<RuleKey, RuleDto> ruleKeyToDto = getImportedRulesDtos(dbSession, importedRules);
    checkIfRulesFromExternalEngines(ruleKeyToDto.values());

    Map<RuleKey, RuleDto> customRulesDefinitions = createCustomRulesIfNotExist(dbSession, importedRules, ruleKeyToDto);
    ruleKeyToDto.putAll(customRulesDefinitions);

    List<RuleActivation> ruleActivations = toRuleActivations(importedRules, ruleKeyToDto);

    BulkChangeResult changes = profileReset.reset(dbSession, targetProfile, ruleActivations);
    return new QProfileRestoreSummary(targetProfile, changes);
  }

  /**
   * Returns map of rule definition for an imported rule key.
   * The imported rule key may refer to a deprecated rule key, in which case the the RuleDto will correspond to a different key (the new key).
   */
  private Map<RuleKey, RuleDto> getImportedRulesDtos(DbSession dbSession, List<ImportedRule> rules) {
    Set<RuleKey> ruleKeys = rules.stream()
      .map(ImportedRule::getRuleKey)
      .collect(toSet());
    Map<RuleKey, RuleDto> ruleDtos = db.ruleDao().selectByKeys(dbSession, ruleKeys).stream()
      .collect(Collectors.toMap(RuleDto::getKey, identity()));

    Set<RuleKey> unrecognizedRuleKeys = ruleKeys.stream()
      .filter(r -> !ruleDtos.containsKey(r))
      .collect(toSet());

    if (!unrecognizedRuleKeys.isEmpty()) {
      Map<String, DeprecatedRuleKeyDto> deprecatedRuleKeysByUuid = db.ruleDao().selectAllDeprecatedRuleKeys(dbSession).stream()
        .filter(r -> r.getNewRepositoryKey() != null && r.getNewRuleKey() != null)
        .filter(r -> unrecognizedRuleKeys.contains(RuleKey.of(r.getOldRepositoryKey(), r.getOldRuleKey())))
        // ignore deprecated rule if the new rule key was already found in the list of imported rules
        .filter(r -> !ruleKeys.contains(RuleKey.of(r.getNewRepositoryKey(), r.getNewRuleKey())))
        .collect(Collectors.toMap(DeprecatedRuleKeyDto::getRuleUuid, identity()));

      List<RuleDto> rulesBasedOnDeprecatedKeys = db.ruleDao().selectByUuids(dbSession, deprecatedRuleKeysByUuid.keySet());
      for (RuleDto rule : rulesBasedOnDeprecatedKeys) {
        DeprecatedRuleKeyDto deprecatedRuleKey = deprecatedRuleKeysByUuid.get(rule.getUuid());
        RuleKey oldRuleKey = RuleKey.of(deprecatedRuleKey.getOldRepositoryKey(), deprecatedRuleKey.getOldRuleKey());
        ruleDtos.put(oldRuleKey, rule);
      }
    }

    return ruleDtos;
  }

  private static void checkIfRulesFromExternalEngines(Collection<RuleDto> ruleDefinitions) {
    List<RuleDto> externalRules = ruleDefinitions.stream()
      .filter(RuleDto::isExternal)
      .toList();

    if (!externalRules.isEmpty()) {
      throw new IllegalArgumentException("The quality profile cannot be restored as it contains rules from external rule engines: "
        + externalRules.stream().map(r -> r.getKey().toString()).collect(Collectors.joining(", ")));
    }
  }

  private Map<RuleKey, RuleDto> createCustomRulesIfNotExist(DbSession dbSession, List<ImportedRule> rules, Map<RuleKey, RuleDto> ruleDefinitionsByKey) {
    List<NewCustomRule> customRulesToCreate = rules.stream()
      .filter(r -> ruleDefinitionsByKey.get(r.getRuleKey()) == null && r.isCustomRule())
      .map(QProfileBackuperImpl::importedRuleToNewCustomRule)
      .toList();

    if (!customRulesToCreate.isEmpty()) {
      return db.ruleDao().selectByKeys(dbSession, ruleCreator.restore(dbSession, customRulesToCreate).stream().map(RuleDto::getKey).toList())
        .stream()
        .collect(Collectors.toMap(RuleDto::getKey, identity()));
    }
    return Collections.emptyMap();
  }

  private static NewCustomRule importedRuleToNewCustomRule(ImportedRule r) {
    return NewCustomRule.createForCustomRule(r.getRuleKey(), r.getTemplateKey())
      .setName(r.getName())
      .setSeverity(r.getSeverity())
      .setImpacts(r.getImpacts().entrySet().stream().map(i -> new NewCustomRule.Impact(i.getKey(), i.getValue())).toList())
      .setStatus(RuleStatus.READY)
      .setPreventReactivation(true)
      .setType(RuleType.valueOf(r.getType()))
      .setMarkdownDescription(r.getDescription())
      .setParameters(r.getParameters());
  }

  private static List<RuleActivation> toRuleActivations(List<ImportedRule> rules, Map<RuleKey, RuleDto> ruleDefinitionsByKey) {
    List<RuleActivation> activatedRule = new ArrayList<>();

    for (ImportedRule r : rules) {
      RuleDto ruleDto = ruleDefinitionsByKey.get(r.getRuleKey());
      if (ruleDto == null) {
        continue;
      }
      activatedRule.add(RuleActivation.create(
        ruleDto.getUuid(),
        r.getSeverity(),
        r.getImpacts(),
        r.getPrioritizedRule(),
        r.getParameters()));
    }
    return activatedRule;
  }

  private enum BackupActiveRuleComparator implements Comparator<ExportRuleDto> {
    INSTANCE;

    @Override
    public int compare(ExportRuleDto o1, ExportRuleDto o2) {
      RuleKey rk1 = o1.getRuleKey();
      RuleKey rk2 = o2.getRuleKey();
      return new CompareToBuilder()
        .append(rk1.repository(), rk2.repository())
        .append(rk1.rule(), rk2.rule())
        .toComparison();
    }
  }
}
