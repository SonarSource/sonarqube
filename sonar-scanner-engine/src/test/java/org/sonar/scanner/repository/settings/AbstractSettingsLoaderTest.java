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
package org.sonar.scanner.repository.settings;

import org.junit.Test;
import org.sonarqube.ws.Settings.FieldValues;
import org.sonarqube.ws.Settings.FieldValues.Value;
import org.sonarqube.ws.Settings.FieldValues.Value.Builder;
import org.sonarqube.ws.Settings.Setting;
import org.sonarqube.ws.Settings.Values;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class AbstractSettingsLoaderTest {

  @Test
  public void should_load_global_multivalue_settings() {
    assertThat(AbstractSettingsLoader.toMap(singletonList(Setting.newBuilder()
      .setKey("sonar.preview.supportedPlugins")
      .setValues(Values.newBuilder().addValues("java").addValues("php")).build())))
        .containsExactly(entry("sonar.preview.supportedPlugins", "java,php"));
  }

  @Test
  public void should_escape_global_multivalue_settings() {
    assertThat(AbstractSettingsLoader.toMap(singletonList(Setting.newBuilder()
      .setKey("sonar.preview.supportedPlugins")
      .setValues(Values.newBuilder().addValues("ja,va").addValues("p\"hp")).build())))
        .containsExactly(entry("sonar.preview.supportedPlugins", "\"ja,va\",\"p\"\"hp\""));
  }

  @Test
  public void should_load_global_propertyset_settings() {
    Builder valuesBuilder = Value.newBuilder();
    valuesBuilder.putValue("filepattern", "**/*.xml");
    valuesBuilder.putValue("rulepattern", "*:S12345");
    Value value1 = valuesBuilder.build();
    valuesBuilder.clear();
    valuesBuilder.putValue("filepattern", "**/*.java");
    valuesBuilder.putValue("rulepattern", "*:S456");
    Value value2 = valuesBuilder.build();

    assertThat(AbstractSettingsLoader.toMap(singletonList(Setting.newBuilder()
      .setKey("sonar.issue.exclusions.multicriteria")
      .setFieldValues(FieldValues.newBuilder().addFieldValues(value1).addFieldValues(value2)).build())))
        .containsOnly(entry("sonar.issue.exclusions.multicriteria", "1,2"),
          entry("sonar.issue.exclusions.multicriteria.1.filepattern", "**/*.xml"),
          entry("sonar.issue.exclusions.multicriteria.1.rulepattern", "*:S12345"),
          entry("sonar.issue.exclusions.multicriteria.2.filepattern", "**/*.java"),
          entry("sonar.issue.exclusions.multicriteria.2.rulepattern", "*:S456"));
  }

}
