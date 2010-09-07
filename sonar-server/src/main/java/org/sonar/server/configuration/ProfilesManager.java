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
package org.sonar.server.configuration;

import org.sonar.api.Plugins;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.DefaultRulesManager;
import org.sonar.jpa.dao.BaseDao;
import org.sonar.jpa.dao.RulesDao;

public class ProfilesManager extends BaseDao {

  private DefaultRulesManager rulesManager;
  private RulesDao rulesDao;
  private Plugins plugins;

  public ProfilesManager(DatabaseSession session, RulesDao rulesDao, Plugins plugins, DefaultRulesManager rulesManager) {
    super(session);
    this.rulesManager = rulesManager;
    this.rulesDao = rulesDao;
    this.plugins = plugins;
  }

  public void copyProfile(int profileId, String newProfileName) {
    RulesProfile profile = getSession().getSingleResult(RulesProfile.class, "id", profileId);
    RulesProfile toImport = (RulesProfile) profile.clone();
    toImport.setName(newProfileName);
    toImport.setDefaultProfile(false);
    toImport.setProvided(false);
    ProfilesBackup pb = new ProfilesBackup(getSession());
    pb.importProfile(rulesDao, toImport);
    getSession().commit();
  }

  public void deleteProfile(int profileId) {
    RulesProfile profile = getSession().getEntity(RulesProfile.class, profileId);
    if (profile != null && !profile.getProvided()) {
      String hql = "UPDATE " + ResourceModel.class.getSimpleName() + " o SET o.rulesProfile=null WHERE o.rulesProfile=:rulesProfile";
      getSession().createQuery(hql).setParameter("rulesProfile", profile).executeUpdate();
      getSession().remove(profile);
      getSession().commit();
    }
  }

  public void deleteAllProfiles() {
    getSession().createQuery("UPDATE " + ResourceModel.class.getSimpleName() + " o SET o.rulesProfile = null WHERE o.rulesProfile IS NOT NULL").executeUpdate();
    getSession().createQuery("DELETE " + RulesProfile.class.getSimpleName() + " o").executeUpdate();
    getSession().commit();
  }

}
