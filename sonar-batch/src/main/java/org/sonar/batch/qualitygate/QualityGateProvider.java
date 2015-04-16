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

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.picocontainer.injectors.ProviderAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.api.utils.HttpDownloader;

import java.net.HttpURLConnection;

public class QualityGateProvider extends ProviderAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(QualityGateProvider.class);

  private static final String PROPERTY_QUALITY_GATE = "sonar.qualitygate";

  private static final String SHOW_URL = "/api/qualitygates/show";

  private static final String ATTRIBUTE_CONDITIONS = "conditions";

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
      result = load(qualityGateSetting, client, metricFinder);
      logger.info("Loaded quality gate '{}'", result.name());
    }
    return result;
  }

  private QualityGate load(String qualityGateSetting, ServerClient client, MetricFinder metricFinder) {
    QualityGate configuredGate = null;
    try {
      configuredGate = fetch(qualityGateSetting, client, metricFinder);
    } catch (HttpDownloader.HttpException serverError) {
      if (serverError.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
        throw MessageException.of("Quality gate '" + qualityGateSetting + "' was not found.");
      } else {
        throw serverError;
      }
    }

    return configuredGate;
  }

  private QualityGate fetch(String qualityGateSetting, ServerClient client, MetricFinder metricFinder) {
    String jsonText = null;
    try {
      long qGateId = Long.parseLong(qualityGateSetting);
      jsonText = client.request(SHOW_URL + "?id=" + qGateId, false);
    } catch (NumberFormatException configIsNameInsteadOfId) {
      jsonText = client.request(SHOW_URL + "?name=" + qualityGateSetting, false);
    }

    JsonParser parser = new JsonParser();
    JsonObject root = parser.parse(jsonText).getAsJsonObject();

    QualityGate configuredGate = new QualityGate(root.get("name").getAsString());

    if (root.has(ATTRIBUTE_CONDITIONS)) {
      for (JsonElement condition : root.get(ATTRIBUTE_CONDITIONS).getAsJsonArray()) {
        JsonObject conditionObject = condition.getAsJsonObject();
        configuredGate.add(new ResolvedCondition(conditionObject, metricFinder.findByKey(conditionObject.get("metric").getAsString())));
      }
    }

    return configuredGate;
  }
}
