/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.tests.integration;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.junit.Assert.assertThat;

public class VariationsIT {

  private static final String TIMEMACHINE_PROJECT = "org.sonar.tests:violations-timemachine";

  private static Sonar sonar;

  @BeforeClass
  public static void buildServer() {
    sonar = ITUtils.createSonarWsClient();
  }

  @Test
  public void checkBaseVariations() {

    Resource project = getProject("files", "ncloc", "violations");

    // period 1 : previous analysis
    assertThat(project.getPeriod1Mode(), is("previous_analysis"));
    assertThat(project.getPeriod1Date(), notNullValue());

    // variations from previous analysis
    assertThat(project.getMeasure("files").getVariation1(), is(1.0));
    assertThat(project.getMeasure("ncloc").getVariation1(), is(8.0));
    assertThat(project.getMeasure("violations").getVariation1(), greaterThan(0.0));
  }

  private Resource getProject(String... metricKeys) {
    return sonar.find(ResourceQuery.createForMetrics(TIMEMACHINE_PROJECT, metricKeys).setIncludeTrends(true));
  }

}
