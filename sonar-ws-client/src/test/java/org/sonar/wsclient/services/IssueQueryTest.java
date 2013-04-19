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

import java.text.SimpleDateFormat;

import static org.fest.assertions.Assertions.assertThat;

public class IssueQueryTest extends QueryTestCase {

  @Test
  public void get_all_issues() {
    IssueQuery query = IssueQuery.create();
    assertThat(query.getUrl()).isEqualTo("/api/issues/search?");
  }

  @Test
  public void get_all_issues_by_parameter() {
    IssueQuery query = IssueQuery.create()
        .setKeys("ABCDE", "FGHIJ")
        .setAssigneeLogins("arthur", "perceval")
        .setComponents("component1", "component2")
        .setComponentRoots("componentRoot1", "componentRoot2")
        .setLimit(1)
        .setResolutions("resolution1", "resolution2")
        .setRules("squid:AvoidCycle")
        .setStatus("OPEN", "CLOSED")
        .setSeverities("BLOCKER", "INFO")
        .setUserLogins("userLogin1", "userLogin2")
        ;
    assertThat(query.getUrl()).isEqualTo("/api/issues/search?keys=ABCDE,FGHIJ&severities=BLOCKER,INFO&statuses=OPEN,CLOSED&" +
        "resolutions=resolution1,resolution2&components=component1,component2&componentRoots=componentRoot1,componentRoot2&rules=squid%3AAvoidCycle&" +
        "userLogins=userLogin1,userLogin2&assignees=arthur,perceval&limit=1&");
  }

  @Test
  public void get_all_issues_by_created_at() throws Exception {
    IssueQuery query = IssueQuery.create()
        .setCreatedAfter(new SimpleDateFormat("yyyy-MM-dd").parse("2013-04-16"))
        .setCreatedBefore(new SimpleDateFormat("yyyy-MM-dd").parse("2010-02-18"))
        ;
    assertThat(query.getUrl()).isEqualTo("/api/issues/search?createdAfter=Tue+Apr+16+00%3A00%3A00+CEST+2013&createdBefore=Thu+Feb+18+00%3A00%3A00+CET+2010&");
  }

}
