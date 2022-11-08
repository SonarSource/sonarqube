/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.core.sarif.Sarif210;
import org.sonar.test.JsonAssert;

import static java.util.Objects.requireNonNull;

public class Sarif210SerializationDeserializationTest {

  private static final String VALID_SARIF_210_FILE_JSON = "valid-sarif210.json";

  @Test
  public void verify_json_serialization_of_sarif210() throws IOException {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    String expectedJson = IOUtils.toString(requireNonNull(getClass().getResource(VALID_SARIF_210_FILE_JSON)), StandardCharsets.UTF_8);
    Sarif210 deserializedJson = gson.fromJson(expectedJson, Sarif210.class);
    String reserializedJson = gson.toJson(deserializedJson);

    JsonAssert.assertJson(reserializedJson).isSimilarTo(expectedJson);
  }

}
