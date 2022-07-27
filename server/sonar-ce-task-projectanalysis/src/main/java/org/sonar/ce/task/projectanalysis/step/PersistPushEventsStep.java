/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
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
  private static final Logger LOGGER = Loggers.get(PersistPushEventsStep.class);

  private final DbClient dbClient;
  private final ProtoIssueCache protoIssueCache;
  private final PushEventFactory pushEventFactory;

  public PersistPushEventsStep(DbClient dbClient,
    ProtoIssueCache protoIssueCache,
    PushEventFactory pushEventFactory) {
    this.dbClient = dbClient;
    this.protoIssueCache = protoIssueCache;
    this.pushEventFactory = pushEventFactory;
  }

  @Override
  public void execute(Context context) {
    try (DbSession dbSession = dbClient.openSession(true);
      CloseableIterator<DefaultIssue> issues = protoIssueCache.traverse()) {
      int batchCounter = 0;

      while (issues.hasNext()) {
        DefaultIssue currentIssue = issues.next();
        Optional<PushEventDto> raisedEvent = pushEventFactory.raiseEventOnIssue(currentIssue);

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
    return "Publishing taint vulnerabilities events";
  }

}
