/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.process.systeminfo;

import fi.iki.elonen.NanoHTTPD;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;
import org.slf4j.LoggerFactory;
import org.sonar.process.DefaultProcessCommands;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static java.lang.Integer.parseInt;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_PROCESS_INDEX;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_SHARED_PATH;

/**
 * This HTTP server exports data required for display of System Info page (and the related web service).
 * It listens on loopback address only, so it does not need to be secure (no HTTPS, no authentication).
 */
public class SystemInfoHttpServer {

  private static final String PROTOBUF_MIME_TYPE = "application/x-protobuf";

  private final Properties processProps;
  private final List<SystemInfoSection> sectionProviders;
  private final SystemInfoNanoHttpd nanoHttpd;

  public SystemInfoHttpServer(Properties processProps, List<SystemInfoSection> sectionProviders) throws UnknownHostException {
    this.processProps = processProps;
    this.sectionProviders = sectionProviders;
    InetAddress loopbackAddress = InetAddress.getByName(null);
    this.nanoHttpd = new SystemInfoNanoHttpd(loopbackAddress.getHostAddress(), 0);
  }

  // do not rename. This naming convention is required for picocontainer.
  public void start() {
    try {
      nanoHttpd.start();
      registerHttpUrl();
    } catch (IOException e) {
      throw new IllegalStateException("Can not start local HTTP server for System Info monitoring", e);
    }
  }

  private void registerHttpUrl() {
    int processNumber = parseInt(processProps.getProperty(PROPERTY_PROCESS_INDEX));
    File shareDir = new File(processProps.getProperty(PROPERTY_SHARED_PATH));
    try (DefaultProcessCommands commands = DefaultProcessCommands.secondary(shareDir, processNumber)) {
      String url = getUrl();
      commands.setSystemInfoUrl(url);
      LoggerFactory.getLogger(getClass()).debug("System Info HTTP server listening at {}", url);
    }
  }

  // do not rename. This naming convention is required for picocontainer.
  public void stop() {
    nanoHttpd.stop();
  }

  // visible for testing
  String getUrl() {
    return "http://" + nanoHttpd.getHostname() + ":" + nanoHttpd.getListeningPort();
  }

  private class SystemInfoNanoHttpd extends NanoHTTPD {

    SystemInfoNanoHttpd(String hostname, int port) {
      super(hostname, port);
    }

    @Override
    public Response serve(IHTTPSession session) {
      ProtobufSystemInfo.SystemInfo.Builder infoBuilder = ProtobufSystemInfo.SystemInfo.newBuilder();
      for (SystemInfoSection sectionProvider : sectionProviders) {
        ProtobufSystemInfo.Section section = sectionProvider.toProtobuf();
        infoBuilder.addSections(section);
      }
      byte[] bytes = infoBuilder.build().toByteArray();
      return newFixedLengthResponse(Response.Status.OK, PROTOBUF_MIME_TYPE, new ByteArrayInputStream(bytes), bytes.length);
    }
  }
}
