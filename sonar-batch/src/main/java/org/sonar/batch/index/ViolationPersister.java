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

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;

import java.util.List;

public final class ViolationPersister {

  private DatabaseSession session;
  private ResourcePersister resourcePersister;
  private RuleFinder ruleFinder;

  public ViolationPersister(DatabaseSession session, ResourcePersister resourcePersister, RuleFinder ruleFinder) {
    this.session = session;
    this.resourcePersister = resourcePersister;
    this.ruleFinder = ruleFinder;
  }

  public void saveViolation(Project project, Violation violation) {
    saveOrUpdateViolation(project, violation);
  }

  public RuleFailureModel selectPreviousViolation(Violation violation) {
    Snapshot snapshot = resourcePersister.getSnapshot(violation.getResource());
    Snapshot previousLastSnapshot = resourcePersister.getLastSnapshot(snapshot, true);
    if (previousLastSnapshot == null) {
      return null;
    }
    // Can be several violations on line with same message: for example - "'3' is a magic number"
    List<RuleFailureModel> models = session.getResults(RuleFailureModel.class,
        "snapshotId", previousLastSnapshot.getId(),
        "line", violation.getLineId(),
        "message", violation.getMessage());
    if (models != null && !models.isEmpty()) {
      return models.get(0);
    }
    return null;
  }

  public void saveOrUpdateViolation(Project project, Violation violation) {
    Snapshot snapshot = resourcePersister.saveResource(project, violation.getResource());
    RuleFailureModel model = selectPreviousViolation(violation);
    if (model != null) {
      // update
      model = mergeModel(violation, model);
    } else {
      // insert
      model = createModel(violation);
      model.setCreatedAt(snapshot.getCreatedAt());
    }
    model.setSnapshotId(snapshot.getId());
    session.save(model);
    violation.setCreatedAt(model.getCreatedAt());
  }

  private RuleFailureModel createModel(Violation violation) {
    return mergeModel(violation, new RuleFailureModel());
  }

  private RuleFailureModel mergeModel(Violation violation, RuleFailureModel merge) {
    Rule rule = ruleFinder.findByKey(violation.getRule().getRepositoryKey(), violation.getRule().getKey());
    merge.setRuleId(rule.getId());
    merge.setPriority(violation.getPriority());
    merge.setLine(violation.getLineId());
    merge.setMessage(violation.getMessage());
    merge.setCost(violation.getCost());
    return merge;
  }
}
