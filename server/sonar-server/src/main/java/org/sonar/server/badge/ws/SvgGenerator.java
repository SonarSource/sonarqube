/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ServerSide;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonar.api.measures.Metric.Level.ERROR;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.api.measures.Metric.Level.WARN;
import static org.sonar.process.ProcessProperties.Property.SONARCLOUD_ENABLED;

@ServerSide
public class SvgGenerator {

  private static final Map<Character, Integer> CHAR_LENGTH = ImmutableMap.<Character, Integer>builder()
    .put('0', 7)
    .put('1', 7)
    .put('2', 7)
    .put('3', 7)
    .put('4', 7)
    .put('5', 7)
    .put('6', 7)
    .put('7', 7)
    .put('8', 7)
    .put('9', 7)
    .put('a', 7)
    .put('b', 7)
    .put('c', 6)
    .put('d', 7)
    .put('e', 6)
    .put('f', 4)
    .put('g', 7)
    .put('h', 7)
    .put('i', 3)
    .put('j', 5)
    .put('k', 6)
    .put('l', 3)
    .put('m', 11)
    .put('n', 7)
    .put('o', 7)
    .put('p', 7)
    .put('q', 7)
    .put('r', 5)
    .put('s', 6)
    .put('t', 4)
    .put('u', 7)
    .put('v', 6)
    .put('w', 9)
    .put('x', 6)
    .put('y', 6)
    .put('z', 6)
    .put('A', 7)
    .put('B', 7)
    .put('C', 8)
    .put('D', 8)
    .put('E', 7)
    .put('F', 6)
    .put('G', 8)
    .put('H', 8)
    .put('I', 5)
    .put('J', 5)
    .put('K', 7)
    .put('L', 6)
    .put('M', 9)
    .put('N', 8)
    .put('O', 9)
    .put('P', 7)
    .put('Q', 9)
    .put('R', 8)
    .put('S', 7)
    .put('T', 7)
    .put('U', 8)
    .put('V', 10)
    .put('W', 10)
    .put('X', 7)
    .put('Y', 7)
    .put('Z', 7)
    .put('%', 12)
    .put(' ', 4)
    .put('.', 4)
    .put('_', 7)
    .put('\'', 3)
    .build();

  private static final String TEMPLATES_SONARCLOUD = "templates/sonarcloud";
  private static final String TEMPLATES_SONARQUBE = "templates/sonarqube";

  private static final int MARGIN = 6;
  private static final int ICON_WIDTH = 20;

  private static final String PARAMETER_ICON_WIDTH_PLUS_MARGIN = "iconWidthPlusMargin";
  private static final String PARAMETER_TOTAL_WIDTH = "totalWidth";
  private static final String PARAMETER_LEFT_WIDTH = "leftWidth";
  private static final String PARAMETER_LEFT_WIDTH_PLUS_MARGIN = "leftWidthPlusMargin";
  private static final String PARAMETER_RIGHT_WIDTH = "rightWidth";
  private static final String PARAMETER_LABEL_WIDTH = "labelWidth";
  private static final String PARAMETER_VALUE_WIDTH = "valueWidth";
  private static final String PARAMETER_COLOR = "color";
  private static final String PARAMETER_LABEL = "label";
  private static final String PARAMETER_VALUE = "value";

  private final String errorTemplate;
  private final String badgeTemplate;
  private final Map<Metric.Level, String> qualityGateTemplates;

  public SvgGenerator(Configuration config) {
    boolean isOnSonarCloud = config.getBoolean(SONARCLOUD_ENABLED.getKey()).orElse(false);
    String templatePath = isOnSonarCloud ? TEMPLATES_SONARCLOUD : TEMPLATES_SONARQUBE;
    this.errorTemplate = readTemplate("templates/error.svg");
    this.badgeTemplate = readTemplate(templatePath + "/badge.svg");
    this.qualityGateTemplates = ImmutableMap.of(
      OK, readTemplate(templatePath + "/quality_gate_passed.svg"),
      WARN, readTemplate(templatePath + "/quality_gate_warn.svg"),
      ERROR, readTemplate(templatePath + "/quality_gate_failed.svg"));
  }

  public String generateBadge(String label, String value, Color backgroundValueColor) {
    int labelWidth = computeWidth(label);
    int valueWidth = computeWidth(value);

    Map<String, String> values = ImmutableMap.<String, String>builder()
      .put(PARAMETER_TOTAL_WIDTH, valueOf(MARGIN * 4 + ICON_WIDTH + labelWidth + valueWidth))
      .put(PARAMETER_LABEL_WIDTH, valueOf(labelWidth))
      .put(PARAMETER_VALUE_WIDTH, valueOf(valueWidth))
      .put(PARAMETER_LEFT_WIDTH, valueOf(MARGIN * 2 + ICON_WIDTH + labelWidth))
      .put(PARAMETER_LEFT_WIDTH_PLUS_MARGIN, valueOf(MARGIN * 3 + ICON_WIDTH + labelWidth))
      .put(PARAMETER_RIGHT_WIDTH, valueOf(MARGIN * 2 + valueWidth))
      .put(PARAMETER_ICON_WIDTH_PLUS_MARGIN, valueOf(MARGIN + ICON_WIDTH))
      .put(PARAMETER_COLOR, backgroundValueColor.getValue())
      .put(PARAMETER_LABEL, label)
      .put(PARAMETER_VALUE, value)
      .build();
    StrSubstitutor strSubstitutor = new StrSubstitutor(values);
    return strSubstitutor.replace(badgeTemplate);
  }

  public String generateQualityGate(Metric.Level level) {
    return qualityGateTemplates.get(level);
  }

  public String generateError(String error) {
    Map<String, String> values = ImmutableMap.of(
      PARAMETER_TOTAL_WIDTH, valueOf(MARGIN + computeWidth(error) + MARGIN),
      PARAMETER_LABEL, error);
    StrSubstitutor strSubstitutor = new StrSubstitutor(values);
    return strSubstitutor.replace(errorTemplate);
  }

  private static int computeWidth(String text) {
    return text.chars()
      .mapToObj(i -> (char) i)
      .mapToInt(c -> {
        Integer length = CHAR_LENGTH.get(c);
        checkState(length != null, "Invalid character '%s'", c);
        return length;
      })
      .sum();
  }

  private String readTemplate(String template) {
    try {
      return IOUtils.toString(getClass().getResource(template), UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Can't read svg template '%s'", template), e);
    }
  }

  static class Color {
    static final Color DEFAULT = new Color("#999");
    static final Color QUALITY_GATE_OK = new Color("#4c1");
    static final Color QUALITY_GATE_WARN = new Color("#ed7d20");
    static final Color QUALITY_GATE_ERROR = new Color("#d4333f");
    static final Color RATING_A = new Color("#00aa00");
    static final Color RATING_B = new Color("#b0d513");
    static final Color RATING_C = new Color("#eabe06");
    static final Color RATING_D = new Color("#ed7d20");
    static final Color RATING_E = new Color("#e00");

    private final String value;

    private Color(String value) {
      this.value = value;
    }

    String getValue() {
      return value;
    }
  }
}
