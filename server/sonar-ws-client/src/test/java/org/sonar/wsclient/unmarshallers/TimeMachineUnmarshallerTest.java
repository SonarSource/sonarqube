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
package org.sonar.wsclient.unmarshallers;

import org.junit.Test;
import org.sonar.wsclient.services.TimeMachine;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeMachineUnmarshallerTest extends UnmarshallerTestCase {

  @Test
  public void test_to_model() throws Exception {
    TimeMachine timeMachine = new TimeMachineUnmarshaller().toModel(loadFile("/timemachine/timemachine.json"));

    // columns
    assertThat(timeMachine.getColumns().length).isEqualTo(2);
    assertThat(timeMachine.getColumns()[0].getIndex()).isEqualTo(0);
    assertThat(timeMachine.getColumns()[0].getMetricKey()).isEqualTo("ncloc");
    assertThat(timeMachine.getColumns()[1].getIndex()).isEqualTo(1);
    assertThat(timeMachine.getColumns()[1].getMetricKey()).isEqualTo("coverage");

    // values sorted by date
    assertThat(timeMachine.getCells().length).isEqualTo(3); // 3 days
    assertThat(getDayOfMonth(timeMachine.getCells()[0].getDate())).isEqualTo(19);
    assertThat(getDayOfMonth(timeMachine.getCells()[1].getDate())).isEqualTo(21);
    assertThat(getDayOfMonth(timeMachine.getCells()[2].getDate())).isEqualTo(25);

    assertThat(timeMachine.getCells()[0].getValues()).hasSize(2);
    assertThat((Double) timeMachine.getCells()[0].getValues()[0]).isEqualTo(21.0);
    assertThat((Double) timeMachine.getCells()[0].getValues()[1]).isEqualTo(80.0);
  }

  @Test
  public void should_accept_null_values() throws Exception {
    TimeMachine timeMachine = new TimeMachineUnmarshaller().toModel(loadFile("/timemachine/null-values.json"));

    assertThat(timeMachine.getCells()[0].getValues().length).isEqualTo(2);
    assertThat(timeMachine.getCells()[0].getValues()[0]).isNull();
    assertThat((Double) timeMachine.getCells()[0].getValues()[1]).isEqualTo(80.0);

    assertThat((Double) timeMachine.getCells()[1].getValues()[0]).isEqualTo(29.0);
    assertThat(timeMachine.getCells()[1].getValues()[1]).isNull();
  }

  @Test
  public void should_cast_values() throws Exception {
    TimeMachine timeMachine = new TimeMachineUnmarshaller().toModel(loadFile("/timemachine/typed-values.json"));

    assertThat(timeMachine.getCells()[0].getValues().length).isEqualTo(2);
    assertThat((String) timeMachine.getCells()[0].getValues()[0]).isEqualTo("Sonar way");
    assertThat((Double) timeMachine.getCells()[0].getValues()[1]).isEqualTo(80.0);
  }

  private int getDayOfMonth(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    return calendar.get(Calendar.DAY_OF_MONTH);
  }
}
