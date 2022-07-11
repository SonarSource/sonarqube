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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Collection;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.pushevent.PushEvent;
import org.sonar.ce.task.projectanalysis.pushevent.PushEventRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.pushevent.PushEventDto;

import static java.nio.charset.StandardCharsets.UTF_8;

public class PersistPushEventsStep implements ComputationStep {
  private static final int MAX_BATCH_SIZE = 250;
  private static final Logger LOGGER = Loggers.get(PersistPushEventsStep.class);
  private static final Gson GSON = new GsonBuilder().create();

  private final DbClient dbClient;
  private final PushEventRepository pushEventRepository;
  private final TreeRootHolder treeRootHolder;

  public PersistPushEventsStep(DbClient dbClient, PushEventRepository pushEventRepository,
    TreeRootHolder treeRootHolder) {
    this.dbClient = dbClient;
    this.pushEventRepository = pushEventRepository;
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public void execute(Context context) {
    Collection<PushEvent<?>> issues = pushEventRepository.getEvents();
    if (issues.isEmpty()) {
      return;
    }

    try (DbSession dbSession = dbClient.openSession(true)) {
      int batchCounter = 0;
      for (PushEvent<?> event : issues) {
        pushEvent(dbSession, event);
        batchCounter++;
        batchCounter = flushIfNeeded(dbSession, batchCounter);
      }
      flushSession(dbSession);

    } catch (Exception ex) {
      LOGGER.warn("Error during publishing push event", ex);
    }
  }

  private void pushEvent(DbSession dbSession, PushEvent<?> event) {
    PushEventDto eventDto = new PushEventDto()
      .setProjectUuid(treeRootHolder.getRoot().getUuid())
      .setPayload(serializeIssueToPushEvent(event));
    dbClient.pushEventDao().insert(dbSession, eventDto);
  }

  private static byte[] serializeIssueToPushEvent(PushEvent<?> event) {
    return GSON.toJson(event.getData()).getBytes(UTF_8);
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
