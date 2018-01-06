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
package org.sonarqube.wsgenerator;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonPrimitive;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class Helper {

  static final Set<String> PATH_EXCLUSIONS = new HashSet<>(asList("api/orchestrator"));
  private static final String OUTPUT_DIR = "target/generated-sources/results";
  private final Map<String, List<String[]>> responseTypes;

  public Helper() {
    InputStream inputStream = Helper.class.getResourceAsStream("/responseClasses.config");
    responseTypes = new BufferedReader(new InputStreamReader(inputStream))
      .lines()
      .map(line -> line.split("\\s+"))
      .collect(Collectors.groupingBy(arr -> arr[0]));
  }

  public boolean isIncluded(String path) {
    return !PATH_EXCLUSIONS.contains(path);
  }

  public String packageName() {
    return "org.sonarqube.ws.client";
  }

  public String packageName(String path) {
    return packageName() + "." + rawName(path).toLowerCase();
  }

  private String rawName(String path) {
    String x = path.replaceFirst("^api\\/", "");
    if (x.contains("_")) {
      return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, x.toLowerCase());
    }
    return capitalizeFirstLetter(x);
  }

  public String className(String path) {
    String name = rawName(path);
    return capitalizeFirstLetter(name) + "Service";
  }
  public String defaultWsClientFieldName(String path) {
    String name = rawName(path);
    return lowercaseFirstLetter(name) + "Service";
  }

  public String defaultWsClientMethodName(String path) {
    String name = rawName(path);
    return lowercaseFirstLetter(name);
  }

  public String webserviceTypeImport(String path) {
    return "import " + packageName(path) + "." + className(path) + ";";
  }

  private String capitalizeFirstLetter(String name) {
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }

  private String lowercaseFirstLetter(String name) {
    return name.substring(0, 1).toLowerCase() + name.substring(1);
  }

  public String responseType(String path, String action) {
    return responseTypeFullyQualified(path, action).replaceFirst("^.*\\.", "");
  }

  private String responseTypeFullyQualified(String path, String action) {
    String fullPath = path + "/" + action;
    List<String[]> responseTypesConfig = responseTypes.get(fullPath);
    String fullyQualified;
    if (responseTypesConfig == null) {
      fullyQualified = guessResponseType(path, action);
      responseTypes.put(fullPath, Collections.singletonList(new String[] {fullPath, fullyQualified}));
    } else {
      fullyQualified = responseTypesConfig.get(0)[1];
    }
    return fullyQualified;
  }

  private String guessResponseType(String path, String action) {
    return guessResponseOuterClassName(path).flatMap(
      potentialClassName -> guessResponseInnerClassName(action).flatMap(potentialInnerClassName -> {
        try {
          String guess = "org.sonarqube.ws." + potentialClassName + "$" + potentialInnerClassName;
          Helper.class.forName(guess);
          return Stream.of(guess.replaceFirst("\\$", "."));
        } catch (ClassNotFoundException e) {
        }
        return Stream.empty();
      })).findFirst().orElseGet(() -> {
        return "String";
      });
  }

  private Stream<String> guessResponseInnerClassName(String action) {
    return Stream.of(
      rawName(action) + "Response",
      rawName(action) + "WsResponse",
      "Ws" + rawName(action) + "Response");
  }

  private Stream<String> guessResponseOuterClassName(String path) {
    return Stream.of(
      rawName(path),
      "Ws" + rawName(path),
      rawName(path) + "Ws");
  }

  public String responseTypeImport(String path, String action) {
    String fullyQualified = responseTypeFullyQualified(path, action);
    if ("String".equals(fullyQualified)) {
      return null;
    }
    return "import " + fullyQualified + ";";
  }

  public String methodName(String path, String action) {
    return lowercaseFirstLetter(rawName(action));
  }

  public String requestType(String path, String action) {
    return rawName(action) + "Request";
  }

  public String parameterGetter(String parameter) {
    return "get" + rawName(parameter);
  }

  public String parameterSetter(String parameter) {
    return "set" + rawName(parameter);
  }

  public String setterParameter(String parameter) {
    return lowercaseFirstLetter(rawName(parameter));
  }

  public String setterParameterType(String parameter, JsonPrimitive parameterDescription) {
    if (parameter.equals("values") || parameter.equals("fieldValues") || parameter.equals("keys")) {
      return "List<String>";
    }
    if (parameterDescription != null && parameterDescription.getAsString().matches(".*[Cc]omma.?separated.*|.*[Ll]ist of.*")) {
      return "List<String>";
    }
    return "String";
  }

  public String apiDocUrl(String path) {
    return "https://next.sonarqube.com/sonarqube/web_api/" + path;
  }

  public String apiDocUrl(String path, String action) {
    return apiDocUrl(path) + "/" + action;
  }

  public String file(String path) {
    return OUTPUT_DIR + "/org/sonarqube/ws/client/" + rawName(path).toLowerCase() + "/" + className(path) + ".java";
  }

  public String defaultWsClientFile() {
    return OUTPUT_DIR + "/org/sonarqube/ws/client/DefaultWsClient.java";
  }

  public String wsClientFile() {
    return OUTPUT_DIR + "/org/sonarqube/ws/client/WsClient.java";
  }

  public String packageInfoFile() {
    return OUTPUT_DIR + "/org/sonarqube/ws/client/package-info.java";
  }

  public String packageInfoFile(String path) {
    return OUTPUT_DIR + "/org/sonarqube/ws/client/" + rawName(path).toLowerCase() + "/package-info.java";
  }

  public String requestFile(String path, String action) {
    return OUTPUT_DIR + "/org/sonarqube/ws/client/" + rawName(path).toLowerCase() + "/" + requestType(path, action) + ".java";
  }
}
