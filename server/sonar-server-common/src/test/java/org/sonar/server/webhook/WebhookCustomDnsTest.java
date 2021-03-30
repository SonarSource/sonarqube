/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.config.Configuration;

import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.SONAR_VALIDATE_WEBHOOKS;

public class WebhookCustomDnsTest {

  private Configuration configuration = Mockito.mock(Configuration.class);
  private WebhookCustomDns underTest = new WebhookCustomDns(configuration);

  @Test
  public void lookup_fail_on_localhost() {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS.getKey())).thenReturn(Optional.of(true));

    Assertions.assertThatThrownBy(() -> underTest.lookup("localhost"))
      .hasMessageContaining("")
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void lookup_fail_on_127_0_0_1() {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS.getKey())).thenReturn(Optional.of(true));

    Assertions.assertThatThrownBy(() -> underTest.lookup("127.0.0.1"))
      .hasMessageContaining("")
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void lookup_dont_fail_on_localhost_if_validation_disabled() throws UnknownHostException {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS.getKey())).thenReturn(Optional.of(false));

    Assertions.assertThat(underTest.lookup("localhost"))
      .extracting(InetAddress::toString)
      .containsExactlyInAnyOrder("localhost/127.0.0.1");
  }

  @Test
  public void lookup_dont_fail_on_classic_host_with_validation_enabled() throws UnknownHostException {
    when(configuration.getBoolean(SONAR_VALIDATE_WEBHOOKS.getKey())).thenReturn(Optional.of(true));

    Assertions.assertThat(underTest.lookup("sonarsource.com").toString()).contains("sonarsource.com/");
  }
}
