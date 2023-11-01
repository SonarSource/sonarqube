/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.core.issue.status;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTester;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleStatusTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void of_shouldMapToCorrectSimpleStatus() {
    assertThat(SimpleStatus.of(Issue.STATUS_RESOLVED, Issue.RESOLUTION_FIXED))
      .isEqualTo(SimpleStatus.FIXED);

    assertThat(SimpleStatus.of(Issue.STATUS_CONFIRMED, null))
      .isEqualTo(SimpleStatus.CONFIRMED);

    assertThat(SimpleStatus.of(Issue.STATUS_RESOLVED, Issue.RESOLUTION_FALSE_POSITIVE))
      .isEqualTo(SimpleStatus.FALSE_POSITIVE);

    assertThat(SimpleStatus.of(Issue.STATUS_RESOLVED, Issue.RESOLUTION_WONT_FIX))
      .isEqualTo(SimpleStatus.ACCEPTED);

    assertThat(SimpleStatus.of(Issue.STATUS_REOPENED, null))
      .isEqualTo(SimpleStatus.OPEN);

    assertThat(SimpleStatus.of(Issue.STATUS_CLOSED, null))
      .isEqualTo(SimpleStatus.FIXED);
  }

  @Test
  public void of_shouldReturnNull_WhenStatusBelongsToHotspot() {
    assertThat(SimpleStatus.of(Issue.STATUS_TO_REVIEW, null))
      .isNull();

    assertThat(SimpleStatus.of(Issue.STATUS_REVIEWED, Issue.RESOLUTION_SAFE))
      .isNull();

    assertThat(SimpleStatus.of(Issue.STATUS_REVIEWED, Issue.RESOLUTION_ACKNOWLEDGED))
      .isNull();
  }

  @Test
  public void of_shouldThrowExceptionWhenUnknownMapping() {
    assertThat(SimpleStatus.of(Issue.STATUS_RESOLVED, null)).isNull();
    assertThat(logTester.getLogs()).extracting(LogAndArguments::getFormattedMsg).contains("Can't find mapped simple status for status 'RESOLVED' and resolution 'null'");

    assertThat(SimpleStatus.of(Issue.STATUS_RESOLVED, Issue.RESOLUTION_SAFE)).isNull();
    assertThat(logTester.getLogs()).extracting(LogAndArguments::getFormattedMsg).contains("Can't find mapped simple status for status 'RESOLVED' and resolution 'SAFE'");
  }

}
