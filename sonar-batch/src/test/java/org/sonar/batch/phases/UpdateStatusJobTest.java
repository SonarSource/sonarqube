/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.phases;

import org.junit.Test;
import org.slf4j.Logger;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.security.ResourcePermissions;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.index.DefaultResourcePersister;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.batch.index.SnapshotCache;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import javax.persistence.Query;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class UpdateStatusJobTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldUnflagPenultimateLastSnapshot() {
    assertAnalysis(11, "shouldUnflagPenultimateLastSnapshot");
  }

  @Test
  public void doNotFailIfNoPenultimateLast() {
    assertAnalysis(5, "doNotFailIfNoPenultimateLast");
  }

  @Test
  public void shouldNotEnableSnapshotWhenNotLatest() {
    assertAnalysis(6, "shouldNotEnableSnapshotWhenNotLatest");
  }

  private void assertAnalysis(int snapshotId, String fixture) {
    setupData("sharedFixture", fixture);

    DatabaseSession session = getSession();
    UpdateStatusJob job = new UpdateStatusJob(new Settings().appendProperty(CoreProperties.SERVER_BASE_URL, "http://myserver/"), mock(ServerClient.class), session,
      new DefaultResourcePersister(session, mock(ResourcePermissions.class), mock(SnapshotCache.class), mock(ResourceCache.class)),
      mock(Project.class), loadSnapshot(snapshotId));
    job.execute();

    checkTables(fixture, "snapshots");
  }

  private Snapshot loadSnapshot(int id) {
    Query query = getSession().createQuery("SELECT s FROM Snapshot s WHERE s.id=:id");
    query.setParameter("id", id);
    return (Snapshot) query.getSingleResult();
  }

  @Test
  public void should_log_successful_analysis() throws Exception {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.SERVER_BASE_URL, "http://myserver/");
    Project project = new Project("struts");
    UpdateStatusJob job = new UpdateStatusJob(settings, mock(ServerClient.class), mock(DatabaseSession.class),
      mock(ResourcePersister.class), project, mock(Snapshot.class));

    Logger logger = mock(Logger.class);
    job.logSuccess(logger);

    verify(logger).info("ANALYSIS SUCCESSFUL, you can browse {}", "http://myserver/dashboard/index/struts");
  }

  @Test
  public void should_log_successful_dry_run_analysis() throws Exception {
    Settings settings = new Settings();
    settings.setProperty("sonar.dryRun", true);
    Project project = new Project("struts");
    UpdateStatusJob job = new UpdateStatusJob(settings, mock(ServerClient.class), mock(DatabaseSession.class),
      mock(ResourcePersister.class), project, mock(Snapshot.class));

    Logger logger = mock(Logger.class);
    job.logSuccess(logger);

    verify(logger).info("ANALYSIS SUCCESSFUL");
  }
}
