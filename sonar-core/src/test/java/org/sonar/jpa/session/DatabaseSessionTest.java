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
package org.sonar.jpa.session;

import org.hamcrest.Matchers;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.jpa.dao.MeasuresDao;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import javax.persistence.NonUniqueResultException;
import java.sql.Date;
import java.util.List;

import static org.junit.Assert.*;

public class DatabaseSessionTest extends AbstractDbUnitTestCase {
  private static final Long NB_INSERTS = 20000l;

  private ResourceModel project1;
  private ResourceModel project2;

  @Before
  public void setup() {
    project1 = new ResourceModel(ResourceModel.SCOPE_PROJECT, "mygroup:myartifact", "JAV", null, "my name");
    project2 = new ResourceModel(ResourceModel.SCOPE_PROJECT, "mygroup:myartifact1", "JAV", null, "my name 2");
  }

  @Test
  public void performanceTestOnBatchInserts() throws Exception {
    getSession().save(project1);
    Snapshot snapshot = new Snapshot(project1, true, "", new Date(1));
    getSession().save(snapshot);
    getSession().save(CoreMetrics.CLASSES);
    getSession().commit();

    Metric metric = new MeasuresDao(getSession()).getMetric(CoreMetrics.CLASSES_KEY);
    for (int i = 0; i < NB_INSERTS; i++) {
      MeasureModel pm = new MeasureModel(metric.getId(), 1.0).setSnapshotId(snapshot.getId());
      getSession().save(pm);
    }

    getSession().commit();
    assertEquals(NB_INSERTS, getHQLCount(MeasureModel.class));

  }

  @Test
  public void testGetSingleResultWithNoResults() {
    assertNull(getSession().getSingleResult(ResourceModel.class, "name", "test"));
  }

  @Test
  public void testGetSingleResultWithNoCriterias() {
    try {
      assertNull(getSession().getSingleResult(ResourceModel.class, (Object[]) null));
      fail("No IllegalStateException raised");
    } catch (IllegalStateException ex) {
      // error raised correctly
    }
  }

  @Test
  public void testGetSingleResultWithOneResult() {
    getSession().save(project1);
    ResourceModel hit = getSession().getSingleResult(ResourceModel.class, "name", "my name");
    assertNotNull(hit);
    assertEquals(project1, hit);
  }

  @Test
  public void testGetSingleResultWithTwoResults() {
    getSession().save(project1, project2);
    try {
      getSession().getSingleResult(ResourceModel.class, "qualifier", "JAV");
      fail("No NonUniqueResultException raised");
    } catch (NonUniqueResultException ex) {
      // error raised correctly
    }
  }

  @Test
  public void testGetResultsWithNoResults() {
    List<ResourceModel> hits = getSession().getResults(ResourceModel.class, "name", "foo");
    assertTrue(hits.isEmpty());
  }

  @Test
  public void testGetResultsWithMultipleResults() {
    ResourceModel project3 = new ResourceModel(ResourceModel.SCOPE_PROJECT, "mygroup:myartifact3", "BRC", null, "my name 3");
    getSession().save(project1, project2, project3);

    List<ResourceModel> hits = getSession().getResults(ResourceModel.class, "qualifier", "JAV");
    assertFalse(hits.isEmpty());
    assertThat(hits, IsCollectionContaining.hasItems(project1, project2));
    assertThat(hits, Matchers.not(IsCollectionContaining.hasItem(project3)));
  }

}
