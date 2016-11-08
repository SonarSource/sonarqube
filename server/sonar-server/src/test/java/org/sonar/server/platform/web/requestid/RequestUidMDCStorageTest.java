/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.web.requestid;

import org.apache.log4j.MDC;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestUidMDCStorageTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @After
  public void tearDown() throws Exception {
    MDC.clear();
  }

  @Test
  public void constructor_fails_with_NPE_when_argument_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Request UID can't be null");

    new RequestUidMDCStorage(null);
  }

  @Test
  public void constructor_adds_specified_value_in_MDC_under_HTTP_REQUEST_ID_key() {
    new RequestUidMDCStorage("toto");

    assertThat(MDC.get("HTTP_REQUEST_ID")).isEqualTo("toto");
  }

  @Test
  public void close_removes_value_from_MDC() {
    RequestUidMDCStorage underTest = new RequestUidMDCStorage("boum");
    assertThat(MDC.get("HTTP_REQUEST_ID")).isEqualTo("boum");

    underTest.close();

    assertThat(MDC.get("HTTP_REQUEST_ID")).isNull();
  }
}
