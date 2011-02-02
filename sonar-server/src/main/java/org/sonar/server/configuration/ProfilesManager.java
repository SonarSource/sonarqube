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

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.jpa.dao.BaseDao;
import org.sonar.jpa.dao.RulesDao;

import java.util.List;

public class ProfilesManager extends BaseDao {

  private RulesDao rulesDao;

  public ProfilesManager(DatabaseSession session, RulesDao rulesDao) {
    super(session);
    this.rulesDao = rulesDao;
  }

  public void renameProfile(int profileId, String newProfileName) {
    RulesProfile profile = getSession().getSingleResult(RulesProfile.class, "id", profileId);
    if (profile != null && !profile.getProvided()) {
      String hql = "UPDATE " + RulesProfile.class.getSimpleName() + " o SET o.parentName=:newName  WHERE o.parentName=:oldName";
      getSession().getEntityManager().createQuery(hql)
          .setParameter("oldName", profile.getName())
          .setParameter("newName", newProfileName)
          .executeUpdate();
      profile.setName(newProfileName);
      getSession().save(profile);
      getSession().commit();
    }
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
    if (profile != null && !profile.getProvided() && getChildren(profile).isEmpty()) {
      String hql = "UPDATE " + ResourceModel.class.getSimpleName() + " o SET o.rulesProfile=null WHERE o.rulesProfile=:rulesProfile";
      getSession().createQuery(hql).setParameter("rulesProfile", profile).executeUpdate();
      getSession().remove(profile);
      getSession().commit();
    }
  }

  public void deleteAllProfiles() {
    String hql = "UPDATE " + ResourceModel.class.getSimpleName() + " o SET o.rulesProfile = null WHERE o.rulesProfile IS NOT NULL";
    getSession().createQuery(hql).executeUpdate();
    List profiles = getSession().createQuery("FROM " + RulesProfile.class.getSimpleName()).getResultList();
    for (Object profile : profiles) {
      getSession().removeWithoutFlush(profile);
    }
    getSession().commit();
  }

  // Managing inheritance of profiles

  public ValidationMessages changeParentProfile(Integer profileId, String parentName) {
    ValidationMessages messages = ValidationMessages.create();
    RulesProfile profile = getSession().getEntity(RulesProfile.class, profileId);
    if (profile != null && !profile.getProvided()) {
      RulesProfile oldParent = getParentProfile(profile);
      RulesProfile newParent = getProfile(profile.getLanguage(), parentName);
      if (isCycle(profile, newParent)) {
        messages.addWarningText("Please do not select a child profile as parent.");
        return messages;
      }
      // Deactivate all inherited rules
      if (oldParent != null) {
        for (ActiveRule activeRule : oldParent.getActiveRules()) {
          deactivate(profile, activeRule.getRule());
        }
      }
      // Activate all inherited rules
      if (newParent != null) {
        for (ActiveRule activeRule : newParent.getActiveRules()) {
          activateOrChange(profile, activeRule);
        }
      }
      profile.setParentName(newParent == null ? null : newParent.getName());
      getSession().saveWithoutFlush(profile);
      getSession().commit();
    }
    return messages;
  }

  /**
   * Rule was activated/changed in parent profile.
   */
  public void activatedOrChanged(int parentProfileId, int activeRuleId) {
    ActiveRule parentActiveRule = getSession().getEntity(ActiveRule.class, activeRuleId);
    if (parentActiveRule.isInherited()) {
      parentActiveRule.setInheritance(ActiveRule.OVERRIDES);
      getSession().saveWithoutFlush(parentActiveRule);
    }
    for (RulesProfile child : getChildren(parentProfileId)) {
      activateOrChange(child, parentActiveRule);
    }
    getSession().commit();
  }

  /**
   * Rule was deactivated in parent profile.
   */
  public void deactivated(int parentProfileId, int ruleId) {
    Rule rule = getSession().getEntity(Rule.class, ruleId);
    for (RulesProfile child : getChildren(parentProfileId)) {
      deactivate(child, rule);
    }
    getSession().commit();
  }

  /**
   * @return true, if setting <code>childProfile</code> as a child of <code>parentProfile</code> adds cycle
   */
  boolean isCycle(RulesProfile childProfile, RulesProfile parentProfile) {
    while (parentProfile != null) {
      if (childProfile.equals(parentProfile)) {
        return true;
      }
      parentProfile = getParentProfile(parentProfile);
    }
    return false;
  }

  public void revert(int profileId, int activeRuleId) {
    RulesProfile profile = getSession().getEntity(RulesProfile.class, profileId);
    ActiveRule activeRule = getSession().getEntity(ActiveRule.class, activeRuleId);
    if (activeRule != null && activeRule.doesOverride()) {
      ActiveRule parentActiveRule = getParentProfile(profile).getActiveRule(activeRule.getRule());
      removeActiveRule(profile, activeRule);
      activeRule = (ActiveRule) parentActiveRule.clone();
      activeRule.setRulesProfile(profile);
      activeRule.setInheritance(ActiveRule.INHERITED);
      profile.getActiveRules().add(activeRule);
      getSession().saveWithoutFlush(activeRule);

      for (RulesProfile child : getChildren(profile)) {
        activateOrChange(child, activeRule);
      }

      getSession().commit();
    }
  }

  private void activateOrChange(RulesProfile profile, ActiveRule parentActiveRule) {
    ActiveRule activeRule = profile.getActiveRule(parentActiveRule.getRule());
    if (activeRule != null) {
      if (activeRule.isInherited()) {
        removeActiveRule(profile, activeRule);
      } else {
        activeRule.setInheritance(ActiveRule.OVERRIDES);
        getSession().saveWithoutFlush(activeRule);
        return; // no need to change in children
      }
    }
    activeRule = (ActiveRule) parentActiveRule.clone();
    activeRule.setRulesProfile(profile);
    activeRule.setInheritance(ActiveRule.INHERITED);
    profile.getActiveRules().add(activeRule);
    getSession().saveWithoutFlush(activeRule);

    for (RulesProfile child : getChildren(profile)) {
      activateOrChange(child, activeRule);
    }
  }

  private void deactivate(RulesProfile profile, Rule rule) {
    ActiveRule activeRule = profile.getActiveRule(rule);
    if (activeRule != null) {
      if (activeRule.isInherited()) {
        removeActiveRule(profile, activeRule);
      } else {
        activeRule.setInheritance(null);
        getSession().saveWithoutFlush(activeRule);
        return; // no need to change in children
      }

      for (RulesProfile child : getChildren(profile)) {
        deactivate(child, rule);
      }
    }
  }

  private List<RulesProfile> getChildren(int parentId) {
    RulesProfile parent = getSession().getEntity(RulesProfile.class, parentId);
    return getChildren(parent);
  }

  private List<RulesProfile> getChildren(RulesProfile parent) {
    return getSession().getResults(RulesProfile.class,
        "language", parent.getLanguage(),
        "parentName", parent.getName(),
        "provided", false,
        "enabled", true);
  }

  private void removeActiveRule(RulesProfile profile, ActiveRule activeRule) {
    profile.getActiveRules().remove(activeRule);
    getSession().removeWithoutFlush(activeRule);
  }

  RulesProfile getProfile(String language, String name) {
    return getSession().getSingleResult(RulesProfile.class,
        "language", language,
        "name", name,
        "enabled", true);
  }

  RulesProfile getParentProfile(RulesProfile profile) {
    if (profile.getParentName() == null) {
      return null;
    }
    return getProfile(profile.getLanguage(), profile.getParentName());
  }

}
