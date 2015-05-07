/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.qualityprofile;

import com.google.common.base.Charsets;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerSide;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@ServerSide
public class QProfileExporters {

  private final QProfileLoader loader;
  private final RuleFinder ruleFinder;
  private final RuleActivator ruleActivator;
  private final ProfileExporter[] exporters;
  private final ProfileImporter[] importers;

  public QProfileExporters(QProfileLoader loader, RuleFinder ruleFinder, RuleActivator ruleActivator, ProfileExporter[] exporters, ProfileImporter[] importers) {
    this.loader = loader;
    this.ruleFinder = ruleFinder;
    this.ruleActivator = ruleActivator;
    this.exporters = exporters;
    this.importers = importers;
  }

  public QProfileExporters(QProfileLoader loader, RuleFinder ruleFinder, RuleActivator ruleActivator) {
    this(loader, ruleFinder, ruleActivator, new ProfileExporter[0], new ProfileImporter[0]);
  }

  public List<ProfileExporter> exportersForLanguage(String language) {
    List<ProfileExporter> result = new ArrayList<ProfileExporter>();
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

  public void export(String profileKey, String exporterKey, Writer writer) {
    ProfileExporter exporter = findExporter(exporterKey);
    QualityProfileDto profile = loader.getByKey(profileKey);
    if (profile == null) {
      throw new NotFoundException("Unknown Quality profile: " + profileKey);
    }
    exporter.exportProfile(wrap(profile), writer);
  }

  /**
   * Only for ruby on rails
   */
  public String export(String profileKey, String tool) {
    StringWriter writer = new StringWriter();
    export(profileKey, tool, writer);
    return writer.toString();
  }

  private RulesProfile wrap(QualityProfileDto profile) {
    RulesProfile target = new RulesProfile(profile.getName(), profile.getLanguage());
    for (Iterator<ActiveRule> activeRuleIterator = loader.findActiveRulesByProfile(profile.getKey()); activeRuleIterator.hasNext();) {
      ActiveRule activeRule = activeRuleIterator.next();
      Rule rule = ruleFinder.findByKey(activeRule.key().ruleKey());
      org.sonar.api.rules.ActiveRule wrappedActiveRule = target.activateRule(rule, RulePriority.valueOf(activeRule.severity()));
      for (Map.Entry<String, String> entry : activeRule.params().entrySet()) {
        wrappedActiveRule.setParameter(entry.getKey(), entry.getValue());
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

  /**
   * Used by rails
   */
  public List<ProfileImporter> findProfileImportersForLanguage(String language) {
    List<ProfileImporter> result = new ArrayList<ProfileImporter>();
    for (ProfileImporter importer : importers) {
      if (importer.getSupportedLanguages() == null || importer.getSupportedLanguages().length == 0 || ArrayUtils.contains(importer.getSupportedLanguages(), language)) {
        result.add(importer);
      }
    }
    return result;
  }

  public QProfileResult importXml(QualityProfileDto profileDto, String importerKey, String xml, DbSession dbSession) {
    return importXml(profileDto, importerKey, new StringReader(xml), dbSession);
  }

  public QProfileResult importXml(QualityProfileDto profileDto, String importerKey, InputStream xml, DbSession dbSession) {
    return importXml(profileDto, importerKey, new InputStreamReader(xml, Charsets.UTF_8), dbSession);
  }

  public QProfileResult importXml(QualityProfileDto profileDto, String importerKey, Reader xml, DbSession dbSession) {
    QProfileResult result = new QProfileResult();
    ValidationMessages messages = ValidationMessages.create();
    ProfileImporter importer = getProfileImporter(importerKey);
    RulesProfile rulesProfile = importer.importProfile(xml, messages);
    importProfile(profileDto, rulesProfile, dbSession);
    processValidationMessages(messages, result);
    return result;
  }

  private void importProfile(QualityProfileDto profileDto, RulesProfile rulesProfile, DbSession dbSession) {
    for (org.sonar.api.rules.ActiveRule activeRule : rulesProfile.getActiveRules()) {
      ruleActivator.activate(dbSession, toRuleActivation(activeRule), profileDto);
    }
  }

  private ProfileImporter getProfileImporter(String importerKey) {
    for (ProfileImporter importer : importers) {
      if (StringUtils.equals(importerKey, importer.getKey())) {
        return importer;
      }
    }
    throw new BadRequestException("No such importer : " + importerKey);
  }

  private void processValidationMessages(ValidationMessages messages, QProfileResult result) {
    if (!messages.getErrors().isEmpty()) {
      throw new BadRequestException(messages);
    }
    result.addWarnings(messages.getWarnings());
    result.addInfos(messages.getInfos());
  }

  private RuleActivation toRuleActivation(org.sonar.api.rules.ActiveRule activeRule) {
    RuleActivation ruleActivation = new RuleActivation(activeRule.getRule().ruleKey());
    ruleActivation.setSeverity(activeRule.getSeverity().name());
    for (ActiveRuleParam activeRuleParam : activeRule.getActiveRuleParams()) {
      ruleActivation.setParameter(activeRuleParam.getKey(), activeRuleParam.getValue());
    }
    return ruleActivation;
  }

}
