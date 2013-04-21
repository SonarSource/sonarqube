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
package org.sonar.wsclient;

import org.junit.Test;
import org.sonar.wsclient.issue.DefaultIssueClient;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class SonarClientTest {
  @Test
  public void should_build_client() {
    SonarClient client = SonarClient.builder().url("http://localhost:9000").build();
    assertThat(client.issueClient()).isNotNull().isInstanceOf(DefaultIssueClient.class);
  }

  @Test
  public void url_should_not_be_null() {
    try {
      SonarClient.builder().build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Server URL must be set");
    }
  }

  @Test
  public void url_should_not_be_empty() {
    try {
      SonarClient.builder().url("").build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Server URL must be set");
    }
  }
}
