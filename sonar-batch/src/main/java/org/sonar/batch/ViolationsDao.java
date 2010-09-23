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
package org.sonar.batch;

import org.slf4j.LoggerFactory;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.jpa.dao.RulesDao;

import java.util.ArrayList;
import java.util.List;

public class ViolationsDao {

  private RulesProfile profile;
  private DatabaseSession session;
  private RulesDao rulesDao;
  private boolean reuseExistingRulesConfig;

  public ViolationsDao(RulesProfile profile, DatabaseSession session, RulesDao rulesDao, Project project) {
    this.profile = profile;
    this.session = session;
    this.rulesDao = rulesDao;
    this.reuseExistingRulesConfig = project.getReuseExistingRulesConfig();
  }

  public List<Violation> getViolations(Resource resource, Integer snapshotId) {
    List<RuleFailureModel> models = session.getResults(RuleFailureModel.class, "snapshotId", snapshotId);
    List<Violation> violations = new ArrayList<Violation>();
    for (RuleFailureModel model : models) {
      Violation violation = Violation.create(model.getRule(), resource);
      violation.setLineId(model.getLine());
      violation.setMessage(model.getMessage());
      violation.setPriority(model.getPriority());
      violation.setCost(model.getCost());
      violations.add(violation);
    }
    return violations;
  }

  public void saveViolation(Snapshot snapshot, Violation violation) {
    if (profile == null || snapshot == null || violation == null) {
      throw new IllegalArgumentException("Missing data to save violation : profile=" + profile + ",snapshot=" + snapshot + ",violation=" + violation);
    }

    ActiveRule activeRule = profile.getActiveRule(violation.getRule());
    if (activeRule == null) {
      if (reuseExistingRulesConfig) {
        activeRule = new ActiveRule(profile, violation.getRule(), violation.getRule().getPriority());
      } else {
        LoggerFactory.getLogger(getClass()).debug("Violation is not saved because rule is not activated : violation={}", violation);
      }
    } 
    if (activeRule != null) {
      RuleFailureModel model = toModel(snapshot, violation, activeRule);
      session.save(model);
    }
  }

  private RuleFailureModel toModel(Snapshot snapshot, Violation violation, ActiveRule activeRule) {
    Rule rule = reload(violation.getRule());
    if (rule == null) {
      throw new IllegalArgumentException("Rule does not exist : " + violation.getRule());
    }
    RuleFailureModel model = new RuleFailureModel(rule, activeRule.getPriority());
    violation.setPriority(activeRule.getPriority());
    model.setLine(violation.getLineId());
    model.setMessage(violation.getMessage());
    model.setCost(violation.getCost());
    model.setSnapshotId(snapshot.getId());

    return model;
  }

  private Rule reload(Rule rule) {
    return rule.getId() != null ? rule : rulesDao.getRuleByKey(rule.getPluginName(), rule.getKey());
  }
}
