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

package org.sonar.server.rules;

import org.sonar.core.preview.PreviewCache;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.profiles.XMLProfileParser;
import org.sonar.api.profiles.XMLProfileSerializer;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public final class ProfilesConsole implements ServerComponent {

  private DatabaseSessionFactory sessionFactory;
  private XMLProfileParser xmlProfileParser;
  private XMLProfileSerializer xmlProfileSerializer;
  private List<ProfileExporter> exporters = newArrayList();
  private List<ProfileImporter> importers = newArrayList();
  private PreviewCache dryRunCache;

  public ProfilesConsole(DatabaseSessionFactory sessionFactory, XMLProfileParser xmlProfileParser, XMLProfileSerializer xmlProfileSerializer, PreviewCache dryRunCache) {
    this.sessionFactory = sessionFactory;
    this.xmlProfileParser = xmlProfileParser;
    this.xmlProfileSerializer = xmlProfileSerializer;
    this.dryRunCache = dryRunCache;
    this.exporters = newArrayList();
    this.importers = newArrayList();
  }

  public ProfilesConsole(DatabaseSessionFactory sessionFactory, XMLProfileParser xmlProfileParser, XMLProfileSerializer xmlProfileSerializer,
    ProfileExporter[] exporters,
    ProfileImporter[] importers, PreviewCache dryRunCache) {
    this(sessionFactory, xmlProfileParser, xmlProfileSerializer, dryRunCache);
    this.exporters.addAll(Arrays.asList(exporters));
    this.importers.addAll(Arrays.asList(importers));
  }

  public String backupProfile(int profileId) {
    DatabaseSession session = sessionFactory.getSession();
    RulesProfile profile = loadProfile(session, profileId);
    if (profile != null) {
      Writer writer = new StringWriter();
      xmlProfileSerializer.write(profile, writer);
      return writer.toString();
    }
    return null;
  }

  public ValidationMessages restoreProfile(String xmlBackup, boolean deleteExisting) {
    ValidationMessages messages = ValidationMessages.create();
    RulesProfile profile = xmlProfileParser.parse(new StringReader(xmlBackup), messages);
    if (profile != null && !messages.hasErrors()) {
      DatabaseSession session = sessionFactory.getSession();
      RulesProfile existingProfile = session.getSingleResult(RulesProfile.class, "name", profile.getName(), "language", profile.getLanguage());
      if (existingProfile != null && !deleteExisting) {
        messages.addErrorText("The profile " + profile + " already exists. Please delete it before restoring.");

      } else {
        if (existingProfile != null) {
          session.removeWithoutFlush(existingProfile);
        }
        session.saveWithoutFlush(profile);
        session.commit();
        dryRunCache.reportGlobalModification();
      }
    }
    return messages;
  }

  private RulesProfile loadProfile(DatabaseSession session, int profileId) {
    return session.getSingleResult(RulesProfile.class, "id", profileId);
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

  public String exportProfile(int profileId, String exporterKey) {
    DatabaseSession session = sessionFactory.getSession();
    RulesProfile profile = loadProfile(session, profileId);
    if (profile != null) {
      ProfileExporter exporter = getProfileExporter(exporterKey);
      if (exporter == null) {
        throw new IllegalArgumentException("No such exporter");
      }
      Writer writer = new StringWriter();
      exporter.exportProfile(profile, writer);
      return writer.toString();
    }
    return null;
  }

  /**
   * Important : the ruby controller has already create the profile
   */
  public ValidationMessages importProfile(String profileName, String language, String importerKey, String profileDefinition) {
    ValidationMessages messages = ValidationMessages.create();
    ProfileImporter importer = getProfileImporter(importerKey);
    RulesProfile profile = importer.importProfile(new StringReader(profileDefinition), messages);
    if (!messages.hasErrors()) {
      DatabaseSession session = sessionFactory.getSession();
      RulesProfile persistedProfile = session.getSingleResult(RulesProfile.class, "name", profileName, "language", language);
      for (ActiveRule activeRule : profile.getActiveRules()) {
        ActiveRule persistedActiveRule = persistedProfile.activateRule(activeRule.getRule(), activeRule.getSeverity());
        for (ActiveRuleParam activeRuleParam : activeRule.getActiveRuleParams()) {
          persistedActiveRule.setParameter(activeRuleParam.getKey(), activeRuleParam.getValue());
        }
      }
      session.saveWithoutFlush(persistedProfile);
      session.commit();
    }
    return messages;
  }

  public ProfileExporter getProfileExporter(String exporterKey) {
    for (ProfileExporter exporter : exporters) {
      if (StringUtils.equals(exporterKey, exporter.getKey())) {
        return exporter;
      }
    }
    return null;
  }

  public ProfileImporter getProfileImporter(String exporterKey) {
    for (ProfileImporter importer : importers) {
      if (StringUtils.equals(exporterKey, importer.getKey())) {
        return importer;
      }
    }
    return null;
  }
}
