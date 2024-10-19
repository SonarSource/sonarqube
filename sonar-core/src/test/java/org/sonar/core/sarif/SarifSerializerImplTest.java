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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.sarif.pojo.ArtifactLocation;
import org.sonar.sarif.pojo.Location;
import org.sonar.sarif.pojo.Message;
import org.sonar.sarif.pojo.MultiformatMessageString;
import org.sonar.sarif.pojo.PhysicalLocation;
import org.sonar.sarif.pojo.Region;
import org.sonar.sarif.pojo.ReportingDescriptor;
import org.sonar.sarif.pojo.Result;
import org.sonar.sarif.pojo.Run;
import org.sonar.sarif.pojo.SarifSchema210;
import org.sonar.sarif.pojo.Tool;
import org.sonar.sarif.pojo.ToolComponent;
import org.sonar.test.JsonAssert;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.core.sarif.SarifSerializerImpl.UNSUPPORTED_VERSION_MESSAGE_TEMPLATE;

@RunWith(MockitoJUnitRunner.class)
public class SarifSerializerImplTest {

  private static final String SARIF_JSON = "{\"version\":\"2.1.0\",\"$schema\":\"http://json.schemastore.org/sarif-2.1.0-rtm.4\",\"runs\":[{\"results\":[]}]}";

  private final SarifSerializerImpl serializer = new SarifSerializerImpl();

  @Test
  public void serialize() {
    new Run().withResults(List.of());
    SarifSchema210 sarif210 = new SarifSchema210()
      .with$schema(URI.create("http://json.schemastore.org/sarif-2.1.0-rtm.4"))
      .withVersion(SarifSchema210.Version._2_1_0)
      .withRuns(List.of(new Run().withResults(List.of())));

    String result = serializer.serialize(sarif210);

    JsonAssert.assertJson(result).isSimilarTo(SARIF_JSON);
  }

  @Test
  public void deserialize() throws URISyntaxException {
    URL sarifResource = requireNonNull(getClass().getResource("eslint-sarif210.json"));
    Path sarif = Paths.get(sarifResource.toURI());

    SarifSchema210 deserializationResult = serializer.deserialize(sarif);

    verifySarif(deserializationResult);
  }

  @Test
  public void deserialize_shouldFail_whenFileCantBeFound() {
    String file = "wrongPathToFile";
    Path sarif = Paths.get(file);

    assertThatThrownBy(() -> serializer.deserialize(sarif))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to read SARIF report at 'wrongPathToFile'");
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

  private void verifySarif(SarifSchema210 deserializationResult) {
    SarifSchema210 expected = buildExpectedSarif210();

    assertThat(deserializationResult).isNotNull();
    assertThat(deserializationResult).usingRecursiveComparison().ignoringFields("runs").isEqualTo(expected);

    Run run = getRun(deserializationResult);
    Run expectedRun = getRun(expected);
    assertThat(run).usingRecursiveComparison().ignoringFields("results", "tool.driver.rules", "tool.driver.informationUri", "artifacts").isEqualTo(expectedRun);

    Result result = getResult(run);
    Result expectedResult = getResult(expectedRun);
    assertThat(result).usingRecursiveComparison().isEqualTo(expectedResult);

    ReportingDescriptor rule = getRule(run);
    ReportingDescriptor expectedRule = getRule(expectedRun);
    assertThat(rule).usingRecursiveComparison().ignoringFields("properties").isEqualTo(expectedRule);
  }

  private static SarifSchema210 buildExpectedSarif210() {
    return new SarifSchema210()
      .with$schema(URI.create("http://json.schemastore.org/sarif-2.1.0-rtm.4"))
      .withVersion(SarifSchema210.Version._2_1_0)
      .withRuns(List.of(buildExpectedRun()));
  }

  private static Run buildExpectedRun() {
    Tool tool = new Tool().withDriver(buildExpectedDriver());
    return new Run().withTool(tool).withResults(List.of(buildExpectedResult()));
  }

  private static ToolComponent buildExpectedDriver() {
    return new ToolComponent()
      .withName("ESLint")
      .withRules(Set.of(buildExpectedRule()));
  }

  private static ReportingDescriptor buildExpectedRule() {
    return new ReportingDescriptor()
      .withId("no-unused-vars")
      .withShortDescription(new MultiformatMessageString().withText("disallow unused variables"))
      .withHelpUri(URI.create("https://eslint.org/docs/rules/no-unused-vars"));
  }

  private static Result buildExpectedResult() {
    return new Result()
      .withRuleId("no-unused-vars")
      .withRuleIndex(0)
      .withLocations(List.of(buildExpectedLocation()))
      .withMessage(new Message().withText("'x' is assigned a value but never used."))
      .withLevel(Result.Level.ERROR);
  }

  private static Location buildExpectedLocation() {
    ArtifactLocation artifactLocation = new ArtifactLocation().withUri("file:///C:/dev/sarif/sarif-tutorials/samples/Introduction/simple-example.js").withIndex(0);
    PhysicalLocation physicalLocation = new PhysicalLocation().withArtifactLocation(artifactLocation).withRegion(buildExpectedRegion());
    return new Location().withPhysicalLocation(physicalLocation);
  }

  private static Region buildExpectedRegion() {
    return new Region()
      .withStartLine(1)
      .withStartColumn(5);
  }

  private static Run getRun(SarifSchema210 sarif210) {
    return sarif210.getRuns().stream().findFirst().orElseGet(() -> fail("runs property is missing"));
  }

  private static Result getResult(Run run) {
    return run.getResults().stream().findFirst().orElseGet(() -> fail("results property is missing"));
  }

  private static ReportingDescriptor getRule(Run run) {
    return run.getTool().getDriver().getRules().stream().findFirst().orElseGet(() -> fail("rules property is missing"));
  }

}
