/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.log;

import java.util.Collection;
import org.sonar.ce.task.CeTask;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeTaskMessageDto;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link CeTaskMessages} to made available into a task's container.
 * <p>
 * Messages are persisted as the are recorded.
 */
public class CeTaskMessagesImpl implements CeTaskMessages {
  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final CeTask ceTask;

  public CeTaskMessagesImpl(DbClient dbClient, UuidFactory uuidFactory, CeTask ceTask) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.ceTask = ceTask;
  }

  @Override
  public void add(Message message) {
    checkMessage(message);

    try (DbSession dbSession = dbClient.openSession(false)) {
      insert(dbSession, message);
      dbSession.commit();
    }
  }

  @Override
  public void addAll(Collection<Message> messages) {
    if (messages.isEmpty()) {
      return;
    }

    messages.forEach(CeTaskMessagesImpl::checkMessage);

    // TODO: commit every X messages?
    try (DbSession dbSession = dbClient.openSession(true)) {
      messages.forEach(message -> insert(dbSession, message));
      dbSession.commit();
    }
  }

  public void insert(DbSession dbSession, Message message) {
    dbClient.ceTaskMessageDao().insert(dbSession, new CeTaskMessageDto()
    .setUuid(uuidFactory.create())
    .setTaskUuid(ceTask.getUuid())
    .setMessage(message.getText())
    .setCreatedAt(message.getTimestamp()));
  }

  private static void checkMessage(Message message) {
    requireNonNull(message, "message can't be null");
  }
}
