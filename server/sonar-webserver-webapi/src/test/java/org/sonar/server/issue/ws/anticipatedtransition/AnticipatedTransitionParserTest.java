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
package org.sonar.server.issue.ws.anticipatedtransition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.AnticipatedTransition;

import static org.assertj.core.api.Assertions.assertThat;

public class AnticipatedTransitionParserTest {

  private static final String USER_UUID = "userUuid";
  private static final String PROJECT_KEY = "projectKey";
  AnticipatedTransitionParser underTest = new AnticipatedTransitionParser();

  @Test
  public void givenRequestBodyWithMultipleTransition_whenParse_thenAllTransitionsAreReturned() throws IOException {
    // given
    String requestBody = readTestResourceFile("request-with-transitions.json");

    // when
    List<AnticipatedTransition> anticipatedTransitions = underTest.parse(requestBody, USER_UUID, PROJECT_KEY);

    // then
    // assert that all transitions are returned
    assertThat(anticipatedTransitions)
      .hasSize(2)
      .containsExactlyElementsOf(transitionsExpectedFromTestFile());
  }

  @Test
  public void givenRequestBodyWithNoTransitions_whenParse_ThenAnEmptyListIsReturned() {
    // given
    String requestBody = "[]";

    // when
    List<AnticipatedTransition> anticipatedTransitions = underTest.parse(requestBody, USER_UUID, PROJECT_KEY);

    // then
    assertThat(anticipatedTransitions).isEmpty();
  }

  @Test
  public void givenRequestBodyWithInvalidJson_whenParse_thenExceptionIsThrown() {
    // given
    String requestBody = "invalidJson";

    // when then
    Assertions.assertThatThrownBy(() -> underTest.parse(requestBody, USER_UUID, PROJECT_KEY))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Unable to parse anticipated transitions from request body.");
  }

  @Test
  public void givenRequestBodyWithInvalidTransition_whenParse_thenExceptionIsThrown() throws IOException {
    // given
    String requestBodyWithInvalidTransition = """
      [
        {
          "ruleKey": "squid:S0001",
          "issueMessage": "issueMessage1",
          "filePath": "filePath1",
          "line": 1,
          "lineHash": "lineHash1",
          "transition": "invalid-transition",
          "comment": "comment1"
        },
      ]
      """;

    // when
    Assertions.assertThatThrownBy(() -> underTest.parse(requestBodyWithInvalidTransition, USER_UUID, PROJECT_KEY))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Transition 'invalid-transition' not supported. Only 'wontfix','accept','falsepositive' are supported.");
  }

  // Handwritten Anticipated Transitions that are expected from the request-with-transitions.json file
  private List<AnticipatedTransition> transitionsExpectedFromTestFile() {
    return List.of(
      new AnticipatedTransition(
        null,
        PROJECT_KEY,
        USER_UUID,
        RuleKey.parse("squid:S0001"),
        "issueMessage1",
        "filePath1",
        1,
        "lineHash1",
        "wontfix",
        "comment1"),
      new AnticipatedTransition(
        null,
        PROJECT_KEY,
        USER_UUID,
        RuleKey.parse("squid:S0002"),
        "issueMessage2",
        "filePath2",
        2,
        "lineHash2",
        "falsepositive",
        "comment2"));
  }

  private String readTestResourceFile(String fileName) throws IOException {
    return Files.readString(Path.of(getClass().getResource(fileName).getPath()));
  }

}
