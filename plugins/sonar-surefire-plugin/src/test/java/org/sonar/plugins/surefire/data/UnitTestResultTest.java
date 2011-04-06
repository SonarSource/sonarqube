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
package org.sonar.plugins.surefire.data;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UnitTestResultTest {

  @Test
  public void shouldBeError() {
    UnitTestResult result = new UnitTestResult().setStatus(UnitTestResult.STATUS_ERROR);
    assertThat(result.getStatus(), is(UnitTestResult.STATUS_ERROR));
    assertThat(result.isError(), is(true));
    assertThat(result.isErrorOrFailure(), is(true));
  }

  @Test
  public void shouldBeFailure() {
    UnitTestResult result = new UnitTestResult().setStatus(UnitTestResult.STATUS_FAILURE);
    assertThat(result.getStatus(), is(UnitTestResult.STATUS_FAILURE));
    assertThat(result.isError(), is(false));
    assertThat(result.isErrorOrFailure(), is(true));
  }

  @Test
  public void shouldBeSuccess() {
    UnitTestResult result = new UnitTestResult().setStatus(UnitTestResult.STATUS_OK);
    assertThat(result.getStatus(), is(UnitTestResult.STATUS_OK));
    assertThat(result.isError(), is(false));
    assertThat(result.isErrorOrFailure(), is(false));
  }

  @Test
  public void shouldExportSuccessToXml() {
    UnitTestResult result = new UnitTestResult().setStatus(UnitTestResult.STATUS_OK);
    result.setDurationMilliseconds(520L);
    result.setName("testOne");

    assertThat(result.toXml(), is("<testcase status=\"ok\" time=\"520\" name=\"testOne\"/>"));
  }

  @Test
  public void shouldExportErrorToXml() {
    UnitTestResult result = new UnitTestResult().setStatus(UnitTestResult.STATUS_ERROR);
    result.setDurationMilliseconds(580L);
    result.setName("testOne");
    result.setStackTrace("java.lang.RuntimeException");
    result.setMessage("expected xxx");

    assertThat(result.toXml(), is("<testcase status=\"error\" time=\"580\" name=\"testOne\"><error message=\"expected xxx\"><![CDATA[java.lang.RuntimeException]]></error></testcase>"));
  }
}
