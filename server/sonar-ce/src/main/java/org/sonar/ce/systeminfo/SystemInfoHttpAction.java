/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.systeminfo;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.sonar.ce.httpd.HttpAction;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

public class SystemInfoHttpAction implements HttpAction {

  private static final String PATH = "/systemInfo";
  private static final String PROTOBUF_MIME_TYPE = "application/x-protobuf";

  private final List<SystemInfoSection> sectionProviders;

  public SystemInfoHttpAction(List<SystemInfoSection> sectionProviders) {
    this.sectionProviders = sectionProviders;
  }

  @Override
  public String getContextPath() {
    return PATH;
  }
  @Override
  public void handle(HttpRequest request, HttpResponse response) {
    if (!"GET".equals(request.getRequestLine().getMethod())) {
      response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_METHOD_NOT_ALLOWED);
      response.setEntity(new StringEntity("Only GET is allowed", StandardCharsets.UTF_8));
      return;
    }
    ProtobufSystemInfo.SystemInfo.Builder infoBuilder = ProtobufSystemInfo.SystemInfo.newBuilder();

    sectionProviders.stream()
      .map(SystemInfoSection::toProtobuf)
      .forEach(infoBuilder::addSections);

    byte[] bytes = infoBuilder.build().toByteArray();
    response.setEntity(new ByteArrayEntity(bytes, ContentType.create(PROTOBUF_MIME_TYPE)));
    response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK);
  }
}
