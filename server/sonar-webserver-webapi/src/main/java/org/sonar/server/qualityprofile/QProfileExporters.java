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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;

import static org.sonar.server.exceptions.BadRequestException.checkRequest;

@ServerSide
public class QProfileExporters {

  private final DbClient dbClient;
  private final RuleFinder ruleFinder;
  private final QProfileRules qProfileRules;
  private final ProfileExporter[] exporters;
  private final ProfileImporter[] importers;

  public QProfileExporters(DbClient dbClient, RuleFinder ruleFinder, QProfileRules qProfileRules, ProfileExporter[] exporters, ProfileImporter[] importers) {
    this.dbClient = dbClient;
    this.ruleFinder = ruleFinder;
    this.qProfileRules = qProfileRules;
    this.exporters = exporters;
    this.importers = importers;
  }

  /**
   * Used by Pico if no {@link ProfileImporter} is found
   */
  public QProfileExporters(DbClient dbClient, RuleFinder ruleFinder, QProfileRules qProfileRules, ProfileExporter[] exporters) {
    this(dbClient, ruleFinder, qProfileRules, exporters, new ProfileImporter[0]);
  }

  /**
   * Used by Pico if no {@link ProfileExporter} is found
   */
  public QProfileExporters(DbClient dbClient, RuleFinder ruleFinder, QProfileRules qProfileRules, ProfileImporter[] importers) {
    this(dbClient, ruleFinder, qProfileRules, new ProfileExporter[0], importers);
  }

  /**
   * Used by Pico if no {@link ProfileImporter} nor {@link ProfileExporter} is found
   */
  public QProfileExporters(DbClient dbClient, RuleFinder ruleFinder, QProfileRules qProfileRules) {
    this(dbClient, ruleFinder, qProfileRules, new ProfileExporter[0], new ProfileImporter[0]);
  }

  public List<ProfileExporter> exportersForLanguage(String language) {
    List<ProfileExporter> result = new ArrayList<>();
    for (ProfileExporter exporter : exporters) {
      if (exporter.getSupportedLanguages() == null || exporter.getSupportedLanguages().length == 0 || ArrayUtils.contains(exporter.getSupportedLanguages(), language)) {
        result.add(exporter);
      }
    }
    return result;
  }

  public String mimeType(String exporterKey) {
    ProfileExporter exporter = findExporter(exporterKey);
    return exporter.getMimeType();
  }

  public void export(DbSession dbSession, QProfileDto profile, String exporterKey, Writer writer) {
    ProfileExporter exporter = findExporter(exporterKey);
    exporter.exportProfile(wrap(dbSession, profile), writer);
  }

  private RulesProfile wrap(DbSession dbSession, QProfileDto profile) {
    RulesProfile target = new RulesProfile(profile.getName(), profile.getLanguage());
    List<OrgActiveRuleDto> activeRuleDtos = dbClient.activeRuleDao().selectByProfile(dbSession, profile);
    List<ActiveRuleParamDto> activeRuleParamDtos = dbClient.activeRuleDao().selectParamsByActiveRuleIds(dbSession, Lists.transform(activeRuleDtos, ActiveRuleDto::getId));
    ListMultimap<Integer, ActiveRuleParamDto> activeRuleParamsByActiveRuleId = FluentIterable.from(activeRuleParamDtos).index(ActiveRuleParamDto::getActiveRuleId);

    for (ActiveRuleDto activeRule : activeRuleDtos) {
      // TODO all rules should be loaded by using one query with all active rule keys as parameter
      Rule rule = ruleFinder.findByKey(activeRule.getRuleKey());
      org.sonar.api.rules.ActiveRule wrappedActiveRule = target.activateRule(rule, RulePriority.valueOf(activeRule.getSeverityString()));
      List<ActiveRuleParamDto> paramDtos = activeRuleParamsByActiveRuleId.get(activeRule.getId());
      for (ActiveRuleParamDto activeRuleParamDto : paramDtos) {
        wrappedActiveRule.setParameter(activeRuleParamDto.getKey(), activeRuleParamDto.getValue());
      }
    }
    return target;
  }

  private ProfileExporter findExporter(String exporterKey) {
    for (ProfileExporter e : exporters) {
      if (exporterKey.equals(e.getKey())) {
        return e;
      }
    }
    throw new NotFoundException("Unknown quality profile exporter: " + exporterKey);
  }

  public QProfileResult importXml(QProfileDto profile, String importerKey, InputStream xml, DbSession dbSession) {
    return importXml(profile, importerKey, new InputStreamReader(xml, StandardCharsets.UTF_8), dbSession);
  }

  private QProfileResult importXml(QProfileDto profile, String importerKey, Reader xml, DbSession dbSession) {
    QProfileResult result = new QProfileResult();
    ValidationMessages messages = ValidationMessages.create();
    ProfileImporter importer = getProfileImporter(importerKey);
    RulesProfile definition = importer.importProfile(xml, messages);
    List<ActiveRuleChange> changes = importProfile(profile, definition, dbSession);
    result.addChanges(changes);
    processValidationMessages(messages, result);
    return result;
  }

  private List<ActiveRuleChange> importProfile(QProfileDto profile, RulesProfile definition, DbSession dbSession) {
    Map<RuleKey, RuleDefinitionDto> rulesByRuleKey = dbClient.ruleDao().selectAllDefinitions(dbSession)
      .stream()
      .collect(MoreCollectors.uniqueIndex(RuleDefinitionDto::getKey));
    List<ActiveRule> activeRules = definition.getActiveRules();
    List<RuleActivation> activations = activeRules.stream()
      .map(activeRule -> toRuleActivation(activeRule, rulesByRuleKey))
      .filter(Objects::nonNull)
      .collect(MoreCollectors.toArrayList(activeRules.size()));
    return qProfileRules.activateAndCommit(dbSession, profile, activations);
  }

  private ProfileImporter getProfileImporter(String importerKey) {
    for (ProfileImporter importer : importers) {
      if (StringUtils.equals(importerKey, importer.getKey())) {
        return importer;
      }
    }
    throw BadRequestException.create("No such importer : " + importerKey);
  }

  private static void processValidationMessages(ValidationMessages messages, QProfileResult result) {
    checkRequest(messages.getErrors().isEmpty(), messages.getErrors());
    result.addWarnings(messages.getWarnings());
    result.addInfos(messages.getInfos());
  }

  @CheckForNull
  private static RuleActivation toRuleActivation(ActiveRule activeRule, Map<RuleKey, RuleDefinitionDto> rulesByRuleKey) {
    RuleKey ruleKey = activeRule.getRule().ruleKey();
    RuleDefinitionDto ruleDefinition = rulesByRuleKey.get(ruleKey);
    if (ruleDefinition == null) {
      return null;
    }
    String severity = activeRule.getSeverity().name();
    Map<String, String> params = activeRule.getActiveRuleParams().stream()
      .collect(MoreCollectors.uniqueIndex(ActiveRuleParam::getKey, ActiveRuleParam::getValue));
    return RuleActivation.create(ruleDefinition.getId(), severity, params);
  }

}
