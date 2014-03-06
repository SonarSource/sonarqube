/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.qualitygate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.qualitygate.QualityGateClient;
import org.sonar.wsclient.qualitygate.QualityGateDetails;

import java.net.HttpURLConnection;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QualityGateProviderTest {

  @Mock
  private Settings settings;

  @Mock
  private ServerClient client;

  @Mock
  private QualityGateClient qualityGateClient;

  @Mock
  private Logger logger;

  @Before
  public void initMocks() {
    SonarClient wsClient = mock(SonarClient.class);
    when(client.wsClient()).thenReturn(wsClient);
    when(wsClient.qualityGateClient()).thenReturn(qualityGateClient);
  }

  @Test
  public void should_load_empty_quality_gate_from_default_settings() {
    assertThat(new QualityGateProvider().provide(settings, client).conditions()).isEmpty();
    assertThat(new QualityGateProvider().init(settings, client, logger).isEnabled()).isFalse();
    verify(logger).info("No quality gate is configured.");
  }

  @Test
  public void should_load_quality_gate_using_name() {
    String qGateName = "Sonar way";
    when(settings.getString("sonar.qualitygate")).thenReturn(qGateName);
    QualityGateDetails qGate = mock(QualityGateDetails.class);
    when(qualityGateClient.show(qGateName)).thenReturn(qGate);
    when(qGate.name()).thenReturn(qGateName);
    QualityGate actualGate = new QualityGateProvider().init(settings, client, logger);
    assertThat(actualGate.name()).isEqualTo(qGateName);
    assertThat(actualGate.isEnabled()).isTrue();
    verify(logger).info("Loaded quality gate '{}'", qGateName);
  }

  @Test
  public void should_load_quality_gate_using_id() {
    long qGateId = 12345L;
    String qGateName = "Sonar way";
    when(settings.getString("sonar.qualitygate")).thenReturn(Long.toString(qGateId));
    QualityGateDetails qGate = mock(QualityGateDetails.class);
    when(qualityGateClient.show(qGateId)).thenReturn(qGate);
    when(qGate.name()).thenReturn(qGateName);
    assertThat(new QualityGateProvider().init(settings, client, logger).name()).isEqualTo(qGateName);
    verify(logger).info("Loaded quality gate '{}'", qGateName);
  }

  @Test(expected = MessageException.class)
  public void should_stop_analysis_if_gate_not_found() {
    String qGateName = "Sonar way";
    when(settings.getString("sonar.qualitygate")).thenReturn(qGateName);
    when(qualityGateClient.show(qGateName)).thenThrow(new HttpException("http://server/api/qualitygates/show?name=Sonar%20way", HttpURLConnection.HTTP_NOT_FOUND));
    new QualityGateProvider().provide(settings, client);
  }

  @Test(expected = HttpException.class)
  public void should_stop_analysis_if_server_error() {
    String qGateName = "Sonar way";
    when(settings.getString("sonar.qualitygate")).thenReturn(qGateName);
    when(qualityGateClient.show(qGateName)).thenThrow(new HttpException("http://server/api/qualitygates/show?name=Sonar%20way", HttpURLConnection.HTTP_NOT_ACCEPTABLE));
    new QualityGateProvider().provide(settings, client);
  }

}
