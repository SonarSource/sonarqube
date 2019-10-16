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
package org.sonar.server.qualityprofile;

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ServerSide;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ExportRuleDto;
import org.sonar.db.qualityprofile.ExportRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.rule.NewCustomRule;
import org.sonar.server.rule.RuleCreator;

import static com.google.common.base.Preconditions.checkArgument;

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
      ImportedRule importedRule = new ImportedRule();
      importedRule.setName(exportRuleDto.getName());
      importedRule.setDescription(exportRuleDto.getDescription());
      importedRule.setRuleKey(exportRuleDto.getRuleKey());
      importedRule.setSeverity(exportRuleDto.getSeverityString());
      if (importedRule.isCustomRule()) {
        importedRule.setTemplateKey(exportRuleDto.getTemplateRuleKey());
      }
      importedRule.setType(exportRuleDto.getRuleType().name());
      importedRule.setParameters(exportRuleDto.getParams().stream().collect(Collectors.toMap(ExportRuleParamDto::getKey, ExportRuleParamDto::getValue)));
      importedRules.add(importedRule);
    }

    return new ImportedQProfile(profileName, profileLang, importedRules);
  }

  @Override
  public QProfileRestoreSummary restore(DbSession dbSession, Reader backup, OrganizationDto organization, @Nullable String overriddenProfileName) {
    return restore(dbSession, backup, nameInBackup -> {
      QProfileName targetName = nameInBackup;
      if (overriddenProfileName != null) {
        targetName = new QProfileName(nameInBackup.getLanguage(), overriddenProfileName);
      }
      return profileFactory.getOrCreateCustom(dbSession, organization, targetName);
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

    Map<RuleKey, RuleDefinitionDto> ruleDefinitionsByKey = getImportedRulesDefinitions(dbSession, importedRules);
    checkIfRulesFromExternalEngines(ruleDefinitionsByKey);

    Map<RuleKey, RuleDefinitionDto> customRulesDefinitions = createCustomRulesIfNotExist(dbSession, importedRules, ruleDefinitionsByKey);
    ruleDefinitionsByKey.putAll(customRulesDefinitions);

    List<RuleActivation> ruleActivations = toRuleActivations(importedRules, ruleDefinitionsByKey);

    BulkChangeResult changes = profileReset.reset(dbSession, targetProfile, ruleActivations);
    return new QProfileRestoreSummary(targetProfile, changes);
  }

  private Map<RuleKey, RuleDefinitionDto> getImportedRulesDefinitions(DbSession dbSession, List<ImportedRule> rules) {
    List<RuleKey> ruleKeys = rules.stream()
      .map(ImportedRule::getRuleKey)
      .collect(MoreCollectors.toList());
    return db.ruleDao().selectDefinitionByKeys(dbSession, ruleKeys)
      .stream()
      .collect(Collectors.toMap(RuleDefinitionDto::getKey, Function.identity()));
  }

  private static void checkIfRulesFromExternalEngines(Map<RuleKey, RuleDefinitionDto> ruleDefinitionsByKey) {
    List<RuleDefinitionDto> externalRules = ruleDefinitionsByKey.values().stream()
      .filter(RuleDefinitionDto::isExternal)
      .collect(Collectors.toList());

    if (!externalRules.isEmpty()) {
      throw new IllegalArgumentException("The quality profile cannot be restored as it contains rules from external rule engines: "
        + externalRules.stream().map(r -> r.getKey().toString()).collect(Collectors.joining(", ")));
    }
  }

  private Map<RuleKey, RuleDefinitionDto> createCustomRulesIfNotExist(DbSession dbSession, List<ImportedRule> rules, Map<RuleKey, RuleDefinitionDto> ruleDefinitionsByKey) {
    List<NewCustomRule> customRulesToCreate = rules.stream()
      .filter(r -> ruleDefinitionsByKey.get(r.getRuleKey()) == null && r.isCustomRule())
      .map(QProfileBackuperImpl::importedRuleToNewCustomRule)
      .collect(Collectors.toList());

    if (!customRulesToCreate.isEmpty()) {
      return db.ruleDao().selectDefinitionByKeys(dbSession, ruleCreator.create(dbSession, customRulesToCreate))
        .stream()
        .collect(Collectors.toMap(RuleDefinitionDto::getKey, Function.identity()));
    }
    return Collections.emptyMap();
  }

  private static NewCustomRule importedRuleToNewCustomRule(ImportedRule r) {
    return NewCustomRule.createForCustomRule(r.getRuleKey().rule(), r.getTemplateKey())
      .setName(r.getName())
      .setMarkdownDescription(r.getDescription())
      .setSeverity(r.getSeverity())
      .setStatus(RuleStatus.READY)
      .setPreventReactivation(true)
      .setType(RuleType.valueOf(r.getType()))
      .setParameters(r.getParameters());
  }

  private static List<RuleActivation> toRuleActivations(List<ImportedRule> rules, Map<RuleKey, RuleDefinitionDto> ruleDefinitionsByKey) {
    return rules.stream()
      .map(r -> {
        RuleDefinitionDto ruleDefinition = ruleDefinitionsByKey.get(r.getRuleKey());
        if (ruleDefinition == null) {
          return null;
        }
        return RuleActivation.create(ruleDefinition.getId(), r.getSeverity(), r.getParameters());
      })
      .filter(Objects::nonNull)
      .collect(MoreCollectors.toList(rules.size()));
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
