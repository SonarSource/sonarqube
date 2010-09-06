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
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.profiles.XMLProfileExporter;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ProfilesConsole implements ServerComponent {

  private DatabaseSessionFactory sessionFactory;
  private List<ProfileExporter> profileExporters = new ArrayList<ProfileExporter>();


  public ProfilesConsole(DatabaseSessionFactory sessionFactory,
                         ProfileExporter[] exporters, DeprecatedProfileExporters deprecatedExporters) {
    this.sessionFactory = sessionFactory;
    initProfileExporters(exporters, deprecatedExporters);
  }

  private void initProfileExporters(ProfileExporter[] exporters, DeprecatedProfileExporters deprecatedExporters) {
    this.profileExporters.addAll(Arrays.asList(exporters));
    for (ProfileExporter exporter : deprecatedExporters.create()) {
      this.profileExporters.add(exporter);
    }
  }

  public String backupProfile(int profileId) {
    RulesProfile profile = loadProfile(profileId);
    if (profile != null) {
      Writer writer = new StringWriter();
      XMLProfileExporter.create().exportProfile(profile, writer);
      return writer.toString();
    }
    return null;
  }

  private RulesProfile loadProfile(int profileId) {
    DatabaseSession session = sessionFactory.getSession();
    RulesProfile profile = session.getSingleResult(RulesProfile.class, "id", profileId);
    return profile;
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

  public String exportProfile(int profileId, String exporterKey) {
    RulesProfile profile = loadProfile(profileId);
    if (profile != null) {
      ProfileExporter exporter = getProfileExporter(exporterKey);
      Writer writer = new StringWriter();
      exporter.exportProfile(profile, writer);
      return writer.toString();
    }
    return null;
  }

  public ProfileExporter getProfileExporter(String exporterKey) {
    for (ProfileExporter exporter : profileExporters) {
      if (StringUtils.equals(exporterKey, exporter.getKey())) {
        return exporter;
      }
    }
    return null;
  }
}
