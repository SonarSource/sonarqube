/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.wsclient.unmarshallers;

import org.junit.Test;
import org.sonar.wsclient.services.TimeMachine;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TimeMachineUnmarshallerTest extends UnmarshallerTestCase {

  @Test
  public void testToModel() throws Exception {
    TimeMachine timeMachine = new TimeMachineUnmarshaller().toModel(loadFile("/timemachine/timemachine.json"));

    // columns
    assertThat(timeMachine.getColumns().length, is(2));
    assertThat(timeMachine.getColumns()[0].getIndex(), is(0));
    assertThat(timeMachine.getColumns()[0].getMetricKey(), is("ncloc"));
    assertThat(timeMachine.getColumns()[1].getIndex(), is(1));
    assertThat(timeMachine.getColumns()[1].getMetricKey(), is("coverage"));

    // values sorted by date
    assertThat(timeMachine.getCells().length, is(3)); // 3 days
    assertThat(timeMachine.getCells()[0].getDate().getDate(), is(19));
    assertThat(timeMachine.getCells()[1].getDate().getDate(), is(21));
    assertThat(timeMachine.getCells()[2].getDate().getDate(), is(25));

    assertThat(timeMachine.getCells()[0].getValues().length, is(2));
    assertThat((Double) timeMachine.getCells()[0].getValues()[0], is(21.0));
    assertThat((Double) timeMachine.getCells()[0].getValues()[1], is(80.0));
  }

  @Test
  public void shouldAcceptNullValues() throws Exception {
    TimeMachine timeMachine = new TimeMachineUnmarshaller().toModel(loadFile("/timemachine/null-values.json"));

    assertThat(timeMachine.getCells()[0].getValues().length, is(2));
    assertThat(timeMachine.getCells()[0].getValues()[0], nullValue());
    assertThat((Double) timeMachine.getCells()[0].getValues()[1], is(80.0));

    assertThat((Double) timeMachine.getCells()[1].getValues()[0], is(29.0));
    assertThat(timeMachine.getCells()[1].getValues()[1], nullValue());
  }

  @Test
  public void shouldCastValues() throws Exception {
    TimeMachine timeMachine = new TimeMachineUnmarshaller().toModel(loadFile("/timemachine/typed-values.json"));

    assertThat(timeMachine.getCells()[0].getValues().length, is(2));
    assertThat((String) timeMachine.getCells()[0].getValues()[0], is("Sonar way"));
    assertThat((Double) timeMachine.getCells()[0].getValues()[1], is(80.0));
  }
}
