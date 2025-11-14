/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.alm.client.github;

import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static java.lang.String.join;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.alm.client.github.SonarQubeIssueKeyFormatter.SONAR_ISSUE_KEY_PREFIX;
import static org.sonar.alm.client.github.SonarQubeIssueKeyFormatter.SONAR_ISSUE_KEY_SUFFIX;

public class SonarQubeIssueKeyFormatterTest {

  @Test
  public void should_serializeIssueKey() {
    String issueKey = RandomStringUtils.secure().nextAlphanumeric(20);

    String serialized = SonarQubeIssueKeyFormatter.serialize(issueKey);

    String expectedSerializedKey = join("", SONAR_ISSUE_KEY_PREFIX, issueKey, SONAR_ISSUE_KEY_SUFFIX);
    assertThat(serialized).isEqualTo(expectedSerializedKey);
  }

  @Test
  public void should_deserializeIssueKey() {
    String issueKey = RandomStringUtils.secure().nextAlphanumeric(20);
    String message = join("", SONAR_ISSUE_KEY_PREFIX, issueKey, SONAR_ISSUE_KEY_SUFFIX, "a message");

    Optional<String> deserialized = SonarQubeIssueKeyFormatter.deserialize(message);

    assertThat(deserialized).hasValue(issueKey);
  }

  @Test
  public void should_notDeserializeIssueKey_when_messageHasWrongFormat() {
    String issueKey = RandomStringUtils.secure().nextAlphanumeric(20);
    String messageWithoutSuffix = join("", SONAR_ISSUE_KEY_PREFIX, issueKey, "a message");
    String messageWithoutPrefix = join("", issueKey, SONAR_ISSUE_KEY_SUFFIX, "a message");
    String messageWithPrefixSuffixReversed = join("", SONAR_ISSUE_KEY_SUFFIX, issueKey, SONAR_ISSUE_KEY_PREFIX, "a message");
    String messageWithNoPrefixSuffix = join("", issueKey, "a message");

    assertThat(SonarQubeIssueKeyFormatter.deserialize(messageWithoutSuffix)).isEmpty();
    assertThat(SonarQubeIssueKeyFormatter.deserialize(messageWithoutPrefix)).isEmpty();
    assertThat(SonarQubeIssueKeyFormatter.deserialize(messageWithPrefixSuffixReversed)).isEmpty();
    assertThat(SonarQubeIssueKeyFormatter.deserialize(messageWithNoPrefixSuffix)).isEmpty();
  }
}
