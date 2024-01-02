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
package org.sonarqube.ws.client.almintegrations;

import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 *
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/alm_integrations/webhook_github">Further information about this action online (including a response example)</a>
 * @since 9.7
 */
@Generated("sonar-ws-generator")
public class SendGithubCodeScanningAlertWebhookPayloadRequest {
  private String payload;
  private String githubEventHeader;
  private String githubSignatureHeader;
  private String githubSignature256Header;

  private String githubAppId;

  /**
   * This is a mandatory parameter.
   */
  public SendGithubCodeScanningAlertWebhookPayloadRequest setPayload(String payload) {
    this.payload = payload;
    return this;
  }

  public String getPayload() {
    return payload;
  }

  /**
   * This is a mandatory parameter.
   */
  public SendGithubCodeScanningAlertWebhookPayloadRequest setGithubEventHeader(String header) {
    this.githubEventHeader = header;
    return this;
  }

  public String getGithubEventHeader() {
    return githubEventHeader;
  }

  /**
   * This is a mandatory parameter.
   */
  public SendGithubCodeScanningAlertWebhookPayloadRequest setGithubSignatureHeader(String header) {
    this.githubSignatureHeader = header;
    return this;
  }

  public String getGithubSignatureHeader() {
    return this.githubSignatureHeader;
  }

  /**
   * This is a mandatory parameter.
   */
  public SendGithubCodeScanningAlertWebhookPayloadRequest setGithubSignature256Header(String header) {
    this.githubSignature256Header = header;
    return this;
  }

  public String getGithubSignature256Header() {
    return githubSignature256Header;
  }

  public String getGithubAppId() {
    return githubAppId;
  }

  public SendGithubCodeScanningAlertWebhookPayloadRequest setGithubAppId(String githubAppId) {
    this.githubAppId = githubAppId;
    return this;
  }
}
