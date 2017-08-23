/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

package org.sonar.server.telemetry;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.core.config.TelemetryProperties;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.property.MapInternalProperties;
import org.sonar.updatecenter.common.Version;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.core.config.TelemetryProperties.PROP_ENABLE;
import static org.sonar.core.config.TelemetryProperties.PROP_FREQUENCY;
import static org.sonar.server.telemetry.TelemetryDaemon.I_PROP_LAST_PING;
import static org.sonar.test.JsonAssert.assertJson;

public class TelemetryDaemonTest {

  private static final long ONE_HOUR = 60 * 60 * 1_000L;
  private static final long ONE_DAY = 24 * ONE_HOUR;

  private TelemetryClient client = mock(TelemetryClient.class);
  private InternalProperties internalProperties = new MapInternalProperties();
  private FakeServer server = new FakeServer();
  private PluginRepository pluginRepository = mock(PluginRepository.class);
  private TestSystem2 system2 = new TestSystem2();
  private MapSettings settings;

  private TelemetryDaemon underTest;

  @Before
  public void setUp() throws Exception {
    settings = new MapSettings(new PropertyDefinitions(TelemetryProperties.all()));
    system2.setNow(System.currentTimeMillis());

    underTest = new TelemetryDaemon(client, settings.asConfig(), internalProperties, server, pluginRepository, system2);
  }

  @Test
  public void send_telemetry_data() throws IOException {
    settings.setProperty(PROP_FREQUENCY, "1");
    String id = "AU-TpxcB-iU5OvuD2FL7";
    String version = "7.5.4";
    server.setId(id);
    server.setVersion(version);
    List<PluginInfo> plugins = Arrays.asList(newPlugin("java", "4.12.0.11033"), newPlugin("scmgit", "1.2"), new PluginInfo("other"));
    when(pluginRepository.getPluginInfos()).thenReturn(plugins);
    underTest.start();

    ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
    verify(client, timeout(1_000).atLeastOnce()).upload(jsonCaptor.capture());
    String json = jsonCaptor.getValue();
    assertJson(json).isSimilarTo(getClass().getResource("telemetry-example.json"));
    assertJson(getClass().getResource("telemetry-example.json")).isSimilarTo(json);
  }

  @Test
  public void send_data_via_client_at_startup_after_initial_delay() throws IOException {
    settings.setProperty(PROP_FREQUENCY, "1");
    underTest.start();

    verify(client, timeout(1_000).atLeastOnce()).upload(anyString());
  }

  @Test
  public void check_if_should_send_data_periodically() throws IOException {
    long now = system2.now();
    long sixDaysAgo = now - (ONE_DAY * 6L);
    long sevenDaysAgo = now - (ONE_DAY * 7L);
    internalProperties.write(I_PROP_LAST_PING, String.valueOf(sixDaysAgo));
    settings.setProperty(PROP_FREQUENCY, "1");
    underTest.start();
    verify(client, timeout(1_000).never()).upload(anyString());
    internalProperties.write(I_PROP_LAST_PING, String.valueOf(sevenDaysAgo));

    verify(client, timeout(1_000).atLeastOnce()).upload(anyString());
  }

  @Test
  public void send_server_id_and_version() throws IOException {
    settings.setProperty(PROP_FREQUENCY, "1");
    String id = randomAlphanumeric(40);
    String version = randomAlphanumeric(10);
    server.setId(id);
    server.setVersion(version);
    underTest.start();

    ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
    verify(client, timeout(1_000).atLeastOnce()).upload(json.capture());
    assertThat(json.getValue()).contains(id, version);
  }

  @Test
  public void do_not_send_data_if_last_ping_earlier_than_one_week_ago() throws IOException {
    settings.setProperty(PROP_FREQUENCY, "1");
    long now = system2.now();
    long sixDaysAgo = now - (ONE_DAY * 6L);

    internalProperties.write(I_PROP_LAST_PING, String.valueOf(sixDaysAgo));
    underTest.start();

    verify(client, timeout(2_000).never()).upload(anyString());
  }

  @Test
  public void send_data_if_last_ping_is_one_week_ago() throws IOException {
    settings.setProperty(PROP_FREQUENCY, "1");
    long today = parseDate("2017-08-01").getTime();
    system2.setNow(today + 15 * ONE_HOUR);
    long now = system2.now();
    long sevenDaysAgo = now - (ONE_DAY * 7L);
    internalProperties.write(I_PROP_LAST_PING, String.valueOf(sevenDaysAgo));

    underTest.start();

    verify(client, timeout(1_000).atLeastOnce()).upload(anyString());
    assertThat(internalProperties.read(I_PROP_LAST_PING).get()).isEqualTo(String.valueOf(today));
  }

  @Test
  public void opt_out_sent_once() throws IOException {
    settings.setProperty(PROP_FREQUENCY, "1");
    settings.setProperty(PROP_ENABLE, "false");
    underTest.start();
    underTest.start();


    verify(client, timeout(1_000).never()).upload(anyString());
    verify(client, timeout(1_000).times(1)).optOut(anyString());
  }

  private PluginInfo newPlugin(String key, String version) {
    return new PluginInfo(key)
      .setVersion(Version.create(version));
  }
}
