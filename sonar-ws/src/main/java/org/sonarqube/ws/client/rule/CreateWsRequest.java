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
package org.sonarqube.ws.client.rule;

public class CreateWsRequest {

  private String customKey;
  private String markdownDescription;
  private String name;
  private String params;
  private Boolean preventReactivation;
  private String severity;
  private String status;
  private String templateKey;

  private CreateWsRequest(Builder builder) {
    this.customKey = builder.customKey;
    this.markdownDescription = builder.markdownDescription;
    this.name = builder.name;
    this.params = builder.params;
    this.preventReactivation = builder.preventReactivation;
    this.severity = builder.severity;
    this.status = builder.status;
    this.templateKey = builder.templateKey;
  }

  public String getCustomKey() {
    return customKey;
  }

  public String getMarkdownDescription() {
    return markdownDescription;
  }

  public String getName() {
    return name;
  }

  public String getParams() {
    return params;
  }

  public Boolean getPreventReactivation() {
    return preventReactivation;
  }

  public String getSeverity() {
    return severity;
  }

  public String getStatus() {
    return status;
  }

  public String getTemplateKey() {
    return templateKey;
  }

  public static class Builder {
    private String customKey;
    private String markdownDescription;
    private String name;
    private String params;
    private Boolean preventReactivation;
    private String severity;
    private String status;
    private String templateKey;

    public Builder setCustomKey(String customKey) {
      this.customKey = customKey;
      return this;
    }

    public Builder setMarkdownDescription(String markdownDescription) {
      this.markdownDescription = markdownDescription;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setParams(String params) {
      this.params = params;
      return this;
    }

    public Builder setPreventReactivation(Boolean preventReactivation) {
      this.preventReactivation = preventReactivation;
      return this;
    }

    public Builder setSeverity(String severity) {
      this.severity = severity;
      return this;
    }

    public Builder setStatus(String status) {
      this.status = status;
      return this;
    }

    public Builder setTemplateKey(String templateKey) {
      this.templateKey = templateKey;
      return this;
    }

    public CreateWsRequest build() {
      return new CreateWsRequest(this);
    }
  }

}
