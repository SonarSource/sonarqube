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
package org.sonar.server.ws;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.protobuf.Message;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.util.ProtobufJsonFormat;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.MessageFormatter;

import static java.lang.String.format;

public class WsUtils {
  private static final Logger LOG = Loggers.get(WsUtils.class);

  private WsUtils() {
    // only statics
  }

  public static void writeProtobuf(Message msg, Request request, Response response) {
    OutputStream output = response.stream().output();
    try {
      if (request.getMediaType().equals(MediaTypes.PROTOBUF)) {
        response.stream().setMediaType(MediaTypes.PROTOBUF);
        msg.writeTo(output);
      } else {
        response.stream().setMediaType(MediaTypes.JSON);
        try (OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
          ProtobufJsonFormat.write(msg, JsonWriter.of(writer));
        }
      }
    } catch (Exception e) {
      LOG.error("Error while writing protobuf message {}", MessageFormatter.print(msg));
      Throwables.propagate(e);
    } finally {
      IOUtils.closeQuietly(output);
    }
  }

  /**
   * @throws BadRequestException
   */
  public static void checkRequest(boolean expression, String message, Object... messageArguments) {
    if (!expression) {
      throw new BadRequestException(format(message, messageArguments));
    }
  }

  /**
   * @throws NotFoundException if the value if null
   * @return the value
   */
  public static <T> T checkFound(@Nullable T value, String message, Object... messageArguments) {
    if (value == null) {
      throw new NotFoundException(format(message, messageArguments));
    }

    return value;
  }

  /**
   * @throws NotFoundException if the value is not present
   * @return the value
   */
  public static <T> T checkFoundWithOptional(Optional<T> value, String message, Object... messageArguments) {
    if (!value.isPresent()) {
      throw new NotFoundException(format(message, messageArguments));
    }

    return value.get();
  }
}
