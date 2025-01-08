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
package org.sonar.server.badge.ws;

import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.api.measures.Metric;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.measures.Metric.Level.ERROR;
import static org.sonar.server.badge.ws.SvgGenerator.Color.DEFAULT;

public class SvgGeneratorTest {

  private SvgGenerator underTest;

  @Test
  public void generate_badge() {
    initSvgGenerator();

    String result = underTest.generateBadge("label", "10", DEFAULT);

    checkBadge(result, "label", "10", DEFAULT);
  }

  @Test
  public void generate_quality_gate() {
    initSvgGenerator();

    String result = underTest.generateQualityGate(ERROR);

    checkQualityGate(result, ERROR);
  }

  @Test
  public void generate_error() {
    initSvgGenerator();

    String result = underTest.generateError("Error");

    assertThat(result).contains("<text", ">Error</text>");
  }

  @Test
  public void fail_when_unknown_character() {
    initSvgGenerator();

    assertThatThrownBy(() -> underTest.generateError("Méssage with accent"))
      .hasMessage("Invalid character 'é'");
  }

  private void initSvgGenerator() {
    underTest = new SvgGenerator();
  }

  private void checkBadge(String svg, String expectedLabel, String expectedValue, SvgGenerator.Color expectedColorValue) {
    assertThat(svg).contains(
      "<text", expectedLabel + "</text>",
      "<text", expectedValue + "</text>",
      "rect fill=\"" + expectedColorValue.getValue() + "\"");
  }

  private void checkQualityGate(String response, Metric.Level status) {
    switch (status) {
      case OK:
        assertThat(response).isEqualTo(readTemplate("quality_gate_passed.svg"));
        break;
      case ERROR:
        assertThat(response).isEqualTo(readTemplate("quality_gate_failed.svg"));
        break;
    }
  }

  private String readTemplate(String template) {
    try {
      return IOUtils.toString(getClass().getResource("templates/sonarqube/" + template), UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Can't read svg template '%s'", template), e);
    }
  }

}
