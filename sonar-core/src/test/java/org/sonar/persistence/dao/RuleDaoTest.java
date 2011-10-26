/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.persistence.dao;

import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.SqlSession;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.IDatabaseTester;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.hamcrest.core.Is;
import org.junit.*;
import org.junit.Rule;
import org.sonar.persistence.DerbyUtils;
import org.sonar.persistence.InMemoryDatabase;
import org.sonar.persistence.MyBatis;
import org.sonar.persistence.model.*;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertThat;

public class RuleDaoTest {

  protected static IDatabaseTester databaseTester;
  private static InMemoryDatabase database;
  private static RuleDao dao;

  @BeforeClass
  public static void startDatabase() throws Exception {
    database = new InMemoryDatabase();
    MyBatis myBatis = new MyBatis(database);

    database.start();
    myBatis.start();

    dao = new RuleDao(myBatis);
    databaseTester = new DataSourceDatabaseTester(database.getDataSource());
  }

  @AfterClass
  public static void stopDatabase() throws Exception {
    if (databaseTester != null) {
      databaseTester.onTearDown();
    }
    database.stop();
  }

  @Test
  public void testSelectAll() throws Exception {
    setupData("selectAll");
    List<org.sonar.persistence.model.Rule> rules = dao.selectAll();

    assertThat(rules.size(), Is.is(1));
    org.sonar.persistence.model.Rule rule = rules.get(0);
    assertThat(rule.getId(), Is.is(1L));
    assertThat(rule.getName(), Is.is("Avoid Null"));
    assertThat(rule.getDescription(), Is.is("Should avoid NULL"));
    assertThat(rule.isEnabled(), Is.is(true));
    assertThat(rule.getRepositoryKey(), Is.is("checkstyle"));
  }

  @Test
  public void testSelectById() throws Exception {
    setupData("selectById");
    org.sonar.persistence.model.Rule rule = dao.selectById(2L);

    assertThat(rule.getId(), Is.is(2L));
    assertThat(rule.getName(), Is.is("Avoid Null"));
    assertThat(rule.getDescription(), Is.is("Should avoid NULL"));
    assertThat(rule.isEnabled(), Is.is(true));
    assertThat(rule.getRepositoryKey(), Is.is("checkstyle"));
  }

  protected final void setupData(String... testNames) throws Exception {
    InputStream[] streams = new InputStream[testNames.length];
    try {
      for (int i = 0; i < testNames.length; i++) {
        String className = getClass().getName();
        className = String.format("/%s/%s.xml", className.replace(".", "/"), testNames[i]);
        streams[i] = getClass().getResourceAsStream(className);
        if (streams[i] == null) {
          throw new RuntimeException("Test not found :" + className);
        }
      }

      setupData(streams);

    } finally {
      for (InputStream stream : streams) {
        IOUtils.closeQuietly(stream);
      }
    }
  }

  protected final void setupData(InputStream... dataSetStream) throws Exception {
    IDataSet[] dataSets = new IDataSet[dataSetStream.length];
    for (int i = 0; i < dataSetStream.length; i++) {
      ReplacementDataSet dataSet = new ReplacementDataSet(new FlatXmlDataSet(dataSetStream[i]));
      dataSet.addReplacementObject("[null]", null);
      dataSets[i] = dataSet;
    }
    CompositeDataSet compositeDataSet = new CompositeDataSet(dataSets);

    databaseTester.setDataSet(compositeDataSet);
    IDatabaseConnection connection = databaseTester.getConnection();
    DatabaseOperation.CLEAN_INSERT.execute(connection, databaseTester.getDataSet());

    connection.getConnection().commit();
    connection.close();

  }

}
