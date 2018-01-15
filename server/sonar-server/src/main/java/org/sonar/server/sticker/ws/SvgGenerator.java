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
package org.sonar.server.sticker.ws;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.lang.text.StrSubstitutor;
import org.sonar.api.server.ServerSide;

import static java.nio.charset.StandardCharsets.UTF_8;

@ServerSide
public class SvgGenerator {

  private static final FontRenderContext FONT_RENDER_CONTEXT = new FontRenderContext(new AffineTransform(), true, true);
  private static final Font FONT = new Font("Verdana", Font.PLAIN, 11);

  public enum Color {
    RED("#d4333f"), ORANGE("#ed7d20"), GREEN("#4c1");

    String value;

    Color(String hex) {
      this.value = hex;
    }

    String getValue() {
      return value;
    }
  }

  private static final int RIGHT_MARGIN = 4;
  private static final int LEFT_MARGIN = 4;

  private final String errorTemplate;
  private final String badgeTemplate;

  public SvgGenerator() {
    this.errorTemplate = readTemplate("error.svg");
    this.badgeTemplate = readTemplate("badge.svg");
  }

  public String generateBadge(String label, String value, Color valueColor) {
    int labelWidth = computeWidth(label);
    int valueWidth = computeWidth(value);
    Map<String, String> values = ImmutableMap.<String, String>builder()
      .put("totalWidth", Integer.toString(RIGHT_MARGIN * 2 + LEFT_MARGIN * 2 + labelWidth + valueWidth))
      .put("labelWidth", Integer.toString(RIGHT_MARGIN + labelWidth + LEFT_MARGIN))
      .put("LabelWidthPlusMargin", Integer.toString(RIGHT_MARGIN * 2 + labelWidth + LEFT_MARGIN))
      .put("valueWidth", Integer.toString(RIGHT_MARGIN + valueWidth + LEFT_MARGIN))
      .put("color", valueColor.getValue())
      .put("label", label)
      .put("value", value)
      .build();
    StrSubstitutor strSubstitutor = new StrSubstitutor(values);
    return strSubstitutor.replace(badgeTemplate);
  }

  public String generateError(String error) {
    Map<String, String> values = ImmutableMap.of(
      "totalWidth", Integer.toString(RIGHT_MARGIN + computeWidth(error) + LEFT_MARGIN),
      "label", error);
    StrSubstitutor strSubstitutor = new StrSubstitutor(values);
    return strSubstitutor.replace(errorTemplate);
  }

  private static String readTemplate(String template) {
    try {
      return Files.toString(new File(Resources.getResource(SvgGenerator.class, "templates/" + template).getFile()), UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Can't read svg template '%s'", template), e);
    }
  }

  private static int computeWidth(String text) {
    return (int) FONT.getStringBounds(text, FONT_RENDER_CONTEXT).getWidth();
  }

}
