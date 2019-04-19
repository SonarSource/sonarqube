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
package org.sonar.server.webhook;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WebhookTest {

  @Test
  public void webhook_with_only_required_fields() {
    Webhook underTest = new Webhook("a_uuid", "a_component_uuid", null, null, "a_name", "an_url", null);

    assertThat(underTest.getUuid()).isEqualTo("a_uuid");
    assertThat(underTest.getComponentUuid()).isEqualTo("a_component_uuid");
    assertThat(underTest.getCeTaskUuid()).isEmpty();
    assertThat(underTest.getAnalysisUuid()).isEmpty();
    assertThat(underTest.getName()).isEqualTo("a_name");
    assertThat(underTest.getUrl()).isEqualTo("an_url");
    assertThat(underTest.getSecret()).isEmpty();
  }

  @Test
  public void webhook_with_all_fields() {
    Webhook underTest = new Webhook("a_uuid", "a_component_uuid", "a_task_uuid", "an_analysis", "a_name", "an_url", "a_secret");

    assertThat(underTest.getUuid()).isEqualTo("a_uuid");
    assertThat(underTest.getComponentUuid()).isEqualTo("a_component_uuid");
    assertThat(underTest.getCeTaskUuid()).hasValue("a_task_uuid");
    assertThat(underTest.getAnalysisUuid()).hasValue("an_analysis");
    assertThat(underTest.getName()).isEqualTo("a_name");
    assertThat(underTest.getUrl()).isEqualTo("an_url");
    assertThat(underTest.getSecret()).hasValue("a_secret");
  }
}
