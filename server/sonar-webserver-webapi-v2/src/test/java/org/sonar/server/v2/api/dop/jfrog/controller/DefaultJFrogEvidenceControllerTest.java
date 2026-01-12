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
package org.sonar.server.v2.api.dop.jfrog.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.v2.api.dop.jfrog.response.GateCondition;
import org.sonar.server.v2.api.dop.jfrog.response.GateStatus;
import org.sonar.server.v2.api.dop.jfrog.response.QualityGateEvidence;
import org.sonar.server.v2.api.dop.jfrog.response.SonarQubePredicate;
import org.sonar.server.v2.api.dop.jfrog.response.SonarQubeStatement;
import org.sonar.server.v2.api.dop.jfrog.service.JFrogEvidenceHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.v2.WebApiEndpoints.JFROG_EVIDENCE_ENDPOINT;
import static org.sonar.server.v2.api.ControllerTester.getMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefaultJFrogEvidenceControllerTest {

  private static final String TASK_ID = "task-uuid-123";
  private static final Gson gson = new Gson();

  private final JFrogEvidenceHandler jfrogEvidenceHandler = mock();
  private final MockMvc mockMvc = getMockMvc(new DefaultJFrogEvidenceController(jfrogEvidenceHandler));

  @Test
  void getEvidence_whenTaskNotFound_shouldReturn404() throws Exception {
    when(jfrogEvidenceHandler.getEvidence(TASK_ID))
      .thenThrow(new NotFoundException("Task '" + TASK_ID + "' not found"));

    mockMvc.perform(get(JFROG_EVIDENCE_ENDPOINT + "/" + TASK_ID))
      .andExpect(status().isNotFound());
  }

  @Test
  void getEvidence_whenNotAuthorized_shouldReturn403() throws Exception {
    when(jfrogEvidenceHandler.getEvidence(TASK_ID))
      .thenThrow(new ForbiddenException("Insufficient privileges"));

    mockMvc.perform(get(JFROG_EVIDENCE_ENDPOINT + "/" + TASK_ID))
      .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @MethodSource("gateStatusTestCases")
  void getEvidence_shouldReturnCorrectStatus(GateStatus inputStatus, String expectedStatus) throws Exception {
    QualityGateEvidence qualityGate = QualityGateEvidence.builder()
      .status(inputStatus)
      .ignoredConditions(false)
      .conditions(List.of())
      .build();

    SonarQubePredicate predicate = new SonarQubePredicate(List.of(qualityGate));
    SonarQubeStatement statement = SonarQubeStatement.create(predicate, "# Markdown");

    when(jfrogEvidenceHandler.getEvidence(TASK_ID)).thenReturn(statement);

    MvcResult result = mockMvc.perform(get(JFROG_EVIDENCE_ENDPOINT + "/" + TASK_ID))
      .andExpect(status().isOk())
      .andReturn();

    JsonObject response = gson.fromJson(result.getResponse().getContentAsString(), JsonObject.class);

    assertThat(response.get("_type").getAsString()).isEqualTo(SonarQubeStatement.STATEMENT_TYPE);
    assertThat(response.get("predicateType").getAsString()).isEqualTo(SonarQubeStatement.PREDICATE_TYPE);
    assertThat(response.getAsJsonObject("predicate")
      .getAsJsonArray("gates").get(0).getAsJsonObject()
      .get("status").getAsString()).isEqualTo(expectedStatus);
  }

  static Stream<Arguments> gateStatusTestCases() {
    return Stream.of(
      Arguments.of(GateStatus.OK, "OK"),
      Arguments.of(GateStatus.ERROR, "ERROR"),
      Arguments.of(GateStatus.WARN, "WARN"),
      Arguments.of(GateStatus.NONE, "NONE"));
  }

  @Test
  void getEvidence_withConditions_shouldReturnConditionDetails() throws Exception {
    QualityGateEvidence qualityGate = QualityGateEvidence.builder()
      .status(GateStatus.OK)
      .ignoredConditions(false)
      .conditions(List.of(
        GateCondition.builder()
          .status(GateStatus.OK)
          .metricKey("new_coverage")
          .comparator(GateCondition.Comparator.LT)
          .errorThreshold("80")
          .actualValue("85")
          .build()))
      .build();

    SonarQubePredicate predicate = new SonarQubePredicate(List.of(qualityGate));
    SonarQubeStatement statement = SonarQubeStatement.create(predicate, "# Quality Gate Passed");

    when(jfrogEvidenceHandler.getEvidence(TASK_ID)).thenReturn(statement);

    MvcResult result = mockMvc.perform(get(JFROG_EVIDENCE_ENDPOINT + "/" + TASK_ID))
      .andExpect(status().isOk())
      .andReturn();

    JsonObject response = gson.fromJson(result.getResponse().getContentAsString(), JsonObject.class);
    JsonObject condition = response.getAsJsonObject("predicate")
      .getAsJsonArray("gates").get(0).getAsJsonObject()
      .getAsJsonArray("conditions").get(0).getAsJsonObject();

    assertThat(condition.get("metricKey").getAsString()).isEqualTo("new_coverage");
    assertThat(condition.get("comparator").getAsString()).isEqualTo("LT");
    assertThat(condition.get("errorThreshold").getAsString()).isEqualTo("80");
    assertThat(condition.get("actualValue").getAsString()).isEqualTo("85");
    assertThat(response.get("markdown").getAsString()).isEqualTo("# Quality Gate Passed");
  }

}
