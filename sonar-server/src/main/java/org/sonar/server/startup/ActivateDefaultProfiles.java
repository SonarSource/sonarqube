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
package org.sonar.server.startup;

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Language;
import org.sonar.api.utils.Logs;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.util.Iterator;
import java.util.List;

public final class ActivateDefaultProfiles {

  private final DatabaseSessionFactory sessionFactory;
  private final Language[] languages;

  // NOSONAR the parameter registerProfilesBefore is used to define the execution order of startup components
  public ActivateDefaultProfiles(DatabaseSessionFactory sessionFactory, Language[] languages, RegisterProvidedProfiles registerProfilesBefore) {
    this.sessionFactory = sessionFactory;
    this.languages = languages;
  }

  public void start() {
    DatabaseSession session = sessionFactory.getSession();
    for (Language language : languages) {
      Logs.INFO.info("Activate default profile for ", language.getKey());
      activateDefaultProfile(language, session);
    }
    session.commit();
  }

  public void activateDefaultProfile(Language language, DatabaseSession session) {
    List<RulesProfile> profiles = session.getResults(RulesProfile.class, "language", language.getKey());
    RulesProfile profileToActivate = null;
    boolean oneProfileIsActivated = false;
    if (profiles.isEmpty()) {
      profileToActivate = new RulesProfile("Default " + language.getName(), language.getKey(), true, false);

    } else if (profiles.size() == 1) {
      profileToActivate = profiles.get(0);

    } else {
      Iterator<RulesProfile> iterator = profiles.iterator();
      while (iterator.hasNext() && !oneProfileIsActivated) {
        RulesProfile profile = iterator.next();
        oneProfileIsActivated |= profile.getDefaultProfile();
        if (RulesProfile.SONAR_WAY_NAME.equals(profile.getName())) {
          profileToActivate = profile;
        }
      }
      if (!oneProfileIsActivated) {
        if (profileToActivate == null) {
          profileToActivate = profiles.get(0);
        }
      }
    }
    if (!oneProfileIsActivated && profileToActivate != null) {
      profileToActivate.setDefaultProfile(true);
      session.saveWithoutFlush(profileToActivate);
    }
  }
}
