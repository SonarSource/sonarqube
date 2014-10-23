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
package org.sonar.server.db.migrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.persistence.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

public class MassUpdate {

  public static interface Handler {
    /**
     * Convert some column values of a given row.
     *
     * @return true if the row must be updated, else false. If false, then the update parameter must not be touched.
     */
    boolean handle(Select.Row row, SqlStatement update) throws SQLException;
  }

  private final Database db;
  private final Connection readConnection, writeConnection;
  private final AtomicLong counter = new AtomicLong(0L);
  private final ProgressTask progressTask = new ProgressTask(counter);

  private Select select;
  private Upsert update;

  MassUpdate(Database db, Connection readConnection, Connection writeConnection) {
    this.db = db;
    this.readConnection = readConnection;
    this.writeConnection = writeConnection;
  }

  public SqlStatement select(String sql) throws SQLException {
    this.select = SelectImpl.create(db, readConnection, sql);
    return this.select;
  }

  public MassUpdate update(String sql) throws SQLException {
    this.update = UpsertImpl.create(writeConnection, sql);
    return this;
  }

  public MassUpdate rowPluralName(String s) {
    this.progressTask.setRowPluralName(s);
    return this;
  }

  public void execute(final Handler handler) throws SQLException {
    if (select == null || update == null) {
      throw new IllegalStateException("SELECT or UPDATE requests are not defined");
    }

    Timer timer = new Timer("Db Migration Progress");
    timer.schedule(progressTask, ProgressTask.PERIOD_MS, ProgressTask.PERIOD_MS);
    try {
      select.scroll(new Select.RowHandler() {
        @Override
        public void handle(Select.Row row) throws SQLException {
          if (handler.handle(row, update)) {
            update.addBatch();
          }
          counter.getAndIncrement();
        }
      });
      if (((UpsertImpl) update).getBatchCount() > 0L) {
        update.execute().commit();
      }
      update.close();

      // log the total number of process rows
      progressTask.log();
    } finally {
      timer.cancel();
      timer.purge();
    }
  }

  public static class ProgressTask extends TimerTask {
    private static final Logger LOGGER = LoggerFactory.getLogger("DbMigration");
    public static final long PERIOD_MS = 60000L;
    private final AtomicLong counter;
    private String rowName = "rows";

    public ProgressTask(AtomicLong counter) {
      this.counter = counter;
    }

    void setRowPluralName(String s) {
      this.rowName = s;
    }

    @Override
    public void run() {
      log();
    }

    public void log() {
      LOGGER.info(String.format("%d %s processed", counter.get(), rowName));
    }
  }

}
