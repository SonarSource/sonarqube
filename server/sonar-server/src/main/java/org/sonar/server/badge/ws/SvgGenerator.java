/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.sonar.api.measures.Metric;
import org.sonar.api.server.ServerSide;

import static java.lang.String.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonar.api.measures.Metric.Level.ERROR;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.api.measures.Metric.Level.WARN;

@ServerSide
public class SvgGenerator {

  private static final FontRenderContext FONT_RENDER_CONTEXT = new FontRenderContext(new AffineTransform(), true, true);
  private static final Font FONT = new Font("Verdana", Font.PLAIN, 11);

  private static final int MARGIN = 6;
  private static final int ICON_WIDTH = 20;

  private static final String PARAMETER_ICON_WIDTH_PLUS_MARGIN = "iconWidthPlusMargin";
  private static final String PARAMETER_TOTAL_WIDTH = "totalWidth";
  private static final String PARAMETER_LABEL_WIDTH = "labelWidth";
  private static final String PARAMETER_LABEL_WIDTH_PLUS_MARGIN = "labelWidthPlusMargin";
  private static final String PARAMETER_VALUE_WIDTH = "valueWidth";
  private static final String PARAMETER_COLOR = "color";
  private static final String PARAMETER_LABEL = "label";
  private static final String PARAMETER_VALUE = "value";

  private final String errorTemplate;
  private final String badgeTemplate;
  private final Map<Metric.Level, String> qualityGateTemplates;

  public SvgGenerator() {
    this.errorTemplate = readTemplate("error.svg");
    this.badgeTemplate = readTemplate("badge.svg");
    this.qualityGateTemplates = ImmutableMap.of(
      OK, readTemplate("quality_gate_passed.svg"),
      WARN, readTemplate("quality_gate_warn.svg"),
      ERROR, readTemplate("quality_gate_failed.svg"));
  }

  public String generateBadge(String label, String value, Color backgroundValueColor) {
    int labelWidth = computeWidth(label);
    int valueWidth = computeWidth(value);

    Map<String, String> values = ImmutableMap.<String, String>builder()
      .put(PARAMETER_ICON_WIDTH_PLUS_MARGIN, valueOf(MARGIN + ICON_WIDTH))
      .put(PARAMETER_TOTAL_WIDTH, valueOf(MARGIN * 4 + ICON_WIDTH + labelWidth + valueWidth))
      .put(PARAMETER_LABEL_WIDTH, valueOf(MARGIN * 2 + ICON_WIDTH + labelWidth))
      .put(PARAMETER_LABEL_WIDTH_PLUS_MARGIN, valueOf( MARGIN * 3 + ICON_WIDTH + labelWidth))
      .put(PARAMETER_VALUE_WIDTH, valueOf(MARGIN * 2 + valueWidth))
      .put(PARAMETER_COLOR, backgroundValueColor.getValue())
      .put(PARAMETER_LABEL, label)
      .put(PARAMETER_VALUE, value)
      .build();
    StrSubstitutor strSubstitutor = new StrSubstitutor(values);
    return strSubstitutor.replace(badgeTemplate);
  }

  public String generateQualityGate(Metric.Level level){
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
    return (int) FONT.getStringBounds(text, FONT_RENDER_CONTEXT).getWidth();
  }

  private String readTemplate(String template) {
    try {
      return IOUtils.toString(getClass().getResource("templates/" + template), UTF_8);
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
