/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.timemachine;

import org.sonar.api.batch.*;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;
import org.sonar.api.violations.ViolationQuery;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.core.DryRunIncompatible;

import java.util.List;

@DryRunIncompatible
@DependsUpon({ DecoratorBarriers.END_OF_VIOLATION_TRACKING, DecoratorBarriers.START_VIOLATION_PERSISTENCE })
@DependedUpon(DecoratorBarriers.END_OF_VIOLATION_PERSISTENCE)
public class ViolationPersisterDecorator implements Decorator {

  private ViolationTrackingDecorator tracker;
  private ResourcePersister persister;
  private RuleFinder ruleFinder;
  private DatabaseSession session;

  public ViolationPersisterDecorator(ViolationTrackingDecorator tracker, ResourcePersister persister, RuleFinder ruleFinder, DatabaseSession session) {
    this.tracker = tracker;
    this.persister = persister;
    this.ruleFinder = ruleFinder;
    this.session = session;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    saveViolations(context.getProject(), context.getViolations(ViolationQuery.create().forResource(resource).setSwitchMode(ViolationQuery.SwitchMode.BOTH)));
  }

  void saveViolations(Project project, List<Violation> violations) {
    for (Violation violation : violations) {
      RuleFailureModel referenceViolation = tracker.getReferenceViolation(violation);
      save(project, violation, referenceViolation);
    }
    session.commit();
  }

  public void save(Project project, Violation violation, RuleFailureModel referenceViolation) {
    Snapshot snapshot = persister.saveResource(project, violation.getResource());

    RuleFailureModel model = createModel(violation);
    if (referenceViolation != null) {
      model.setPermanentId(referenceViolation.getPermanentId());
    }
    model.setSnapshotId(snapshot.getId());
    session.saveWithoutFlush(model);

    if (model.getPermanentId() == null) {
      model.setPermanentId(model.getId());
      session.saveWithoutFlush(model);
    }
    violation.setMessage(model.getMessage());// the message can be changed in the class RuleFailure (truncate + trim)
  }


  private RuleFailureModel createModel(Violation violation) {
    RuleFailureModel model = new RuleFailureModel();
    Rule rule = ruleFinder.findByKey(violation.getRule().getRepositoryKey(), violation.getRule().getKey());
    model.setRuleId(rule.getId());
    model.setPriority(violation.getSeverity());
    model.setLine(violation.getLineId());
    model.setMessage(violation.getMessage());
    model.setCost(violation.getCost());
    model.setChecksum(violation.getChecksum());
    model.setCreatedAt(violation.getCreatedAt());
    model.setSwitchedOff(violation.isSwitchedOff());
    model.setPersonId(violation.getPersonId());
    return model;
  }
}
