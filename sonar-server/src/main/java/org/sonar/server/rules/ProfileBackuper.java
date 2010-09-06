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

import org.sonar.api.ServerComponent;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.profiles.XMLProfileExporter;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.io.StringWriter;
import java.io.Writer;

public final class ProfileBackuper implements ServerComponent {

  private DatabaseSessionFactory sessionFactory;
  private XMLProfileExporter exporter;

  public ProfileBackuper(DatabaseSessionFactory sessionFactory, XMLProfileExporter exporter) {
    this.sessionFactory = sessionFactory;
    this.exporter = exporter;
  }

  public String exportProfile(int profileId) {
    DatabaseSession session = sessionFactory.getSession();
    RulesProfile profile = session.getSingleResult(RulesProfile.class, "id", profileId);
    if (profile != null) {
      Writer writer = new StringWriter();
      exporter.exportProfile(profile, writer);
      return writer.toString();
    }
    return null;
  }
}
