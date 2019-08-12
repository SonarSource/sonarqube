/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.ws;

import com.google.protobuf.Message;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.util.ProtobufJsonFormat;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonarqube.ws.MediaTypes.JSON;
import static org.sonarqube.ws.MediaTypes.PROTOBUF;

public class WsUtils {

  private WsUtils() {
    // only statics
  }

  public static void writeProtobuf(Message msg, Request request, Response response) {
    OutputStream output = response.stream().output();
    try {
      if (request.getMediaType().equals(PROTOBUF)) {
        response.stream().setMediaType(PROTOBUF);
        msg.writeTo(output);
      } else {
        response.stream().setMediaType(JSON);
        try (JsonWriter writer = JsonWriter.of(new OutputStreamWriter(output, UTF_8))) {
          ProtobufJsonFormat.write(msg, writer);
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Error while writing protobuf message", e);
    } finally {
      IOUtils.closeQuietly(output);
    }
  }

  public static <T> T checkStateWithOptional(java.util.Optional<T> value, String message, Object... messageArguments) {
    if (!value.isPresent()) {
      throw new IllegalStateException(format(message, messageArguments));
    }

    return value.get();
  }
}
