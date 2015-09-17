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
package org.sonar.server.ws;

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.util.ProtobufJsonFormat;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.plugins.MimeTypes;

public class WsUtils {

  private WsUtils() {
    // only statics
  }

  public static void writeProtobuf(Message msg, Request request, Response response) throws Exception {
    OutputStream output = response.stream().output();
    try {
      if (request.getMediaType().equals(MimeTypes.PROTOBUF)) {
        response.stream().setMediaType(MimeTypes.PROTOBUF);
        msg.writeTo(output);
      } else {
        response.stream().setMediaType(MimeTypes.JSON);
        try (OutputStreamWriter writer = new OutputStreamWriter(output)) {
          ProtobufJsonFormat.write(msg, JsonWriter.of(writer));
        }
      }
    } finally {
      IOUtils.closeQuietly(output);
    }
  }

  /**
   * @throws BadRequestException
   */
  public static void checkRequest(boolean expression, String message) {
    if (!expression) {
      throw new BadRequestException(message);
    }
  }

  /**
   * @throws NotFoundException if the value if null
   * @return the value
   */
  public static <T> T checkFound(T value, String message) {
    if (value == null) {
      throw new NotFoundException(message);
    }

    return value;
  }

  /**
   * @throws NotFoundException if the value is not present
   * @return the value
   */
  public static <T> T checkFound(Optional<T> value, String message) {
    if (!value.isPresent()) {
      throw new NotFoundException(message);
    }

    return value.get();
  }
}
