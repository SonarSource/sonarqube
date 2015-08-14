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
package org.sonar.server.computation.analysis;

import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportAnalysisMetadataHolderTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Date someDate = new Date();

  @Test
  public void getAnalysisDate_throws_ISE_when_not_holder_is_not_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Analysis date has not been set");

    new ReportAnalysisMetadataHolder().getAnalysisDate();
  }

  @Test
  public void getAnalysisDate_returns_date_with_same_time_as_the_one_set_with_setAnalysisDate() throws InterruptedException {
    ReportAnalysisMetadataHolder underTest = new ReportAnalysisMetadataHolder();

    underTest.setAnalysisDate(someDate);

    Thread.sleep(10);

    Date analysisDate = underTest.getAnalysisDate();
    assertThat(analysisDate.getTime()).isEqualTo(someDate.getTime());
    assertThat(analysisDate).isNotSameAs(someDate);
  }

  @Test
  public void setAnalysisDate_throws_ISE_when_called_twice() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Analysis date has already been set");

    ReportAnalysisMetadataHolder underTest = new ReportAnalysisMetadataHolder();

    underTest.setAnalysisDate(someDate);
    underTest.setAnalysisDate(someDate);
  }
}
