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
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;

import java.util.Collections;
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

  public List<RuleFailureModel> getPreviousViolations(Resource resource) {
    Snapshot snapshot = resourcePersister.getSnapshot(resource);
    Snapshot previousLastSnapshot = resourcePersister.getLastSnapshot(snapshot, true);
    if (previousLastSnapshot == null) {
      return Collections.emptyList();
    }
    return session.getResults(RuleFailureModel.class, "snapshotId", previousLastSnapshot.getId());
  }

  public void saveOrUpdateViolation(Project project, Violation violation, RuleFailureModel oldModel) {
    Snapshot snapshot = resourcePersister.saveResource(project, violation.getResource());
    if (snapshot == null) {
      return; // TODO Godin why ? when?
    }
    RuleFailureModel model;
    if (oldModel != null) {
      // update
      model = session.reattach(RuleFailureModel.class, oldModel.getId());
      model = mergeModel(violation, oldModel);
    } else {
      // insert
      model = createModel(violation);
    }
    model.setSnapshotId(snapshot.getId());
    session.save(model);
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
