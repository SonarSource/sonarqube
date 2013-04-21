/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.wsclient.unmarshallers;

import org.junit.Test;
import org.sonar.wsclient.services.ManualMeasure;

import java.util.List;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ManualMeasureUnmarshallerTest extends UnmarshallerTestCase {

  @Test
  public void testSingleMeasure() {
    ManualMeasure measure = new ManualMeasureUnmarshaller().toModel("[]");
    assertThat(measure, nullValue());

    measure = new ManualMeasureUnmarshaller().toModel(loadFile("/manual_measures/single_measure.json"));
    assertThat(measure.getId(), is(1L));
    assertThat(measure.getMetricKey(), is("burned_budget"));
    assertThat(measure.getResourceKey(), is("org.apache.struts:struts-parent"));
    assertThat(measure.getValue(), is(302.5));
    assertThat(measure.getUserLogin(), is("admin"));
    assertThat(measure.getUsername(), is("Administrator"));
    assertThat(measure.getCreatedAt().getDate(), is(27));
    assertThat(measure.getUpdatedAt().getDate(), is(3));
  }


  @Test
  public void testAllMeasures() {
    List<ManualMeasure> measures = new ManualMeasureUnmarshaller().toModels(loadFile("/manual_measures/all_measures.json"));
    assertThat(measures.size(), is(2));
  }
}
