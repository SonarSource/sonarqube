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

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class WebhookTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void constructor_with_null_componentUuid_should_throw_NPE() {
    expectedException.expect(NullPointerException.class);

    new Webhook(randomAlphanumeric(40), null, null, null, randomAlphanumeric(10), randomAlphanumeric(10));
  }

  @Test
  public void constructor_with_null_name_should_throw_NPE() {
    expectedException.expect(NullPointerException.class);

    new Webhook(randomAlphanumeric(40), randomAlphanumeric(10), null, null, null, randomAlphanumeric(10));
  }

  @Test
  public void constructor_with_null_url_should_throw_NPE() {
    expectedException.expect(NullPointerException.class);

    new Webhook(randomAlphanumeric(40), randomAlphanumeric(10), null, null, randomAlphanumeric(10), null);
  }

  @Test
  public void constructor_with_null_ceTaskUuid_or_analysisUuidurl_should_return_Optional_empty() {
    String componentUuid = randomAlphanumeric(10);
    String name = randomAlphanumeric(10);
    String url = randomAlphanumeric(10);
    Webhook underTest = new Webhook(randomAlphanumeric(40), componentUuid, null, null, name, url);

    assertThat(underTest.getComponentUuid()).isEqualTo(componentUuid);
    assertThat(underTest.getName()).isEqualTo(name);
    assertThat(underTest.getUrl()).isEqualTo(url);
    assertThat(underTest.getCeTaskUuid()).isEqualTo(Optional.empty());
    assertThat(underTest.getAnalysisUuid()).isEqualTo(Optional.empty());

    String ceTaskUuid = randomAlphanumeric(10);
    String analysisUuid = randomAlphanumeric(10);
    underTest = new Webhook(randomAlphanumeric(40), componentUuid, ceTaskUuid, analysisUuid, name, url);
    assertThat(underTest.getComponentUuid()).isEqualTo(componentUuid);
    assertThat(underTest.getName()).isEqualTo(name);
    assertThat(underTest.getUrl()).isEqualTo(url);
    assertThat(underTest.getCeTaskUuid().get()).isEqualTo(ceTaskUuid);
    assertThat(underTest.getAnalysisUuid().get()).isEqualTo(analysisUuid);
  }
}
