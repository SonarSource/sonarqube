package org.sonar.plugins.core.timemachine;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.batch.index.ViolationPersister;

import java.util.List;

@DependsUpon(DecoratorBarriers.END_OF_VIOLATIONS_GENERATION)
public class ViolationPersisterDecorator implements Decorator {

  private DatabaseSession session;
  private ResourcePersister resourcePersister;
  private ViolationPersister violationPersister;

  public ViolationPersisterDecorator(DatabaseSession session, ResourcePersister resourcePersister, ViolationPersister violationPersister) {
    this.session = session;
    this.resourcePersister = resourcePersister;
    this.violationPersister = violationPersister;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  private RuleFailureModel selectPreviousViolation(Violation violation) {
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

  public void decorate(Resource resource, DecoratorContext context) {
    for (Violation violation : context.getViolations()) {
      RuleFailureModel previousViolation = selectPreviousViolation(violation);
      violationPersister.saveOrUpdateViolation(context.getProject(), violation, previousViolation);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
