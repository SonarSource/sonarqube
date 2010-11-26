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
package org.sonar.batch.index;

import com.google.common.collect.Maps;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.SonarException;

import java.util.Map;

public final class ViolationPersister {

  private DatabaseSession session;
  private ResourcePersister resourcePersister;
  private Map<Rule, Integer> ruleIds = Maps.newHashMap();

  public ViolationPersister(DatabaseSession session, ResourcePersister resourcePersister) {
    this.session = session;
    this.resourcePersister = resourcePersister;
  }

  public void saveViolation(Project project, Violation violation) {
    Snapshot snapshot = resourcePersister.saveResource(project, violation.getResource());
    if (snapshot != null) {
      session.save(toModel(snapshot, violation));
    }
  }

  private RuleFailureModel toModel(Snapshot snapshot, Violation violation) {
    RuleFailureModel model = new RuleFailureModel();
    model.setRuleId(getRuleId(violation.getRule()));
    model.setPriority(violation.getPriority());
    model.setLine(violation.getLineId());
    model.setMessage(violation.getMessage());
    model.setCost(violation.getCost());
    model.setSnapshotId(snapshot.getId());
    return model;
  }

  private Integer getRuleId(Rule rule) {
    Integer ruleId = ruleIds.get(rule);
    if (ruleId == null) {
      Rule persistedRule = session.getSingleResult(Rule.class, "pluginName", rule.getRepositoryKey(), "key", rule.getKey(), "enabled", true);
      if (persistedRule == null) {
        throw new SonarException("Rule not found: " + rule);
      }
      ruleId = persistedRule.getId();
      ruleIds.put(rule, ruleId);

    }
    return ruleId;
  }

  public void clear() {
    ruleIds.clear();
  }
}
