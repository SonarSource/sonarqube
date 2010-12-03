package org.sonar.plugins.core.timemachine;

import org.sonar.api.BatchExtension;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.database.model.SnapshotSource;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.index.ResourcePersister;

import java.util.Collections;
import java.util.List;

public class PastViolationsLoader implements BatchExtension {

  private DatabaseSession session;
  private ResourcePersister resourcePersister;

  public PastViolationsLoader(DatabaseSession session, ResourcePersister resourcePersister) {
    this.session = session;
    this.resourcePersister = resourcePersister;
  }

  public List<RuleFailureModel> getPastViolations(Resource resource) {
    if (resource == null) {
      return Collections.emptyList();
    }

    Snapshot snapshot = resourcePersister.getSnapshot(resource);
    if (snapshot == null) {
      throw new SonarException("This resource has no snapshot ???" + resource);
    }
    Snapshot previousLastSnapshot = resourcePersister.getLastSnapshot(snapshot, true);
    if (previousLastSnapshot == null) {
      return Collections.emptyList();
    }
    return session.getResults(RuleFailureModel.class,
        "snapshotId", previousLastSnapshot.getId());
  }

  public SnapshotSource getSource(Resource resource) {
    Snapshot snapshot = resourcePersister.getSnapshot(resource);
    return session.getSingleResult(SnapshotSource.class,
        "snapshotId", snapshot.getId());
  }

}
