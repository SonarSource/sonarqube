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
package org.sonar.server.tester;

import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.search.ESNode;

import java.sql.Connection;

public class DataStoreCleanup implements ServerComponent {

  private static final Logger LOG = LoggerFactory.getLogger(DataStoreCleanup.class);

  private final ESNode esNode;
  private final MyBatis myBatis;

  public DataStoreCleanup(ESNode esNode, MyBatis myBatis) {
    this.esNode = esNode;
    this.myBatis = myBatis;
  }

  public void clearAll() {
    clearDb();
    clearIndexes();
  }

  public void clearDb() {
    DbSession dbSession = myBatis.openSession(false);
    Connection connection = dbSession.getConnection();
    try {
      LOG.info("Truncate db tables");
      for (String table : DatabaseVersion.TABLES) {
        try {
          connection.createStatement().execute("TRUNCATE TABLE " + table.toLowerCase());
          // commit is useless on some databases
          connection.commit();
        } catch (Exception e) {
          throw new IllegalStateException("Fail to truncate db table " + table, e);
        }
      }

    } finally {
      dbSession.close();
    }
  }

  public void clearIndexes() {
    LOG.info("Truncate es indices");
    Client client = esNode.client();
    client.prepareDeleteByQuery(client.admin().cluster().prepareState().get()
        .getState().getMetaData().concreteAllIndices())
      .setQuery(QueryBuilders.matchAllQuery())
      .get();

  }
}
