/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.telemetry.metrics.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Set;
import org.sonar.telemetry.core.Dimension;

public class BaseMessage {
  @JsonProperty("message_uuid")
  private String messageUuid;

  @JsonProperty("installation_id")
  private String installationId;

  @JsonProperty("dimension")
  private Dimension dimension;

  @JsonProperty("metric_values")
  private Set<Metric> metrics;

  protected BaseMessage(String messageUuid, String installationId, Dimension dimension, Set<Metric> metrics) {
    this.messageUuid = messageUuid;
    this.installationId = installationId;
    this.dimension = dimension;
    this.metrics = metrics;
  }

  public String getMessageUuid() {
    return messageUuid;
  }

  public String getInstallationId() {
    return installationId;
  }

  public Dimension getDimension() {
    return dimension;
  }

  public Set<Metric> getMetrics() {
    return metrics;
  }

  public static class Builder {
    private String messageUuid;
    private String installationId;
    private Dimension dimension;
    private Set<Metric> metrics;

    public Builder setMessageUuid(String messageUuid) {
      this.messageUuid = messageUuid;
      return this;
    }

    public Builder setInstallationId(String installationId) {
      this.installationId = installationId;
      return this;
    }

    public Builder setDimension(Dimension dimension) {
      this.dimension = dimension;
      return this;
    }

    public Builder setMetrics(Set<Metric> metrics) {
      this.metrics = metrics;
      return this;
    }

    public BaseMessage build() {
      Objects.requireNonNull(messageUuid, "messageUuid must be specified");
      Objects.requireNonNull(installationId, "installationId must be specified");
      Objects.requireNonNull(dimension, "dimension must be specified");
      Objects.requireNonNull(metrics, "metrics must be specified");

      return new BaseMessage(messageUuid, installationId, dimension, metrics);
    }
  }
}