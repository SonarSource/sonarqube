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
package org.sonarqube.ws.client;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import java.io.InputStream;
import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonarqube.ws.MediaTypes;

import static org.sonarqube.ws.WsUtils.checkArgument;
import static org.sonarqube.ws.WsUtils.isNullOrEmpty;

public abstract class BaseService {
  protected static final String APPLICATION_MERGE_PATCH_JSON = "application/merge-patch+json";

  private final WsConnector wsConnector;
  protected final String controller;

  public BaseService(WsConnector wsConnector, String controllerPath) {
    checkArgument(!isNullOrEmpty(controllerPath), "Controller path cannot be empty");
    this.wsConnector = wsConnector;
    this.controller = controllerPath;
  }

  protected <T extends Message> T call(BaseRequest request, Parser<T> parser) {
    return call(request, parser, MediaTypes.PROTOBUF);
  }

  protected <T extends Message> T call(BaseRequest request, Parser<T> parser, String mediaType) {
    request.setMediaType(mediaType);
    WsResponse response = call(request);
    return convert(response, parser);
  }

  protected WsResponse call(WsRequest request) {
    return wsConnector.call(request).failIfNotSuccessful();
  }

  public static <T extends Message> T convert(WsResponse response, Parser<T> parser) {
    try (InputStream byteStream = response.contentStream()) {
      // HTTP header "Content-Type" is not verified. It may be different than protobuf.
      return parser.parseFrom(byteStream);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to parse protobuf response of " + response.requestUrl(), e);
    }
  }

  protected String path() {
    return controller;
  }

  protected String path(String action) {
    return String.format("%s/%s", controller, action);
  }

  @CheckForNull
  protected static String inlineMultipleParamValue(@Nullable Collection<String> values) {
    return values == null ? null : String.join(",", values);
  }
}
