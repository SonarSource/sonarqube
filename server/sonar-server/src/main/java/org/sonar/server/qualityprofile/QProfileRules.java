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

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.rule.index.RuleQuery;

/**
 * Operations related to activation and deactivation of rules on user profiles.
 * Use {@link BuiltInQProfileUpdate} for built-in profiles.
 */
@ServerSide
public interface QProfileRules {

  /**
   * Activate multiple rules at once on a Quality profile.
   * Db session is committed and Elasticsearch indices are updated.
   * If an activation fails to be executed, then all others are
   * canceled, db session is not committed and an exception is
   * thrown.
   */
  List<ActiveRuleChange> activateAndCommit(DbSession dbSession, QProfileDto profile, Collection<RuleActivation> activations);

  /**
   * Same as {@link #activateAndCommit(DbSession, QProfileDto, Collection)} except
   * that:
   * - rules are loaded from search engine
   * - rules are activated with default parameters
   * - an activation failure does not break others. No exception is thrown.
   */
  BulkChangeResult bulkActivateAndCommit(DbSession dbSession, QProfileDto profile, RuleQuery ruleQuery, @Nullable String severity);

  List<ActiveRuleChange> deactivateAndCommit(DbSession dbSession, QProfileDto profile, Collection<Integer> ruleIds);

  BulkChangeResult bulkDeactivateAndCommit(DbSession dbSession, QProfileDto profile, RuleQuery ruleQuery);

  /**
   * Delete a rule from all Quality profiles. Db session is not committed. As a
   * consequence Elasticsearch indices are NOT updated.
   */
  List<ActiveRuleChange> deleteRule(DbSession dbSession, RuleDefinitionDto rule);
}
