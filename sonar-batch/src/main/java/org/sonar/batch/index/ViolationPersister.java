/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

public final class ViolationPersister {

  private DatabaseSession session;
  private ResourcePersister resourcePersister;
  private RuleFinder ruleFinder;

  public ViolationPersister(DatabaseSession session, ResourcePersister resourcePersister, RuleFinder ruleFinder) {
    this.session = session;
    this.resourcePersister = resourcePersister;
    this.ruleFinder = ruleFinder;
  }

  void saveViolation(Project project, Violation violation) {
    saveViolation(project, violation, null, null);
  }

  public void saveViolation(Project project, Violation violation, RuleFailureModel pastViolation, String checksum) {
    Snapshot snapshot = resourcePersister.saveResource(project, violation.getResource());

    RuleFailureModel model = createModel(violation);
    if (pastViolation!=null) {
      model.setCreatedAt(pastViolation.getCreatedAt());
      model.setPermanentId(pastViolation.getPermanentId());

    } else {
      // avoid plugins setting date
      model.setCreatedAt(snapshot.getCreatedAt());
    }
    model.setSnapshotId(snapshot.getId());
    model.setChecksum(checksum);
    session.save(model);

    if (model.getPermanentId()==null) {
      model.setPermanentId(model.getId());
      session.save(model);
    }

    // the following fields can have been changed
    violation.setMessage(model.getMessage());// the message can be changed in the class RuleFailure (truncate + trim)
    violation.setCreatedAt(model.getCreatedAt());
  }
  
  public void commit() {
    session.commit();
  }

  private RuleFailureModel createModel(Violation violation) {
    RuleFailureModel model = new RuleFailureModel();
    Rule rule = ruleFinder.findByKey(violation.getRule().getRepositoryKey(), violation.getRule().getKey());
    model.setRuleId(rule.getId());
    model.setPriority(violation.getSeverity());
    model.setLine(violation.getLineId());
    model.setMessage(violation.getMessage());
    model.setCost(violation.getCost());
    return model;
  }

}
