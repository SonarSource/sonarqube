/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.auth.saml;

import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.json.JSONObject;

public class SamlAuthStatusPageGenerator {

  private static final String WEB_CONTEXT = "%WEB_CONTEXT%";
  private static final String STATUS = "%STATUS%";
  private static final String ERRORS = "%ERRORS%";
  private static final String WARNINGS = "%WARNINGS%";
  private static final String AVAILABLE_ATTRIBUTES = "%AVAILABLE_ATTRIBUTES%";
  private static final String ATTRIBUTE_MAPPINGS = "%ATTRIBUTE_MAPPINGS%";

  private static final String HTML_TEMPLATE_NAME = "samlAuthResult.html";

  private SamlAuthStatusPageGenerator() {
    throw new IllegalStateException("This Utility class cannot be instantiated");
  }

  public static String getSamlAuthStatusHtml(SamlAuthenticationStatus samlAuthenticationStatus) {
    Map<String, String> substitutionsMap = getSubstitutionsMap(samlAuthenticationStatus);
    String htmlTemplate = getPlainTemplate();

    return substitutionsMap
      .keySet()
      .stream()
      .reduce(htmlTemplate, (accumulator, pattern) -> accumulator.replace(pattern, substitutionsMap.get(pattern)));
  }

  private static Map<String, String> getSubstitutionsMap(SamlAuthenticationStatus samlAuthenticationStatus) {
    return Map.of(
      WEB_CONTEXT, "",
      STATUS, toJsString(samlAuthenticationStatus.getStatus()),
      ERRORS, toJsArrayFromList(samlAuthenticationStatus.getErrors()),
      WARNINGS, toJsArrayFromList(samlAuthenticationStatus.getWarnings()),
      AVAILABLE_ATTRIBUTES, new JSONObject(samlAuthenticationStatus.getAvailableAttributes()).toString(),
      ATTRIBUTE_MAPPINGS, new JSONObject(samlAuthenticationStatus.getMappedAttributes()).toString());
  }

  private static String toJsString(@Nullable String inputString) {
    return String.format("'%s'", inputString != null ? inputString.replace("'", "\\'") : "");
  }

  private static String toJsArrayFromList(List<String> inputArray) {
    return "[" + inputArray.stream()
      .map(SamlAuthStatusPageGenerator::toJsString)
      .collect(Collectors.joining(",")) + "]";
  }

  private static String getPlainTemplate() {
    URL url = Resources.getResource(HTML_TEMPLATE_NAME);
    try {
      return Resources.toString(url, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read the template " + HTML_TEMPLATE_NAME);
    }
  }

}
