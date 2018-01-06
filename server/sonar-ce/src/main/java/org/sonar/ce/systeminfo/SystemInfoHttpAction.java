/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import fi.iki.elonen.NanoHTTPD;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.sonar.ce.httpd.HttpAction;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static fi.iki.elonen.NanoHTTPD.MIME_PLAINTEXT;
import static fi.iki.elonen.NanoHTTPD.Response.Status.*;
import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class SystemInfoHttpAction implements HttpAction {

  private static final String PATH = "systemInfo";
  private static final String PROTOBUF_MIME_TYPE = "application/x-protobuf";

  private final List<SystemInfoSection> sectionProviders;

  public SystemInfoHttpAction(List<SystemInfoSection> sectionProviders) {
    this.sectionProviders = sectionProviders;
  }

  @Override
  public void register(ActionRegistry registry) {
    registry.register(PATH, this);
  }

  @Override
  public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
    if (session.getMethod() != NanoHTTPD.Method.GET) {
      return newFixedLengthResponse(METHOD_NOT_ALLOWED, MIME_PLAINTEXT, null);
    }

    ProtobufSystemInfo.SystemInfo.Builder infoBuilder = ProtobufSystemInfo.SystemInfo.newBuilder();
    for (SystemInfoSection sectionProvider : sectionProviders) {
      ProtobufSystemInfo.Section section = sectionProvider.toProtobuf();
      infoBuilder.addSections(section);
    }
    byte[] bytes = infoBuilder.build().toByteArray();
    return newFixedLengthResponse(OK, PROTOBUF_MIME_TYPE, new ByteArrayInputStream(bytes), bytes.length);
  }
}
