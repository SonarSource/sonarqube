/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.qualityprofile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;

import static org.sonar.server.ws.WsUtils.checkRequest;

public class QProfileTreeImpl implements QProfileTree {

  private final DbClient db;
  private final RuleActivator ruleActivator;
  private final System2 system2;
  private final ActiveRuleIndexer activeRuleIndexer;

  public QProfileTreeImpl(DbClient db, RuleActivator ruleActivator, System2 system2, ActiveRuleIndexer activeRuleIndexer) {
    this.db = db;
    this.ruleActivator = ruleActivator;
    this.system2 = system2;
    this.activeRuleIndexer = activeRuleIndexer;
  }

  @Override
  public List<ActiveRuleChange> removeParentAndCommit(DbSession dbSession, QProfileDto profile) {
    List<ActiveRuleChange> changes = removeParent(dbSession, profile);
    activeRuleIndexer.commitAndIndex(dbSession, changes);
    return changes;
  }

  @Override
  public List<ActiveRuleChange> setParentAndCommit(DbSession dbSession, QProfileDto profile, QProfileDto parentProfile) {
    List<ActiveRuleChange> changes = setParent(dbSession, profile, parentProfile);
    activeRuleIndexer.commitAndIndex(dbSession, changes);
    return changes;
  }

  private List<ActiveRuleChange> setParent(DbSession dbSession, QProfileDto profile, QProfileDto parent) {
    checkRequest(parent.getLanguage().equals(profile.getLanguage()), "Cannot set the profile '%s' as the parent of profile '%s' since their languages differ ('%s' != '%s')",
      parent.getKee(), profile.getKee(), parent.getLanguage(), profile.getLanguage());

    List<ActiveRuleChange> changes = new ArrayList<>();
    if (parent.getKee().equals(profile.getParentKee())) {
      return changes;
    }

    checkRequest(!isDescendant(dbSession, profile, parent), "Descendant profile '%s' can not be selected as parent of '%s'", parent.getKee(), profile.getKee());
    changes.addAll(removeParent(dbSession, profile));

    // set new parent
    profile.setParentKee(parent.getKee());
    db.qualityProfileDao().update(dbSession, profile);

    List<OrgActiveRuleDto> parentActiveRules = db.activeRuleDao().selectByProfile(dbSession, parent);
    Collection<Integer> ruleIds = parentActiveRules.stream().map(ActiveRuleDto::getRuleId).collect(MoreCollectors.toArrayList());
    RuleActivationContext context = ruleActivator.createContextForUserProfile(dbSession, profile, ruleIds);

    for (ActiveRuleDto parentActiveRule : parentActiveRules) {
      try {
        RuleActivation activation = RuleActivation.create(parentActiveRule.getRuleId(), null, null);
        changes.addAll(ruleActivator.activate(dbSession, activation, context));
      } catch (BadRequestException e) {
        // for example because rule status is REMOVED
        // TODO return errors
      }
    }
    return changes;
  }

  private List<ActiveRuleChange> removeParent(DbSession dbSession, QProfileDto profile) {
    List<ActiveRuleChange> changes = new ArrayList<>();
    if (profile.getParentKee() == null) {
      return changes;
    }

    profile.setParentKee(null);
    db.qualityProfileDao().update(dbSession, profile);

    List<OrgActiveRuleDto> activeRules = db.activeRuleDao().selectByProfile(dbSession, profile);
    Collection<Integer> ruleIds = activeRules.stream().map(ActiveRuleDto::getRuleId).collect(MoreCollectors.toArrayList());
    RuleActivationContext context = ruleActivator.createContextForUserProfile(dbSession, profile, ruleIds);

    for (OrgActiveRuleDto activeRule : activeRules) {
      if (ActiveRuleDto.INHERITED.equals(activeRule.getInheritance())) {
        changes.addAll(ruleActivator.deactivate(dbSession, context, activeRule.getRuleId(), true));

      } else if (ActiveRuleDto.OVERRIDES.equals(activeRule.getInheritance())) {
        context.reset(activeRule.getRuleId());
        activeRule.setInheritance(null);
        activeRule.setUpdatedAt(system2.now());
        db.activeRuleDao().update(dbSession, activeRule);
        changes.add(new ActiveRuleChange(ActiveRuleChange.Type.UPDATED, activeRule, context.getRule().get()).setInheritance(null));
      }
    }
    return changes;
  }

  private boolean isDescendant(DbSession dbSession, QProfileDto childProfile, @Nullable QProfileDto parentProfile) {
    QProfileDto currentParent = parentProfile;
    while (currentParent != null) {
      if (childProfile.getName().equals(currentParent.getName())) {
        return true;
      }
      String parentKey = currentParent.getParentKee();
      if (parentKey != null) {
        currentParent = db.qualityProfileDao().selectByUuid(dbSession, parentKey);
      } else {
        currentParent = null;
      }
    }
    return false;
  }
}
