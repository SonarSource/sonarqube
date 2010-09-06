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
package org.sonar.jpa.dao;

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.profiles.RulesProfile;

import java.util.List;

public class ProfilesDao extends BaseDao {

  public ProfilesDao(DatabaseSession session) {
    super(session);
  }

  public List<RulesProfile> getActiveProfiles() {
    return getSession().getResults(RulesProfile.class, "defaultProfile", true);
  }

  public RulesProfile getActiveProfile(String languageKey, String projectResourceKey) {
    ResourceModel projectResource = getSession().getSingleResult(ResourceModel.class, "key", projectResourceKey, "scope", ResourceModel.SCOPE_PROJECT);
    if (projectResource != null && projectResource.getRulesProfile() != null) {
      return projectResource.getRulesProfile();
    }
    return getSession().getSingleResult(RulesProfile.class, "defaultProfile", true, "language", languageKey);
  }

  public List<RulesProfile> getProfiles(String languageKey) {
    return getSession().getResults(RulesProfile.class, "language", languageKey);
  }

  public List<RulesProfile> getProfiles() {
    return getSession().getResults(RulesProfile.class);
  }

  public List<RulesProfile> getProvidedProfiles() {
    return getSession().getResults(RulesProfile.class, "provided", true);
  }

  public RulesProfile getProfile(String languageKey, String profileName) {
    return getSession().getSingleResult(RulesProfile.class, "language", languageKey, "name", profileName);
  }

  public RulesProfile getProfileById(int profileId) {
    return getSession().getEntityManager().getReference(RulesProfile.class, profileId);
  }

}
