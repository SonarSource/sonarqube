/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;
import org.sonar.wsclient.services.Violation;

/**
 * @deprecated in 3.6. Replaced by issues.
 */
@Deprecated

public class ViolationUnmarshallerTest extends UnmarshallerTestCase {

  @Test
  public void testToModels() {
    Violation violation = new ViolationUnmarshaller().toModel("[]");
    assertThat(violation, nullValue());

    List<Violation> violations = new ViolationUnmarshaller().toModels(loadFile("/violations/violations.json"));
    assertThat(violations.size(), is(2));

    violation = violations.get(0);
    assertThat(violation.getKey(), is("1"));
    assertThat(violation.getMessage(), is("throw java.lang.Exception"));
    assertThat(violation.hasLine(), is(true));
    assertThat(violation.getLine(), is(97));
    assertThat(violation.getCreatedAt(), notNullValue());
    assertThat(violation.getSeverity(), is("MAJOR"));
    assertThat(violation.getRuleKey(), is("pmd:SignatureDeclareThrowsException"));
    assertThat(violation.getRuleName(), is("Signature Declare Throws Exception"));
    assertThat(violation.getResourceKey(),
        is("org.apache.excalibur.components:excalibur-pool-instrumented:org.apache.avalon.excalibur.pool.TraceableResourceLimitingPool"));
    assertThat(violation.getResourceName(), is("TraceableResourceLimitingPool"));
    assertThat(violation.getResourceQualifier(), is("CLA"));
    assertThat(violation.isSwitchedOff(), is(false));
  }

  @Test
  public void testViolationWithoutLineNumber() {
    Violation violation = new ViolationUnmarshaller().toModel(loadFile("/violations/violation-without-optional-fields.json"));
    assertThat(violation.getMessage(), not(nullValue()));
    assertThat(violation.hasLine(), is(false));
    assertThat(violation.getLine(), nullValue());
    assertThat(violation.getCreatedAt(), nullValue());
  }

  @Test
  public void testSwitchedOff() {
    Violation violation = new ViolationUnmarshaller().toModel(loadFile("/violations/false-positive.json"));
    assertThat(violation.isSwitchedOff(), is(true));
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2386
   */
  @Test
  public void testIncorrectLine() {
    Violation violation = new ViolationUnmarshaller().toModel(loadFile("/violations/violation-with-incorrect-line.json"));
    assertThat(violation.hasLine(), is(false));
    assertThat(violation.getLine(), nullValue());
  }
}
