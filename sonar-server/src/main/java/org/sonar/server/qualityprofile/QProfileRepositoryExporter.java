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

package org.sonar.server.qualityprofile;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Used through ruby code <pre>Internal.profile_exporter</pre>
 */
public class QProfileRepositoryExporter implements ServerComponent {

  private final DatabaseSessionFactory sessionFactory;
  private final ActiveRuleDao activeRuleDao;
  private final ESActiveRule ruleRegistry;
  private final List<ProfileExporter> exporters;
  private final List<ProfileImporter> importers;

  /**
   * Used by pico when no plugin provide profile exporter / importer
   */
  public QProfileRepositoryExporter(DatabaseSessionFactory sessionFactory, ActiveRuleDao activeRuleDao, ESActiveRule esActiveRule) {
    this(sessionFactory, activeRuleDao, esActiveRule, Lists.<ProfileImporter>newArrayList(), Lists.<ProfileExporter>newArrayList());
  }

  public QProfileRepositoryExporter(DatabaseSessionFactory sessionFactory, ActiveRuleDao activeRuleDao, ESActiveRule esActiveRule,
                                    List<ProfileImporter> importers, List<ProfileExporter> exporters) {
    this.sessionFactory = sessionFactory;
    this.activeRuleDao = activeRuleDao;
    this.ruleRegistry = esActiveRule;
    this.importers = importers;
    this.exporters = exporters;
  }

  public QProfileResult importXml(QProfile profile, String pluginKey, String xml, SqlSession session) {
    QProfileResult result = new QProfileResult();
    ValidationMessages messages = ValidationMessages.create();
    ProfileImporter importer = getProfileImporter(pluginKey);
    RulesProfile rulesProfile = importer.importProfile(new StringReader(xml), messages);
    importProfile(profile.id(), rulesProfile, session);
    processValidationMessages(messages, result);
    return result;
  }

  public String exportToXml(QProfile profile, String pluginKey) {
    DatabaseSession session = sessionFactory.getSession();
    RulesProfile rulesProfile = session.getSingleResult(RulesProfile.class, "id", profile.id());
    if (profile == null) {
      throw new NotFoundException("This profile does not exists.");
    }
    ProfileExporter exporter = getProfileExporter(pluginKey);
    Writer writer = new StringWriter();
    exporter.exportProfile(rulesProfile, writer);
    return writer.toString();
  }

  public String getProfileExporterMimeType(String pluginKey) {
    return getProfileExporter(pluginKey).getMimeType();
  }

  private void importProfile(int profileId, RulesProfile rulesProfile, SqlSession sqlSession) {
    List<ActiveRuleDto> activeRuleDtos = newArrayList();
    Multimap<Integer, ActiveRuleParamDto> paramsByActiveRule = ArrayListMultimap.create();
    for (ActiveRule activeRule : rulesProfile.getActiveRules()) {
      ActiveRuleDto activeRuleDto = toActiveRuleDto(activeRule, profileId);
      activeRuleDao.insert(activeRuleDto, sqlSession);
      activeRuleDtos.add(activeRuleDto);
      for (ActiveRuleParam activeRuleParam : activeRule.getActiveRuleParams()) {
        ActiveRuleParamDto activeRuleParamDto = toActiveRuleParamDto(activeRuleParam, activeRuleDto);
        activeRuleDao.insert(activeRuleParamDto, sqlSession);
        paramsByActiveRule.put(activeRuleDto.getId(), activeRuleParamDto);
      }
    }
    ruleRegistry.bulkIndexActiveRules(activeRuleDtos, paramsByActiveRule);
  }

  private void processValidationMessages(ValidationMessages messages, QProfileResult result) {
    if (!messages.getErrors().isEmpty()) {
      List<BadRequestException.Message> errors = newArrayList();
      for (String error : messages.getErrors()) {
        errors.add(BadRequestException.Message.of(error));
      }
      throw BadRequestException.of("Fail to import profile", errors);
    }
    result.setWarnings(messages.getWarnings());
    result.setInfos(messages.getInfos());
  }

  private ActiveRuleDto toActiveRuleDto(ActiveRule activeRule, int profileId) {
    return new ActiveRuleDto()
      .setProfileId(profileId)
      .setRuleId(activeRule.getRule().getId())
      .setSeverity(toSeverityLevel(activeRule.getSeverity()));
  }

  private String toSeverityLevel(RulePriority rulePriority) {
    return rulePriority.name();
  }

  private ActiveRuleParamDto toActiveRuleParamDto(ActiveRuleParam activeRuleParam, ActiveRuleDto activeRuleDto) {
    return new ActiveRuleParamDto()
      .setActiveRuleId(activeRuleDto.getId())
      .setRulesParameterId(activeRuleParam.getRuleParam().getId())
      .setKey(activeRuleParam.getKey())
      .setValue(activeRuleParam.getValue());
  }

  private ProfileImporter getProfileImporter(String importerKey) {
    for (ProfileImporter importer : importers) {
      if (StringUtils.equals(importerKey, importer.getKey())) {
        return importer;
      }
    }
    throw BadRequestException.of("No such importer : " + importerKey);
  }

  private ProfileExporter getProfileExporter(String exporterKey) {
    for (ProfileExporter exporter : exporters) {
      if (StringUtils.equals(exporterKey, exporter.getKey())) {
        return exporter;
      }
    }
    throw BadRequestException.of("No such exporter : " + exporterKey);
  }

  public List<ProfileExporter> getProfileExportersForLanguage(String language) {
    List<ProfileExporter> result = new ArrayList<ProfileExporter>();
    for (ProfileExporter exporter : exporters) {
      if (exporter.getSupportedLanguages() == null || exporter.getSupportedLanguages().length == 0 || ArrayUtils.contains(exporter.getSupportedLanguages(), language)) {
        result.add(exporter);
      }
    }
    return result;
  }

  public List<ProfileImporter> getProfileImportersForLanguage(String language) {
    List<ProfileImporter> result = new ArrayList<ProfileImporter>();
    for (ProfileImporter importer : importers) {
      if (importer.getSupportedLanguages() == null || importer.getSupportedLanguages().length == 0 || ArrayUtils.contains(importer.getSupportedLanguages(), language)) {
        result.add(importer);
      }
    }
    return result;
  }

}
