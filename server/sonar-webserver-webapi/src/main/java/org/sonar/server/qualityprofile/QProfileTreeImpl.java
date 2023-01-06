/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.sonar.server.pushapi.qualityprofile.QualityProfileChangeEventService;
import org.sonar.server.qualityprofile.builtin.RuleActivationContext;
import org.sonar.server.qualityprofile.builtin.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;

import static org.sonar.server.exceptions.BadRequestException.checkRequest;

public class QProfileTreeImpl implements QProfileTree {

  private final DbClient db;
  private final RuleActivator ruleActivator;
  private final System2 system2;
  private final ActiveRuleIndexer activeRuleIndexer;
  private final QualityProfileChangeEventService qualityProfileChangeEventService;

  public QProfileTreeImpl(DbClient db, RuleActivator ruleActivator, System2 system2, ActiveRuleIndexer activeRuleIndexer,
    QualityProfileChangeEventService qualityProfileChangeEventService) {
    this.db = db;
    this.ruleActivator = ruleActivator;
    this.system2 = system2;
    this.activeRuleIndexer = activeRuleIndexer;
    this.qualityProfileChangeEventService = qualityProfileChangeEventService;
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

    // set new parent
    profile.setParentKee(parent.getKee());
    db.qualityProfileDao().update(dbSession, profile);

    List<OrgActiveRuleDto> activeRules = db.activeRuleDao().selectByProfile(dbSession, profile);
    List<OrgActiveRuleDto> parentActiveRules = db.activeRuleDao().selectByProfile(dbSession, parent);

    changes = getChangesFromRulesToBeRemoved(dbSession, profile, getRulesDifference(activeRules, parentActiveRules));

    Collection<String> parentRuleUuids = parentActiveRules.stream().map(ActiveRuleDto::getRuleUuid).collect(MoreCollectors.toArrayList());
    RuleActivationContext context = ruleActivator.createContextForUserProfile(dbSession, profile, parentRuleUuids);

    for (ActiveRuleDto parentActiveRule : parentActiveRules) {
      try {
        RuleActivation activation = RuleActivation.create(parentActiveRule.getRuleUuid(), null, null);
        changes.addAll(ruleActivator.activate(dbSession, activation, context));
      } catch (BadRequestException e) {
        // for example because rule status is REMOVED
        // TODO return errors
      }
    }
    qualityProfileChangeEventService.distributeRuleChangeEvent(List.of(profile), changes, profile.getLanguage());
    return changes;
  }

  private static List<OrgActiveRuleDto> getRulesDifference(Collection<OrgActiveRuleDto> rulesCollection1, Collection<OrgActiveRuleDto> rulesCollection2) {
    Collection<String> rulesCollection2Uuids = rulesCollection2.stream()
      .map(ActiveRuleDto::getRuleUuid)
      .collect(MoreCollectors.toArrayList());

    return rulesCollection1.stream()
      .filter(rule -> !rulesCollection2Uuids.contains(rule.getRuleUuid()))
      .toList();
  }

  private List<ActiveRuleChange> removeParent(DbSession dbSession, QProfileDto profile) {
    List<ActiveRuleChange> changes = new ArrayList<>();
    if (profile.getParentKee() == null) {
      return changes;
    }

    profile.setParentKee(null);
    db.qualityProfileDao().update(dbSession, profile);

    List<OrgActiveRuleDto> activeRules = db.activeRuleDao().selectByProfile(dbSession, profile);
    changes = getChangesFromRulesToBeRemoved(dbSession, profile, activeRules);

    qualityProfileChangeEventService.distributeRuleChangeEvent(List.of(profile), changes, profile.getLanguage());
    return changes;
  }

  private List<ActiveRuleChange> getChangesFromRulesToBeRemoved(DbSession dbSession, QProfileDto profile, List<OrgActiveRuleDto> rules) {
    List<ActiveRuleChange> changes = new ArrayList<>();

    Collection<String> ruleUuids = rules.stream().map(ActiveRuleDto::getRuleUuid).collect(MoreCollectors.toArrayList());
    RuleActivationContext context = ruleActivator.createContextForUserProfile(dbSession, profile, ruleUuids);

    for (OrgActiveRuleDto activeRule : rules) {
      if (ActiveRuleDto.INHERITED.equals(activeRule.getInheritance())) {
        changes.addAll(ruleActivator.deactivate(dbSession, context, activeRule.getRuleUuid(), true));

      } else if (ActiveRuleDto.OVERRIDES.equals(activeRule.getInheritance())) {
        context.reset(activeRule.getRuleUuid());
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
