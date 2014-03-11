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

import com.google.common.annotations.VisibleForTesting;
import org.picocontainer.injectors.ProviderAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.qualitygate.QualityGateClient;
import org.sonar.wsclient.qualitygate.QualityGateCondition;
import org.sonar.wsclient.qualitygate.QualityGateDetails;

import java.net.HttpURLConnection;

public class QualityGateProvider extends ProviderAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(QualityGateProvider.class);

  private static final String PROPERTY_QUALITY_GATE = "sonar.qualitygate";

  private QualityGate instance;

  public QualityGate provide(Settings settings, ServerClient client, MetricFinder metricFinder) {
    if (instance == null) {
      instance = init(settings, client, metricFinder, LOG);
    }
    return instance;
  }

  @VisibleForTesting
  QualityGate init(Settings settings, ServerClient client, MetricFinder metricFinder, Logger logger) {
    QualityGate result = QualityGate.disabled();
    String qualityGateSetting = settings.getString(PROPERTY_QUALITY_GATE);
    if (qualityGateSetting == null) {
      logger.info("No quality gate is configured.");
    } else {
      result = load(qualityGateSetting, client.wsClient().qualityGateClient(), metricFinder);
      logger.info("Loaded quality gate '{}'", result.name());
    }
    return result;
  }

  private QualityGate load(String qualityGateSetting, QualityGateClient qualityGateClient, MetricFinder metricFinder) {
    QualityGateDetails definitionFromServer = null;
    try {
      definitionFromServer = fetch(qualityGateSetting, qualityGateClient);
    } catch (HttpException serverError) {
      if (serverError.status() == HttpURLConnection.HTTP_NOT_FOUND) {
        throw MessageException.of("No quality gate found with configured value '" + qualityGateSetting + "'. Please check your configuration.");
      } else {
        throw serverError;
      }
    }

    QualityGate configuredGate = new QualityGate(definitionFromServer.name());

    for (QualityGateCondition condition: definitionFromServer.conditions()) {
      configuredGate.add(new ResolvedCondition(condition, metricFinder.findByKey(condition.metricKey())));
    }

    return configuredGate;
  }

  private QualityGateDetails fetch(String qualityGateSetting, QualityGateClient qualityGateClient) {
    QualityGateDetails definitionFromServer = null;
    try {
      long qGateId = Long.valueOf(qualityGateSetting);
      definitionFromServer = qualityGateClient.show(qGateId);
    } catch(NumberFormatException configIsNameInsteadOfId) {
      definitionFromServer = qualityGateClient.show(qualityGateSetting);
    }
    return definitionFromServer;
  }
}
