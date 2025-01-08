/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.core.issue;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;

public class AnticipatedTransitionTest {

  @Test
  public void givenTwoAnticipatedTransitions_whenFieldsHaveTheSameValue_theyShouldBeEqual() {
    AnticipatedTransition anticipatedTransition = getAnticipatedTransition();
    AnticipatedTransition anticipatedTransition2 = getAnticipatedTransition();

    assertFieldsAreTheSame(anticipatedTransition, anticipatedTransition2);
    Assertions.assertThat(anticipatedTransition).isEqualTo(anticipatedTransition2);
  }

  @Test
  public void givenTwoAnticipatedTransitions_whenFieldsHaveTheSameValue_hashcodeShouldBeTheSame() {
    AnticipatedTransition anticipatedTransition = getAnticipatedTransition();
    AnticipatedTransition anticipatedTransition2 = getAnticipatedTransition();

    assertFieldsAreTheSame(anticipatedTransition, anticipatedTransition2);
    Assertions.assertThat(anticipatedTransition).hasSameHashCodeAs(anticipatedTransition2);
  }

  private void assertFieldsAreTheSame(AnticipatedTransition anticipatedTransition, AnticipatedTransition anticipatedTransition2) {
    Assertions.assertThat(anticipatedTransition.getProjectKey()).isEqualTo(anticipatedTransition2.getProjectKey());
    Assertions.assertThat(anticipatedTransition.getUserUuid()).isEqualTo(anticipatedTransition2.getUserUuid());
    Assertions.assertThat(anticipatedTransition.getTransition()).isEqualTo(anticipatedTransition2.getTransition());
    Assertions.assertThat(anticipatedTransition.getComment()).isEqualTo(anticipatedTransition2.getComment());
    Assertions.assertThat(anticipatedTransition.getFilePath()).isEqualTo(anticipatedTransition2.getFilePath());
    Assertions.assertThat(anticipatedTransition.getLine()).isEqualTo(anticipatedTransition2.getLine());
    Assertions.assertThat(anticipatedTransition.getMessage()).isEqualTo(anticipatedTransition2.getMessage());
    Assertions.assertThat(anticipatedTransition.getLineHash()).isEqualTo(anticipatedTransition2.getLineHash());
    Assertions.assertThat(anticipatedTransition.getRuleKey()).isEqualTo(anticipatedTransition2.getRuleKey());
    Assertions.assertThat(anticipatedTransition.getUuid()).isEqualTo(anticipatedTransition2.getUuid());
  }

  private AnticipatedTransition getAnticipatedTransition() {
    return new AnticipatedTransition(
      null,
      "projectKey",
      "userUuid",
      RuleKey.parse("rule:key"),
      "message",
      "filepath",
      1,
      "lineHash",
      "transition",
      "comment"
    );
  }

}
