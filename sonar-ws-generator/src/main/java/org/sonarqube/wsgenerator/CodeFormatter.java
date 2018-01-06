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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonarqube.wsgenerator.Helper.PATH_EXCLUSIONS;

public class CodeFormatter {

  public static void format(String json) {
    JsonObject jsonElement = new Gson().fromJson(json, JsonObject.class);
    JsonArray webServices = (JsonArray) jsonElement.get("webServices");

    Helper helper = new Helper();

    VelocityContext globalContext = new VelocityContext();
    globalContext.put("webServices", webServices);
    globalContext.put("helper", helper);
    String defaultWsClientCode = applyTemplate("defaultWsClient.vm", globalContext);
    writeSourceFile(helper.defaultWsClientFile(), defaultWsClientCode);
    String wsClientCode = applyTemplate("wsClient.vm", globalContext);
    writeSourceFile(helper.wsClientFile(), wsClientCode);
    writeSourceFile(helper.packageInfoFile(), applyTemplate("package-info.vm", globalContext));

    for (JsonElement webServiceElement : webServices) {
      JsonObject webService = (JsonObject) webServiceElement;
      String webServicePath = webService.get("path").getAsString();

      if (PATH_EXCLUSIONS.contains(webServicePath)) {
        System.out.println("Excluding WS " + webServicePath + " from code generation");
        continue;
      }

      VelocityContext webServiceContext = new VelocityContext();
      webServiceContext.put("webService", webServiceElement);
      webServiceContext.put("helper", helper);

      String webServiceCode = applyTemplate("webService.vm", webServiceContext);
      writeSourceFile(helper.file(webServicePath), webServiceCode);
      writeSourceFile(helper.packageInfoFile(webServicePath), applyTemplate("package-info.vm", webServiceContext));

      for (JsonElement actionElement : (JsonArray) webService.get("actions")) {
        JsonObject action = (JsonObject) actionElement;

        JsonArray params = (JsonArray) action.get("params");
        if (params == null || params.size() < 1) {
          continue;
        }

        VelocityContext actionContext = new VelocityContext();
        actionContext.put("webService", webServiceElement);
        actionContext.put("action", actionElement);
        actionContext.put("helper", helper);

        String requestCode = applyTemplate("request.vm", actionContext);
        writeSourceFile(helper.requestFile(webServicePath, action.get("key").getAsString()), requestCode);
      }
    }
  }

  private static void writeSourceFile(String file, String code) {
    try {
      FileUtils.writeStringToFile(new File(file), code, UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String applyTemplate(String templateName, VelocityContext context) {
    VelocityEngine velocity = new VelocityEngine();
    Properties properties = new Properties();
    properties.setProperty("resource.loader", "class");
    properties.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    velocity.init(properties);

    Writer writer = new StringWriter();
    velocity.mergeTemplate(templateName, "UTF-8", context, writer);
    try {
      writer.flush();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return writer.toString();
  }

  public static void main(String[] args) {
    String json = readFromInputStream(Generator.class.getResourceAsStream("/snapshot-of-api.json"));
    format(json);
  }

  private static String readFromInputStream(InputStream inputStream) {
    StringBuilder resultStringBuilder = new StringBuilder();
    try {
      try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
        String line;
        while ((line = br.readLine()) != null) {
          resultStringBuilder.append(line).append("\n");
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return resultStringBuilder.toString();
  }
}
