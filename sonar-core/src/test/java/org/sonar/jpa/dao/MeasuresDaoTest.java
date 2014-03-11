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
package org.sonar.jpa.dao;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.measures.Metric;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class MeasuresDaoTest extends AbstractDbUnitTestCase {

  private MeasuresDao service;
  private ResourceModel project;

  @Before
  public void before() throws Exception {
    service = new MeasuresDao(getSession());
    project = new ResourceModel(ResourceModel.SCOPE_PROJECT, "foo:bar", ResourceModel.QUALIFIER_PROJECT_TRUNK, null, "Foo");
    project.setName("project name");
    getSession().save(project);
  }

  @Test
  public void shouldReturnUserDefinedMetrics() {
    for (Metric metric : createMetrics()) {
      getSession().save(metric);
    }

    Collection<Metric> metrics = service.getUserDefinedMetrics();
    assertThat(metrics.size(), is(2));
    for (Metric metric : metrics) {
      assertThat(metric.getOrigin(), not(Metric.Origin.JAV));
    }
  }

  @Test
  public void shouldRegisterMetrics() {
    Collection<Metric> newMetrics = createMetrics();
    service.registerMetrics(newMetrics);

    Collection<Metric> metrics = service.getEnabledMetrics();
    assertThat(metrics.size(), is(newMetrics.size()));
  }

  @Test
  public void shouldDisabledMetrics() {
    Collection<Metric> newMetrics = createMetrics();

    service.disabledMetrics(newMetrics);

    Collection<Metric> allMetrics = service.getMetrics();
    assertThat(allMetrics.size(), is(newMetrics.size()));

    Collection<Metric> disabledMetrics = service.getEnabledMetrics();
    assertThat(disabledMetrics.size(), is(0));
  }


  private Collection<Metric> createMetrics() {
    Metric m1 = new Metric("metric1");
    m1.setEnabled(false);
    m1.setOrigin(Metric.Origin.JAV);

    Metric m2 = new Metric("metric2");
    m2.setEnabled(true);
    m2.setOrigin(Metric.Origin.JAV);

    Metric m3 = new Metric("metric3");
    m3.setEnabled(false);
    m3.setOrigin(Metric.Origin.GUI);

    Metric m4 = new Metric("metric4");
    m4.setEnabled(true);
    m4.setOrigin(Metric.Origin.GUI);

    Metric m5 = new Metric("metric5");
    m5.setEnabled(true);
    m5.setOrigin(Metric.Origin.WS);

    return Arrays.asList(m1, m2, m3, m4, m5);
  }
}
