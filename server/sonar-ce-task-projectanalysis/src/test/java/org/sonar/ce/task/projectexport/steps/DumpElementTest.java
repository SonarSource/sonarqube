/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectexport.steps;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.ce.task.projectexport.steps.DumpElement.ANALYSES;
import static org.sonar.ce.task.projectexport.steps.DumpElement.COMPONENTS;
import static org.sonar.ce.task.projectexport.steps.DumpElement.EVENTS;
import static org.sonar.ce.task.projectexport.steps.DumpElement.ISSUES;
import static org.sonar.ce.task.projectexport.steps.DumpElement.ISSUES_CHANGELOG;
import static org.sonar.ce.task.projectexport.steps.DumpElement.LINKS;
import static org.sonar.ce.task.projectexport.steps.DumpElement.MEASURES;
import static org.sonar.ce.task.projectexport.steps.DumpElement.METADATA;
import static org.sonar.ce.task.projectexport.steps.DumpElement.METRICS;
import static org.sonar.ce.task.projectexport.steps.DumpElement.PLUGINS;
import static org.sonar.ce.task.projectexport.steps.DumpElement.RULES;
import static org.sonar.ce.task.projectexport.steps.DumpElement.SETTINGS;

public class DumpElementTest {

  @Test
  public void test_filename() {
    assertThat(METADATA.filename()).isEqualTo("metadata.pb");
    assertThat(COMPONENTS.filename()).isEqualTo("components.pb");
    assertThat(MEASURES.filename()).isEqualTo("measures.pb");
    assertThat(METRICS.filename()).isEqualTo("metrics.pb");
    assertThat(ISSUES.filename()).isEqualTo("issues.pb");
    assertThat(ISSUES_CHANGELOG.filename()).isEqualTo("issues_changelog.pb");
    assertThat(RULES.filename()).isEqualTo("rules.pb");
    assertThat(ANALYSES.filename()).isEqualTo("analyses.pb");
    assertThat(SETTINGS.filename()).isEqualTo("settings.pb");
    assertThat(LINKS.filename()).isEqualTo("links.pb");
    assertThat(EVENTS.filename()).isEqualTo("events.pb");
    assertThat(PLUGINS.filename()).isEqualTo("plugins.pb");
  }

  @Test
  public void test_parser() {
    assertThat(METADATA.parser()).isSameAs(ProjectDump.Metadata.parser());
  }
}
