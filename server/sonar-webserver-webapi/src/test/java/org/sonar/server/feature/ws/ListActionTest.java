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
package org.sonar.server.feature.ws;

import java.util.List;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.feature.SonarQubeFeature;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class ListActionTest {

  private static final List<SonarQubeFeature> EXAMPLE_FEATURES = getExampleFeatures();

  private final ListAction underTest = new ListAction(EXAMPLE_FEATURES);
  private final WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void define_hasNecessaryInformation() {
    WebService.Action def = tester.getDef();

    assertThat(def.since()).isEqualTo("9.6");
    assertTrue(def.isInternal());
    assertThat(def.description()).isNotEmpty();
    assertThat(def.responseExampleAsString()).isNotEmpty();
  }

  @Test
  public void handle_returnsValidJsonWithInfoAboutExampleFeatures() {
    TestResponse execute = tester.newRequest().execute();

    execute.assertJson(this.getClass(), "valid-json-with-2-features.json");
  }

  @Test
  public void handle_returnsEmptyJsonWhenNoFeaturesDefined() {
    ListAction actionWithNoFeatures = new ListAction(List.of());
    WsActionTester tester = new WsActionTester(actionWithNoFeatures);
    TestResponse execute = tester.newRequest().execute();

    execute.assertJson("[]");
  }

  private static List<SonarQubeFeature> getExampleFeatures() {
    ExampleSonarQubeFeature feature1 = new ExampleSonarQubeFeature("feature1", true);
    ExampleSonarQubeFeature feature2 = new ExampleSonarQubeFeature("feature2", true);
    ExampleSonarQubeFeature feature3 = new ExampleSonarQubeFeature("feature3", false);

    return List.of(feature1, feature2, feature3);
  }

  static class ExampleSonarQubeFeature implements SonarQubeFeature {

    private final String name;
    private final boolean enabled;

    public ExampleSonarQubeFeature(String name, boolean enabled) {
      this.name = name;
      this.enabled = enabled;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public boolean isEnabled() {
      return enabled;
    }
  }
}

