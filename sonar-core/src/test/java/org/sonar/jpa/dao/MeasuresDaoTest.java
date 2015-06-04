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

import java.util.Arrays;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.measures.Metric;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MeasuresDaoTest extends AbstractDbUnitTestCase {

  private MeasuresDao service;
  private ResourceModel project;

  @Before
  public void before() {
    service = new MeasuresDao(getSession());
    project = new ResourceModel(ResourceModel.SCOPE_PROJECT, "foo:bar", ResourceModel.QUALIFIER_PROJECT_TRUNK, null, "Foo");
    project.setName("project name");
    getSession().save(project);
  }

  @Test
  public void shouldRegisterMetrics() {
    Collection<Metric> newMetrics = createMetrics();
    service.registerMetrics(newMetrics);

    Collection<Metric> metrics = service.getEnabledMetrics();
    assertThat(metrics.size(), is(newMetrics.size()));
  }

  private Collection<Metric> createMetrics() {
    Metric m1 = new Metric("metric1");
    m1.setEnabled(false);

    Metric m2 = new Metric("metric2");
    m2.setEnabled(true);

    Metric m3 = new Metric("metric3");
    m3.setEnabled(false);

    Metric m4 = new Metric("metric4");
    m4.setEnabled(true);

    Metric m5 = new Metric("metric5");
    m5.setEnabled(true);

    return Arrays.asList(m1, m2, m3, m4, m5);
  }
}
