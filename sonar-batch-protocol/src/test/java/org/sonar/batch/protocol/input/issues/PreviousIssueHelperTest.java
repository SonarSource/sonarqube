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
package org.sonar.batch.protocol.input.issues;

import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

public class PreviousIssueHelperTest {

  @Test
  public void writeIssues() throws JSONException {
    PreviousIssueHelper helper = PreviousIssueHelper.create();
    StringWriter out = new StringWriter();

    PreviousIssue issue1 = new PreviousIssue();
    issue1.setKey("key1");
    issue1.setComponentPath("path");
    issue1.setRuleKey("repokey", "rulekey");
    issue1.setLine(2);
    issue1.setMessage("message");
    issue1.setSeverity("severity");
    issue1.setResolution("resolution");
    issue1.setStatus("status");
    issue1.setChecksum("checksum");
    issue1.setAssigneeLogin("login");
    issue1.setAssigneeFullname("fullname");
    PreviousIssue issue2 = new PreviousIssue();
    issue2.setKey("key2");

    helper.streamIssues(out, Arrays.asList(issue1, issue2), new PreviousIssueHelper.Function<PreviousIssue, PreviousIssue>() {
      @Override
      public PreviousIssue apply(PreviousIssue from) {
        return from;
      }
    });

    JSONAssert
      .assertEquals(
        "[{\"key\": \"key1\", \"componentPath\": \"path\", \"ruleKey\": \"rulekey\", \"ruleRepo\": \"repokey\", \"line\": 2,\"message\": \"message\", \"severity\": \"severity\", \"resolution\": \"resolution\", \"status\": \"status\", \"checksum\": \"checksum\",\"assigneeLogin\": \"login\", \"assigneeFullname\": \"fullname\"},"
          +
          "{\"key\": \"key2\"}]",
        out.getBuffer().toString(), true);
  }

  @Test
  public void readIssues() {
    PreviousIssueHelper helper = PreviousIssueHelper.create();
    StringReader reader = new StringReader(
      "[{\"key\": \"key1\", \"componentPath\": \"path\", \"ruleKey\": \"rulekey\", \"ruleRepo\": \"repokey\", \"line\": 2,\"message\": \"message\", \"severity\": \"severity\", \"resolution\": \"resolution\", \"status\": \"status\", \"checksum\": \"checksum\",\"assigneeLogin\": \"login\", \"assigneeFullname\": \"fullname\"},"
        +
        "{\"key\": \"key2\"}]");

    Iterator<PreviousIssue> iterator = helper.getIssues(reader).iterator();
    PreviousIssue issue1 = iterator.next();
    assertThat(iterator.hasNext()).isTrue();
    PreviousIssue issue2 = iterator.next();
    assertThat(iterator.hasNext()).isFalse();

    assertThat(issue1.key()).isEqualTo("key1");
    assertThat(issue1.componentPath()).isEqualTo("path");
    assertThat(issue1.ruleRepo()).isEqualTo("repokey");
    assertThat(issue1.ruleKey()).isEqualTo("rulekey");
    assertThat(issue1.line()).isEqualTo(2);
    assertThat(issue1.message()).isEqualTo("message");
    assertThat(issue1.severity()).isEqualTo("severity");
    assertThat(issue1.resolution()).isEqualTo("resolution");
    assertThat(issue1.status()).isEqualTo("status");
    assertThat(issue1.checksum()).isEqualTo("checksum");
    assertThat(issue1.assigneeLogin()).isEqualTo("login");
    assertThat(issue1.assigneeFullname()).isEqualTo("fullname");

    assertThat(issue2.key()).isEqualTo("key2");
  }

}
