/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import com.google.common.collect.Sets;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Language;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.jpa.session.DatabaseSessionFactory;

import javax.persistence.Query;
import java.util.Set;

/**
 * @since 2.6
 */
public final class EnableProfiles {

  private Language[] languages;
  private DatabaseSessionFactory sessionFactory;


  public EnableProfiles(Language[] languages, DatabaseSessionFactory sessionFactory, RegisterProvidedProfiles registerProfilesBefore) {// NOSONAR the parameter registerProfilesBefore is used to define the execution order of startup components
    this.languages = languages;
    this.sessionFactory = sessionFactory;
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler().start("Enable profiles");
    DatabaseSession session = sessionFactory.getSession();
    Set<String> languages = getLanguageKeys();

    enableProfilesOnKnownLanguages(languages, session);
    disableProfilesOnMissingLanguages(languages, session);
    
    session.commit();
    profiler.stop();
  }

  private void enableProfilesOnKnownLanguages(Set<String> languages, DatabaseSession session) {
    Query query = session.createQuery("update " + RulesProfile.class.getSimpleName() + " set enabled=:enabled where language in (:languages)");
    query.setParameter("enabled", Boolean.TRUE);
    query.setParameter("languages", languages);
    query.executeUpdate();
  }

  private void disableProfilesOnMissingLanguages(Set<String> languages, DatabaseSession session) {
    Query query = session.createQuery("update " + RulesProfile.class.getSimpleName() + " set enabled=:enabled where language not in (:languages)");
    query.setParameter("enabled", Boolean.FALSE);
    query.setParameter("languages", languages);
    query.executeUpdate();
  }

  private Set<String> getLanguageKeys() {
    Set<String> keys = Sets.newLinkedHashSet();
    for (Language language : languages) {
      keys.add(language.getKey());
    }
    return keys;
  }
}
