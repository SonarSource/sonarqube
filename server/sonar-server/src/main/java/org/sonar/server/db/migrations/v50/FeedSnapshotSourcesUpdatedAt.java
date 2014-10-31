/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonar.server.db.migrations.v50;

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.migration.v50.Migration50Mapper;
import org.sonar.core.persistence.migration.v50.SnapshotSource;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.db.migrations.MassUpdate;

import java.util.Timer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Used in the Active Record Migration 705
 *
 * @since 5.0
 */
public class FeedSnapshotSourcesUpdatedAt implements DatabaseMigration {

  private final DbClient db;
  private final AtomicLong counter = new AtomicLong(0L);
  private final MassUpdate.ProgressTask progressTask = new MassUpdate.ProgressTask(counter);

  public FeedSnapshotSourcesUpdatedAt(DbClient db) {
    this.db = db;
  }

  @Override
  public void execute() {
    Timer timer = new Timer("Db Migration Progress");
    timer.schedule(progressTask, MassUpdate.ProgressTask.PERIOD_MS, MassUpdate.ProgressTask.PERIOD_MS);

    final DbSession readSession = db.openSession(false);
    final DbSession writeSession = db.openSession(true);
    try {
      readSession.select("org.sonar.core.persistence.migration.v50.Migration50Mapper.selectSnapshotSources", new ResultHandler() {
        @Override
        public void handleResult(ResultContext context) {
          SnapshotSource snapshotSource = (SnapshotSource) context.getResultObject();
          snapshotSource.setUpdatedAt(snapshotSource.getSnapshotBuildDate());
          writeSession.getMapper(Migration50Mapper.class).updateSnapshotSource(snapshotSource);
          counter.getAndIncrement();
        }
      });
      writeSession.commit();
      readSession.commit();

      // log the total number of process rows
      progressTask.log();
    } finally {
      readSession.close();
      writeSession.close();
      timer.cancel();
      timer.purge();
    }
  }
}
