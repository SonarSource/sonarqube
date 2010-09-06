package org.sonar.core.purge;

import org.junit.Test;
import org.sonar.api.batch.PurgeContext;
import org.sonar.api.database.DatabaseSession;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: SimonBrandhof
 * Date: Jul 20, 2010
 * Time: 10:47:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class AbstractPurgeTest extends AbstractDbUnitTestCase {

  @Test
  public void purgeSnapshots() throws SQLException {
    setupData("purgeSnapshots");

    final FakePurge purge = new FakePurge(getSession());
    purge.purge(null);

    checkTables("purgeSnapshots", "snapshots", "project_measures", "measure_data", "rule_failures", "snapshot_sources", "dependencies");
  }
}

class FakePurge extends AbstractPurge {
  public FakePurge(DatabaseSession session) {
    super(session);
  }

  public void purge(PurgeContext context) {
    deleteSnapshotData(Arrays.asList(3, 4));
  }
}