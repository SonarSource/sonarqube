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
package org.sonar.server.configuration;

import com.google.common.collect.Lists;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.*;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.preview.PreviewCache;
import org.sonar.jpa.dao.BaseDao;
import org.sonar.jpa.dao.RulesDao;
import org.sonar.server.qualityprofile.RuleInheritanceActions;

import java.util.List;

public class ProfilesManager extends BaseDao {

  private RulesDao rulesDao;
  private PreviewCache dryRunCache;

  public ProfilesManager(DatabaseSession session, RulesDao rulesDao, PreviewCache dryRunCache) {
    super(session);
    this.rulesDao = rulesDao;
    this.dryRunCache = dryRunCache;
  }

  public void copyProfile(int profileId, String newProfileName) {
    RulesProfile profile = getSession().getSingleResult(RulesProfile.class, "id", profileId);
    RulesProfile toImport = (RulesProfile) profile.clone();
    toImport.setName(newProfileName);
    ProfilesBackup pb = new ProfilesBackup(getSession(), dryRunCache);
    pb.importProfile(rulesDao, toImport);
    getSession().commit();
    dryRunCache.reportGlobalModification();
  }

  public void deleteAllProfiles() {
    // Remove history of rule changes
    String hqlDeleteRc = "DELETE " + ActiveRuleChange.class.getSimpleName() + " rc";
    getSession().createQuery(hqlDeleteRc).executeUpdate();

    List profiles = getSession().createQuery("FROM " + RulesProfile.class.getSimpleName()).getResultList();
    for (Object profile : profiles) {
      getSession().removeWithoutFlush(profile);
    }
    getSession().commit();
    dryRunCache.reportGlobalModification();
  }

  // Managing inheritance of profiles

  public ValidationMessages changeParentProfile(Integer profileId, String parentName, String userName) {
    ValidationMessages messages = ValidationMessages.create();
    RulesProfile profile = getSession().getEntity(RulesProfile.class, profileId);
    if (profile != null) {
      RulesProfile oldParent = getParentProfile(profile);
      RulesProfile newParent = getProfile(profile.getLanguage(), parentName);
      if (isCycle(profile, newParent)) {
        messages.addWarningText("Please do not select a child profile as parent.");
        return messages;
      }
      // Deactivate all inherited rules
      if (oldParent != null) {
        for (ActiveRule activeRule : oldParent.getActiveRules()) {
          deactivate(profile, activeRule.getRule(), userName);
        }
      }
      // Activate all inherited rules
      if (newParent != null) {
        for (ActiveRule activeRule : newParent.getActiveRules()) {
          activateOrChange(profile, activeRule, userName);
        }
      }
      profile.setParentName(newParent == null ? null : newParent.getName());
      getSession().saveWithoutFlush(profile);
      getSession().commit();
      dryRunCache.reportGlobalModification();
    }
    return messages;
  }

  /**
   * Deactivate all active rules from profiles using a rule, then remove then.
   */
  public void removeActivatedRules(int ruleId) {
    List<ActiveRule> activeRules = getSession().createQuery("FROM " + ActiveRule.class.getSimpleName() + " WHERE rule.id=:ruleId").setParameter("ruleId", ruleId).getResultList();
    List<ActiveRule> activeRulesToRemove = Lists.newArrayList();

    for (ActiveRule activeRule : activeRules) {
      if (!activeRule.isInherited()) {
        RulesProfile profile = activeRule.getRulesProfile();
        incrementProfileVersionIfNeeded(profile);
        ruleDisabled(profile, activeRule, null);
        for (RulesProfile child : getChildren(profile.getId())) {
          deactivate(child, activeRule.getRule(), null);
        }
        activeRulesToRemove.add(activeRule);
      }
    }

    for (ActiveRule activeRule : activeRulesToRemove) {
      // Do not use getSingleResult as it can generate an EntityNotFoundException
      ActiveRule activeRuleToRemove = getSession().getEntity(ActiveRule.class, activeRule.getId());
      removeActiveRule(activeRuleToRemove);
    }
    getSession().commit();
    dryRunCache.reportGlobalModification();
  }


  /**
   * Rule was activated
   */
  public RuleInheritanceActions activated(int profileId, int activeRuleId, String userName) {
    ActiveRule activeRule = getSession().getEntity(ActiveRule.class, activeRuleId);
    RulesProfile profile = getSession().getEntity(RulesProfile.class, profileId);
    ruleEnabled(profile, activeRule, userName);
    // Notify child profiles
    return activatedOrChanged(profileId, activeRuleId, userName);
  }

  /**
   * Rule param was changed
   */
  public RuleInheritanceActions ruleParamChanged(int profileId, int activeRuleId, String paramKey, String oldValue, String newValue, String userName) {
    ActiveRule activeRule = getSession().getEntity(ActiveRule.class, activeRuleId);
    RulesProfile profile = getSession().getEntity(RulesProfile.class, profileId);

    ruleParamChanged(profile, activeRule.getRule(), paramKey, oldValue, newValue, userName);

    // Notify child profiles
    return activatedOrChanged(profileId, activeRuleId, userName);
  }

  /**
   * Rule severity was changed
   */
  public RuleInheritanceActions ruleSeverityChanged(int profileId, int activeRuleId, RulePriority oldSeverity, RulePriority newSeverity, String userName) {
    ActiveRule activeRule = getSession().getEntity(ActiveRule.class, activeRuleId);
    RulesProfile profile = getSession().getEntity(RulesProfile.class, profileId);

    ruleSeverityChanged(profile, activeRule.getRule(), oldSeverity, newSeverity, userName);

    // Notify child profiles
    return activatedOrChanged(profileId, activeRuleId, userName);
  }

  /**
   * Rule was activated/changed in parent profile.
   */
  private RuleInheritanceActions activatedOrChanged(int parentProfileId, int activeRuleId, String userName) {
    RuleInheritanceActions actions = new RuleInheritanceActions();
    ActiveRule parentActiveRule = getSession().getEntity(ActiveRule.class, activeRuleId);
    if (parentActiveRule.isInherited()) {
      parentActiveRule.setInheritance(ActiveRule.OVERRIDES);
      getSession().saveWithoutFlush(parentActiveRule);
    }
    actions.addToIndex(activeRuleId);
    for (RulesProfile child : getChildren(parentProfileId)) {
      actions.add(activateOrChange(child, parentActiveRule, userName));
    }
    getSession().commit();
    dryRunCache.reportGlobalModification();
    return actions;
  }

  /**
   * Rule was deactivated in parent profile.
   */
  public RuleInheritanceActions deactivated(int parentProfileId, int deactivatedRuleId, String userName) {
    RuleInheritanceActions actions = new RuleInheritanceActions();
    ActiveRule parentActiveRule = getSession().getEntity(ActiveRule.class, deactivatedRuleId);
    RulesProfile profile = getSession().getEntity(RulesProfile.class, parentProfileId);
    ruleDisabled(profile, parentActiveRule, userName);
    actions.addToIndex(parentActiveRule.getId());
    for (RulesProfile child : getChildren(parentProfileId)) {
      actions.add(deactivate(child, parentActiveRule.getRule(), userName));
    }
    getSession().commit();
    dryRunCache.reportGlobalModification();
    return actions;
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

  public void revert(int profileId, int activeRuleId, String userName) {
    RulesProfile profile = getSession().getEntity(RulesProfile.class, profileId);
    ActiveRule oldActiveRule = getSession().getEntity(ActiveRule.class, activeRuleId);
    if (oldActiveRule != null && oldActiveRule.doesOverride()) {
      ActiveRule parentActiveRule = getParentProfile(profile).getActiveRule(oldActiveRule.getRule());
      removeActiveRule(oldActiveRule);
      ActiveRule newActiveRule = (ActiveRule) parentActiveRule.clone();
      newActiveRule.setRulesProfile(profile);
      newActiveRule.setInheritance(ActiveRule.INHERITED);
      profile.addActiveRule(newActiveRule);
      getSession().saveWithoutFlush(newActiveRule);

      // Compute change
      ruleChanged(profile, oldActiveRule, newActiveRule, userName);

      for (RulesProfile child : getChildren(profile)) {
        activateOrChange(child, newActiveRule, userName);
      }

      getSession().commit();
      dryRunCache.reportGlobalModification();
    }
  }

  private synchronized void incrementProfileVersionIfNeeded(RulesProfile profile) {
    if (profile.getUsed()) {
      profile.setVersion(profile.getVersion() + 1);
      profile.setUsed(false);
      getSession().saveWithoutFlush(profile);
    }
  }

  /**
   * Deal with creation of ActiveRuleChange item when a rule param is changed on a profile
   */
  private void ruleParamChanged(RulesProfile profile, Rule rule, String paramKey, String oldValue, String newValue, String userName) {
    incrementProfileVersionIfNeeded(profile);
    ActiveRuleChange rc = new ActiveRuleChange(userName, profile, rule);
    if (!StringUtils.equals(oldValue, newValue)) {
      rc.setParameterChange(paramKey, oldValue, newValue);
      getSession().saveWithoutFlush(rc);
    }
  }

  /**
   * Deal with creation of ActiveRuleChange item when a rule severity is changed on a profile
   */
  private void ruleSeverityChanged(RulesProfile profile, Rule rule, RulePriority oldSeverity, RulePriority newSeverity, String userName) {
    incrementProfileVersionIfNeeded(profile);
    ActiveRuleChange rc = new ActiveRuleChange(userName, profile, rule);
    if (!ObjectUtils.equals(oldSeverity, newSeverity)) {
      rc.setOldSeverity(oldSeverity);
      rc.setNewSeverity(newSeverity);
      getSession().saveWithoutFlush(rc);
    }
  }

  /**
   * Deal with creation of ActiveRuleChange item when a rule is changed (severity and/or param(s)) on a profile
   */
  private void ruleChanged(RulesProfile profile, ActiveRule oldActiveRule, ActiveRule newActiveRule, String userName) {
    incrementProfileVersionIfNeeded(profile);
    ActiveRuleChange rc = new ActiveRuleChange(userName, profile, newActiveRule.getRule());

    if (oldActiveRule.getSeverity() != newActiveRule.getSeverity()) {
      rc.setOldSeverity(oldActiveRule.getSeverity());
      rc.setNewSeverity(newActiveRule.getSeverity());
    }
    if (oldActiveRule.getRule().getParams() != null) {
      for (RuleParam p : oldActiveRule.getRule().getParams()) {
        String oldParam = oldActiveRule.getParameter(p.getKey());
        String newParam = newActiveRule.getParameter(p.getKey());
        if (!StringUtils.equals(oldParam, newParam)) {
          rc.setParameterChange(p.getKey(), oldParam, newParam);
        }
      }
    }

    getSession().saveWithoutFlush(rc);
  }

  /**
   * Deal with creation of ActiveRuleChange item when a rule is enabled on a profile
   */
  private void ruleEnabled(RulesProfile profile, ActiveRule newActiveRule, String userName) {
    incrementProfileVersionIfNeeded(profile);
    ActiveRuleChange rc = new ActiveRuleChange(userName, profile, newActiveRule.getRule());
    rc.setEnabled(true);
    rc.setNewSeverity(newActiveRule.getSeverity());
    if (newActiveRule.getRule().getParams() != null) {
      for (RuleParam p : newActiveRule.getRule().getParams()) {
        String newParam = newActiveRule.getParameter(p.getKey());
        if (newParam != null) {
          rc.setParameterChange(p.getKey(), null, newParam);
        }
      }
    }
    getSession().saveWithoutFlush(rc);
  }

  /**
   * Deal with creation of ActiveRuleChange item when a rule is disabled on a profile
   */
  private void ruleDisabled(RulesProfile profile, ActiveRule disabledRule, String userName) {
    incrementProfileVersionIfNeeded(profile);
    ActiveRuleChange rc = new ActiveRuleChange(userName, profile, disabledRule.getRule());
    rc.setEnabled(false);
    rc.setOldSeverity(disabledRule.getSeverity());
    if (disabledRule.getRule().getParams() != null) {
      for (RuleParam p : disabledRule.getRule().getParams()) {
        String oldParam = disabledRule.getParameter(p.getKey());
        if (oldParam != null) {
          rc.setParameterChange(p.getKey(), oldParam, null);
        }
      }
    }
    getSession().saveWithoutFlush(rc);
  }

  private RuleInheritanceActions activateOrChange(RulesProfile profile, ActiveRule parentActiveRule, String userName) {
    ActiveRule oldActiveRule = profile.getActiveRule(parentActiveRule.getRule());
    RuleInheritanceActions actions = new RuleInheritanceActions();
    if (oldActiveRule != null) {
      if (oldActiveRule.isInherited()) {
        removeActiveRule(oldActiveRule);
        actions.addToDelete(oldActiveRule.getId());
      } else {
        oldActiveRule.setInheritance(ActiveRule.OVERRIDES);
        getSession().saveWithoutFlush(oldActiveRule);
        // no need to change in children
        actions.addToIndex(oldActiveRule.getId());
        return actions;
      }
    }
    ActiveRule newActiveRule = (ActiveRule) parentActiveRule.clone();
    newActiveRule.setRulesProfile(profile);
    newActiveRule.setInheritance(ActiveRule.INHERITED);
    profile.addActiveRule(newActiveRule);
    getSession().saveWithoutFlush(newActiveRule);

    actions.addToIndex(newActiveRule.getId());

    if (oldActiveRule != null) {
      ruleChanged(profile, oldActiveRule, newActiveRule, userName);
    } else {
      ruleEnabled(profile, newActiveRule, userName);
    }

    for (RulesProfile child : getChildren(profile)) {
      actions.add(activateOrChange(child, newActiveRule, userName));
    }

    return actions;
  }

  private RuleInheritanceActions deactivate(RulesProfile profile, Rule rule, String userName) {
    RuleInheritanceActions actions = new RuleInheritanceActions();
    ActiveRule activeRule = profile.getActiveRule(rule);
    if (activeRule != null) {
      if (activeRule.isInherited()) {
        ruleDisabled(profile, activeRule, userName);
        actions.addToDelete(activeRule.getId());
        removeActiveRule(activeRule);
      } else {
        activeRule.setInheritance(null);
        getSession().saveWithoutFlush(activeRule);
        actions.addToIndex(activeRule.getId());
        // no need to change in children
        return actions;
      }

      for (RulesProfile child : getChildren(profile)) {
        actions.add(deactivate(child, rule, userName));
      }
    }
    return actions;
  }

  private List<RulesProfile> getChildren(int parentId) {
    RulesProfile parent = getSession().getEntity(RulesProfile.class, parentId);
    return getChildren(parent);
  }

  private List<RulesProfile> getChildren(RulesProfile parent) {
    return getSession().getResults(RulesProfile.class,
      "language", parent.getLanguage(),
      "parentName", parent.getName());
  }

  private void removeActiveRule(ActiveRule activeRule) {
    org.sonar.api.profiles.RulesProfile profile = activeRule.getRulesProfile();
    profile.removeActiveRule(activeRule);
    getSession().removeWithoutFlush(activeRule);
  }

  RulesProfile getProfile(String language, String name) {
    return getSession().getSingleResult(RulesProfile.class,
      "language", language,
      "name", name);
  }

  RulesProfile getParentProfile(RulesProfile profile) {
    if (profile.getParentName() == null) {
      return null;
    }
    return getProfile(profile.getLanguage(), profile.getParentName());
  }

}
