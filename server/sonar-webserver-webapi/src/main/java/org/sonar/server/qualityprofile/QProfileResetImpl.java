/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

@ServerSide
public class QProfileResetImpl implements QProfileReset {

  private final DbClient db;
  private final RuleActivator activator;
  private final ActiveRuleIndexer activeRuleIndexer;

  public QProfileResetImpl(DbClient db, RuleActivator activator, ActiveRuleIndexer activeRuleIndexer) {
    this.db = db;
    this.activator = activator;
    this.activeRuleIndexer = activeRuleIndexer;
  }

  @Override
  public BulkChangeResult reset(DbSession dbSession, QProfileDto profile, Collection<RuleActivation> activations) {
    requireNonNull(profile.getId(), "Quality profile must be persisted");
    checkArgument(!profile.isBuiltIn(), "Operation forbidden for built-in Quality Profile '%s'", profile.getKee());

    BulkChangeResult result = new BulkChangeResult();
    Set<Integer> rulesToBeDeactivated = new HashSet<>();
    // Keep reference to all the activated rules before backup restore
    for (ActiveRuleDto activeRuleDto : db.activeRuleDao().selectByProfile(dbSession, profile)) {
      if (activeRuleDto.getInheritance() == null) {
        // inherited rules can't be deactivated
        rulesToBeDeactivated.add(activeRuleDto.getRuleId());
      }
    }
    Set<Integer> ruleIds = new HashSet<>(rulesToBeDeactivated.size() + activations.size());
    ruleIds.addAll(rulesToBeDeactivated);
    activations.forEach(a -> ruleIds.add(a.getRuleId()));
    RuleActivationContext context = activator.createContextForUserProfile(dbSession, profile, ruleIds);

    for (RuleActivation activation : activations) {
      try {
        List<ActiveRuleChange> changes = activator.activate(dbSession, activation, context);
        rulesToBeDeactivated.remove(activation.getRuleId());
        result.incrementSucceeded();
        result.addChanges(changes);
      } catch (BadRequestException e) {
        result.incrementFailed();
        result.getErrors().addAll(e.errors());
      }
    }

    List<ActiveRuleChange> changes = new ArrayList<>(result.getChanges());
    for (Integer ruleId : rulesToBeDeactivated) {
      try {
        changes.addAll(activator.deactivate(dbSession, context, ruleId, false));
      } catch (BadRequestException e) {
        // ignore, probably a rule inherited from parent that can't be deactivated
      }
    }
    activeRuleIndexer.commitAndIndex(dbSession, changes);
    return result;
  }

}
