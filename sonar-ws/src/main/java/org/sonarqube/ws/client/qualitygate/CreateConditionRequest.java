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
package org.sonarqube.ws.client.qualitygate;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

@Immutable
public class CreateConditionRequest {

  private final long qualityGateId;
  private final String metricKey;
  private final String operator;
  private final String warning;
  private final String error;
  private final Integer period;

  private CreateConditionRequest(Builder builder) {
    this.qualityGateId = builder.qualityGateId;
    this.metricKey = builder.metricKey;
    this.operator = builder.operator;
    this.warning = builder.warning;
    this.error = builder.error;
    this.period = builder.period;
  }

  public long getQualityGateId() {
    return qualityGateId;
  }

  public String getMetricKey() {
    return metricKey;
  }

  public String getOperator() {
    return operator;
  }

  @CheckForNull
  public String getWarning() {
    return warning;
  }

  @CheckForNull
  public String getError() {
    return error;
  }

  @CheckForNull
  public Integer getPeriod() {
    return period;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private long qualityGateId;
    private String metricKey;
    private String operator;
    private String warning;
    private String error;
    private Integer period;

    private Builder() {
      // enforce factory method use
    }

    public Builder setQualityGateId(long qualityGateId) {
      this.qualityGateId = qualityGateId;
      return this;
    }

    public Builder setMetricKey(String metricKey) {
      this.metricKey = metricKey;
      return this;
    }

    public Builder setOperator(String operator) {
      this.operator = operator;
      return this;
    }

    public Builder setWarning(@Nullable String warning) {
      this.warning = warning;
      return this;
    }

    public Builder setError(@Nullable String error) {
      this.error = error;
      return this;
    }

    public Builder setPeriod(@Nullable Integer period) {
      this.period = period;
      return this;
    }

    public CreateConditionRequest build() {
      checkArgument(qualityGateId > 0, "Quality gate id is mandatory and must not be empty");
      checkArgument(!isNullOrEmpty(metricKey), "Metric key is mandatory and must not be empty");
      checkArgument(!isNullOrEmpty(operator), "Operator is mandatory and must not be empty");
      return new CreateConditionRequest(this);
    }
  }
}
