/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.ce.task.projectanalysis.step;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.issue.ProtoIssueCache;
import org.sonar.ce.task.projectanalysis.pushevent.PushEventFactory;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.pushevent.PushEventDto;

public class PersistPushEventsStep implements ComputationStep {
  private static final int MAX_BATCH_SIZE = 250;
  private static final Logger LOGGER = LoggerFactory.getLogger(PersistPushEventsStep.class);

  private final DbClient dbClient;
  private final ProtoIssueCache protoIssueCache;
  private final PushEventFactory pushEventFactory;
  private final TreeRootHolder treeRootHolder;

  public PersistPushEventsStep(DbClient dbClient,
    ProtoIssueCache protoIssueCache,
    PushEventFactory pushEventFactory,
    TreeRootHolder treeRootHolder) {
    this.dbClient = dbClient;
    this.protoIssueCache = protoIssueCache;
    this.pushEventFactory = pushEventFactory;
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public void execute(Context context) {
    try (DbSession dbSession = dbClient.openSession(true);
      CloseableIterator<DefaultIssue> issues = protoIssueCache.traverse()) {
      int batchCounter = 0;
      var projectUuid = getProjectUuid(dbSession);

      while (issues.hasNext()) {
        DefaultIssue currentIssue = issues.next();
        Optional<PushEventDto> raisedEvent = pushEventFactory.raiseEventOnIssue(projectUuid, currentIssue);

        if (raisedEvent.isEmpty()) {
          continue;
        }

        dbClient.pushEventDao().insert(dbSession, raisedEvent.get());
        batchCounter++;
        batchCounter = flushIfNeeded(dbSession, batchCounter);
      }
      flushSession(dbSession);

    } catch (Exception ex) {
      LOGGER.warn("Error during publishing push event", ex);
    }
  }

  private String getProjectUuid(DbSession dbSession) {
    var branch = dbClient.branchDao().selectByUuid(dbSession, treeRootHolder.getRoot().getUuid());
    if (branch.isEmpty()) {
      return treeRootHolder.getRoot().getUuid();
    }
    return branch.get().getProjectUuid();
  }

  private static int flushIfNeeded(DbSession dbSession, int batchCounter) {
    if (batchCounter > MAX_BATCH_SIZE) {
      flushSession(dbSession);
      batchCounter = 0;
    }
    return batchCounter;
  }

  private static void flushSession(DbSession dbSession) {
    dbSession.flushStatements();
    dbSession.commit();
  }

  @Override
  public String getDescription() {
    return "Publishing taint vulnerabilities and security hotspots events";
  }

}
