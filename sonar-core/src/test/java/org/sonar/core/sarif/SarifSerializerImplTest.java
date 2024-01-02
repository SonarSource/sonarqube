/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.core.sarif;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.core.sarif.SarifVersionValidator.UNSUPPORTED_VERSION_MESSAGE_TEMPLATE;

@RunWith(MockitoJUnitRunner.class)
public class SarifSerializerImplTest {

  private static final String SARIF_JSON = "{\"version\":\"2.1.0\",\"$schema\":\"http://json.schemastore.org/sarif-2.1.0-rtm.4\",\"runs\":[{\"results\":[]}]}";

  private final SarifSerializerImpl serializer = new SarifSerializerImpl();

  @Test
  public void serialize() {
    Run.builder().results(Set.of()).build();
    Sarif210 sarif210 = new Sarif210("http://json.schemastore.org/sarif-2.1.0-rtm.4", Run.builder().results(Set.of()).build());

    String result = serializer.serialize(sarif210);

    assertThat(result).isEqualTo(SARIF_JSON);
  }

  @Test
  public void deserialize() throws URISyntaxException, NoSuchFileException {
    URL sarifResource = requireNonNull(getClass().getResource("eslint-sarif210.json"));
    Path sarif = Paths.get(sarifResource.toURI());

    Sarif210 deserializationResult = serializer.deserialize(sarif);

    verifySarif(deserializationResult);
  }

  @Test
  public void deserialize_shouldFail_whenFileCantBeFound() {
    String file = "wrongPathToFile";
    Path sarif = Paths.get(file);

    assertThatThrownBy(() -> serializer.deserialize(sarif))
      .isInstanceOf(NoSuchFileException.class);
  }

  @Test
  public void deserialize_shouldFail_whenJsonSyntaxIsIncorrect() throws URISyntaxException {
    URL sarifResource = requireNonNull(getClass().getResource("invalid-json-syntax.json"));
    Path sarif = Paths.get(sarifResource.toURI());

    assertThatThrownBy(() -> serializer.deserialize(sarif))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(format("Failed to read SARIF report at '%s': invalid JSON syntax or file is not UTF-8 encoded", sarif));
  }

  @Test
  public void deserialize_whenFileIsNotUtf8encoded_shouldFail() throws URISyntaxException {
    URL sarifResource = requireNonNull(getClass().getResource("sarif210-nonUtf8.json"));
    Path sarif = Paths.get(sarifResource.toURI());

    assertThatThrownBy(() -> serializer.deserialize(sarif))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(format("Failed to read SARIF report at '%s': invalid JSON syntax or file is not UTF-8 encoded", sarif));
  }

  @Test
  public void deserialize_shouldFail_whenSarifVersionIsNotSupported() throws URISyntaxException {
    URL sarifResource = requireNonNull(getClass().getResource("unsupported-sarif-version-abc.json"));
    Path sarif = Paths.get(sarifResource.toURI());

    assertThatThrownBy(() -> serializer.deserialize(sarif))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(format(UNSUPPORTED_VERSION_MESSAGE_TEMPLATE, "A.B.C"));
  }

  private void verifySarif(Sarif210 deserializationResult) {
    Sarif210 expected = buildExpectedSarif210();

    assertThat(deserializationResult).isNotNull();
    assertThat(deserializationResult).usingRecursiveComparison().ignoringFields("runs").isEqualTo(expected);

    Run run = getRun(deserializationResult);
    Run expectedRun = getRun(expected);
    assertThat(run).usingRecursiveComparison().ignoringFields("results", "tool.driver.rules").isEqualTo(expectedRun);

    Result result = getResult(run);
    Result expectedResult = getResult(expectedRun);
    assertThat(result).usingRecursiveComparison().isEqualTo(expectedResult);

    Rule rule = getRule(run);
    Rule expectedRule = getRule(expectedRun);
    assertThat(rule).usingRecursiveComparison().ignoringFields("properties").isEqualTo(expectedRule);
  }

  private static Sarif210 buildExpectedSarif210() {
    return new Sarif210("http://json.schemastore.org/sarif-2.1.0-rtm.4", buildExpectedRun());
  }

  private static Run buildExpectedRun() {
    Tool tool = new Tool(buildExpectedDriver());
    return Run.builder()
      .tool(tool)
      .results(Set.of(buildExpectedResult())).build();
  }

  private static Driver buildExpectedDriver() {
    return Driver.builder()
      .name("ESLint")
      .rules(Set.of(buildExpectedRule()))
      .build();
  }

  private static Rule buildExpectedRule() {
    return Rule.builder()
      .id("no-unused-vars")
      .shortDescription("disallow unused variables")
      .build();
  }

  private static Result buildExpectedResult() {
    return Result.builder()
      .ruleId("no-unused-vars")
      .locations(Set.of(buildExpectedLocation()))
      .message("'x' is assigned a value but never used.")
      .level("error")
      .build();
  }

  private static Location buildExpectedLocation() {
    ArtifactLocation artifactLocation = new ArtifactLocation(null, "file:///C:/dev/sarif/sarif-tutorials/samples/Introduction/simple-example.js");
    PhysicalLocation physicalLocation = PhysicalLocation.of(artifactLocation, buildExpectedRegion());
    return Location.of(physicalLocation);
  }

  private static Region buildExpectedRegion() {
    return Region.builder()
      .startLine(1)
      .startColumn(5)
      .build();
  }

  private static Run getRun(Sarif210 sarif210) {
    return sarif210.getRuns().stream().findFirst().orElseGet(() -> fail("runs property is missing"));
  }

  private static Result getResult(Run run) {
    return run.getResults().stream().findFirst().orElseGet(() -> fail("results property is missing"));
  }

  private static Rule getRule(Run run) {
    return run.getTool().getDriver().getRules().stream().findFirst().orElseGet(() -> fail("rules property is missing"));
  }

}
