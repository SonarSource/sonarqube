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
package org.sonar.jpa.dao;

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.profiles.RulesProfile;

public class ProfilesDao extends BaseDao {

  public ProfilesDao(DatabaseSession session) {
    super(session);
  }

  public RulesProfile getActiveProfile(String languageKey, String projectResourceKey) {
    ResourceModel projectResource = getSession().getSingleResult(ResourceModel.class, "key", projectResourceKey, "scope", ResourceModel.SCOPE_PROJECT);
    if (projectResource != null && projectResource.getRulesProfile() != null && projectResource.getRulesProfile().isEnabled()) {
      return projectResource.getRulesProfile();
    }
    return getSession().getSingleResult(RulesProfile.class, "defaultProfile", true, "language", languageKey, "enabled", true);
  }

  public RulesProfile getProfile(String languageKey, String profileName) {
    return getSession().getSingleResult(RulesProfile.class, "language", languageKey, "name", profileName, "enabled", true);
  }

}
