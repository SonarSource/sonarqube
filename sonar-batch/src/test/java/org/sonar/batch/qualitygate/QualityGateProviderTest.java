/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.api.utils.HttpDownloader;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class QualityGateProviderTest {

  @Mock
  private Settings settings;

  @Mock
  private ServerClient client;

  @Mock
  private MetricFinder metricFinder;

  @Mock
  private Logger logger;

  @Test
  public void should_load_empty_quality_gate_from_default_settings() {
    QualityGateProvider provider = new QualityGateProvider();
    assertThat(provider.provide(settings, client, metricFinder).conditions()).isEmpty();
    assertThat(provider.init(settings, client, metricFinder, logger).isEnabled()).isFalse();
    verify(logger, times(1)).info("No quality gate is configured.");
    verify(settings, times(2)).getString("sonar.qualitygate");
  }

  @Test
  public void should_load_empty_quality_gate_using_name() {
    String qGateName = "Sonar way";
    when(settings.getString("sonar.qualitygate")).thenReturn(qGateName);
    when(client.request("/api/qualitygates/show?name=Sonar way", false)).thenReturn("{'id':12345,'name':'Sonar way'}");
    QualityGate qGate = new QualityGateProvider().init(settings, client, metricFinder, logger);
    assertThat(qGate.name()).isEqualTo(qGateName);
    assertThat(qGate.isEnabled()).isTrue();
    assertThat(qGate.conditions()).isEmpty();
    verify(logger).info("Loaded quality gate '{}'", qGateName);
  }

  @Test
  public void should_load_quality_gate_using_id() {
    long qGateId = 12345L;
    String qGateName = "Sonar way";
    when(settings.getString("sonar.qualitygate")).thenReturn(Long.toString(qGateId));
    when(client.request("/api/qualitygates/show?id=12345", false)).thenReturn("{'id':12345,'name':'Sonar way','conditions':["
      + "{'id':1,'metric':'metric1','op':'EQ','warning':'POLOP'},"
      + "{'id':2,'metric':'metric2','op':'NE','error':'PALAP','period':3}"
      + "]}");

    QualityGate qGate = new QualityGateProvider().init(settings, client, metricFinder, logger);

    assertThat(qGate.name()).isEqualTo(qGateName);
    assertThat(qGate.conditions()).hasSize(2);
    Iterator<ResolvedCondition> conditions = qGate.conditions().iterator();
    ResolvedCondition cond1 = conditions.next();
    assertThat(cond1.warningThreshold()).isEqualTo("POLOP");
    assertThat(cond1.errorThreshold()).isNull();
    assertThat(cond1.period()).isNull();
    ResolvedCondition cond2 = conditions.next();
    assertThat(cond2.warningThreshold()).isNull();
    assertThat(cond2.errorThreshold()).isEqualTo("PALAP");
    assertThat(cond2.period()).isEqualTo(3);

    verify(logger).info("Loaded quality gate '{}'", qGateName);
    verify(metricFinder).findByKey("metric1");
    verify(metricFinder).findByKey("metric2");
  }

  @Test(expected = MessageException.class)
  public void should_stop_analysis_if_gate_not_found() {
    String qGateName = "Sonar way";
    when(settings.getString("sonar.qualitygate")).thenReturn(qGateName);
    when(client.request("/api/qualitygates/show?name=Sonar way", false)).thenThrow(
        new HttpDownloader.HttpException(URI.create("/api/qualitygates/show?name=Sonar%20way"), HttpURLConnection.HTTP_NOT_FOUND));
    new QualityGateProvider().provide(settings, client, metricFinder);
  }

  @Test(expected = HttpDownloader.HttpException.class)
  public void should_stop_analysis_if_server_error() {
    String qGateName = "Sonar way";
    when(settings.getString("sonar.qualitygate")).thenReturn(qGateName);
    when(client.request("/api/qualitygates/show?name=Sonar way", false)).thenThrow(
        new HttpDownloader.HttpException(URI.create("/api/qualitygates/show?name=Sonar%20way"), HttpURLConnection.HTTP_NOT_ACCEPTABLE));
    new QualityGateProvider().provide(settings, client, metricFinder);
  }

}
