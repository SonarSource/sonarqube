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

package org.sonar.wsclient.services;

import org.junit.Test;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class IssueTest {

  @Test
  public void test_getter_and_setter() {
    Issue issue = new Issue()
        .setKey("key")
        .setComponentKey("componentKey")
        .setRuleKey("ruleKey")
        .setRuleRepositoryKey("ruleRepositoryKey")
        .setSeverity("severity")
        .setTitle("title")
        .setMessage("message")
        .setLine(1)
        .setCost(1.0)
        .setStatus("status")
        .setResolution("resolution")
        .setUserLogin("userLogin")
        .setAssigneeLogin("assigneeLogin")
        .setCreatedAt(new Date())
        .setUpdatedAt(new Date())
        .setClosedAt(new Date());

    assertThat(issue.getKey()).isNotNull();
    assertThat(issue.getComponentKey()).isNotNull();
    assertThat(issue.getRuleKey()).isNotNull();
    assertThat(issue.getRuleRepositoryKey()).isNotNull();
    assertThat(issue.getSeverity()).isNotNull();
    assertThat(issue.getTitle()).isNotNull();
    assertThat(issue.getMessage()).isNotNull();
    assertThat(issue.getLine()).isNotNull();
    assertThat(issue.getCost()).isNotNull();
    assertThat(issue.getStatus()).isNotNull();
    assertThat(issue.getResolution()).isNotNull();
    assertThat(issue.getUserLogin()).isNotNull();
    assertThat(issue.getAssigneeLogin()).isNotNull();
    assertThat(issue.getCreatedAt()).isNotNull();
    assertThat(issue.getUpdatedAt()).isNotNull();
    assertThat(issue.getClosedAt()).isNotNull();

  }

}
