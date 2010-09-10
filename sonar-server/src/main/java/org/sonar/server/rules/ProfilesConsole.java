/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.rules;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.profiles.*;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleProvider;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class ProfilesConsole implements ServerComponent {

  private DatabaseSessionFactory sessionFactory;
  private RuleProvider ruleProvider;
  private List<ProfileExporter> profileExporters = new ArrayList<ProfileExporter>();
  private List<ProfileImporter> profileImporters = new ArrayList<ProfileImporter>();

  public ProfilesConsole(DatabaseSessionFactory sessionFactory, RuleProvider ruleProvider,
                         ProfileExporter[] exporters, DeprecatedProfileExporters deprecatedExporters,
                         ProfileImporter[] importers) {
    this.ruleProvider = ruleProvider;
    this.sessionFactory = sessionFactory;
    initProfileExporters(exporters, deprecatedExporters);
    this.profileImporters.addAll(Arrays.asList(importers));
  }

  private void initProfileExporters(ProfileExporter[] exporters, DeprecatedProfileExporters deprecatedExporters) {
    this.profileExporters.addAll(Arrays.asList(exporters));
    for (ProfileExporter exporter : deprecatedExporters.create()) {
      this.profileExporters.add(exporter);
    }
  }

  public String backupProfile(int profileId) {
    DatabaseSession session = sessionFactory.getSession();
    RulesProfile profile = loadProfile(session, profileId);
    if (profile != null) {
      Writer writer = new StringWriter();
      XMLProfileExporter.create().exportProfile(profile, writer);
      return writer.toString();
    }
    return null;
  }

  public ValidationMessages restoreProfile(String profileName, String language, String xmlBackup) {
    DatabaseSession session = sessionFactory.getSession();
    RulesProfile profile = session.getSingleResult(RulesProfile.class, "name", profileName, "language", language);
    if (profile != null) {
      session.remove(session);
    }
    profile = RulesProfile.create(profileName, language);
    ValidationMessages messages = ValidationMessages.create();
    ProfilePrototype prototype = XMLProfileImporter.create().importProfile(new StringReader(xmlBackup), messages);
    completeProfileWithPrototype(profile, prototype, messages);
    session.saveWithoutFlush(profile);
    session.commit();
    return messages;
  }

  private void completeProfileWithPrototype(RulesProfile profile, ProfilePrototype prototype, ValidationMessages messages) {
    for (ProfilePrototype.RulePrototype rulePrototype : prototype.getRules()) {
      Rule rule = findRule(rulePrototype);
      if (rule == null) {
        messages.addWarningText("The following rule has been ignored: " + rulePrototype);

      } else {
        ActiveRule activeRule = profile.activateRule(rule, rulePrototype.getPriority());
        for (Map.Entry<String, String> entry : rulePrototype.getParameters().entrySet()) {
          if (rule.getParam(entry.getKey())==null) {
            messages.addWarningText("The rule " + rulePrototype + " has no parameter named '" + entry.getKey() + "'.");

          } else {
            activeRule.setParameter(entry.getKey(), entry.getValue());
          }
        }
      }
    }
  }

  private Rule findRule(ProfilePrototype.RulePrototype rulePrototype) {
    if (StringUtils.isNotBlank(rulePrototype.getKey())) {
      return ruleProvider.findByKey(rulePrototype.getRepositoryKey(), rulePrototype.getKey());
    }
    if (StringUtils.isNotBlank(rulePrototype.getConfigKey())) {
      return ruleProvider.find(RuleQuery.create().withRepositoryKey(rulePrototype.getRepositoryKey()).withConfigKey(rulePrototype.getConfigKey()));
    }
    return null;
  }

  private RulesProfile loadProfile(DatabaseSession session, int profileId) {
    return session.getSingleResult(RulesProfile.class, "id", profileId);
  }

  public List<ProfileExporter> getProfileExportersForLanguage(String language) {
    List<ProfileExporter> result = new ArrayList<ProfileExporter>();
    for (ProfileExporter exporter : profileExporters) {
      if (exporter.getSupportedLanguages() == null || exporter.getSupportedLanguages().length == 0 || ArrayUtils.contains(exporter.getSupportedLanguages(), language)) {
        result.add(exporter);
      }
    }
    return result;
  }

  public List<ProfileImporter> getProfileImportersForLanguage(String language) {
    List<ProfileImporter> result = new ArrayList<ProfileImporter>();
    for (ProfileImporter importer : profileImporters) {
      if (importer.getSupportedLanguages() == null || importer.getSupportedLanguages().length == 0 || ArrayUtils.contains(importer.getSupportedLanguages(), language)) {
        result.add(importer);
      }
    }
    return result;
  }

  public String exportProfile(int profileId, String exporterKey) {
    DatabaseSession session = sessionFactory.getSession();
    RulesProfile profile = loadProfile(session, profileId);
    if (profile != null) {
      ProfileExporter exporter = getProfileExporter(exporterKey);
      Writer writer = new StringWriter();
      exporter.exportProfile(profile, writer);
      return writer.toString();
    }
    return null;
  }

  public ValidationMessages importProfile(int profileId, String importerKey, String profileDefinition) {
    ValidationMessages messages = ValidationMessages.create();
    ProfileImporter importer = getProfileImporter(importerKey);
    ProfilePrototype prototype = importer.importProfile(new StringReader(profileDefinition), messages);
    if (!messages.hasErrors()) {
      DatabaseSession session = sessionFactory.getSession();
      RulesProfile profile = loadProfile(session, profileId); // the profile has been create in the ruby controller
      completeProfileWithPrototype(profile, prototype, messages);
      session.saveWithoutFlush(profile);
      session.commit();
    }
    return messages;
  }

  public ProfileExporter getProfileExporter(String exporterKey) {
    for (ProfileExporter exporter : profileExporters) {
      if (StringUtils.equals(exporterKey, exporter.getKey())) {
        return exporter;
      }
    }
    return null;
  }

  public ProfileImporter getProfileImporter(String exporterKey) {
    for (ProfileImporter importer : profileImporters) {
      if (StringUtils.equals(exporterKey, importer.getKey())) {
        return importer;
      }
    }
    return null;
  }
}
