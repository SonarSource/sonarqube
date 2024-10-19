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
package org.sonar.server.badge.ws;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.write;
import static org.sonar.server.badge.ws.ETagUtils.RFC1123_DATE;
import static org.sonar.server.badge.ws.ETagUtils.getETag;
import static org.sonarqube.ws.MediaTypes.SVG;

public abstract class AbstractProjectBadgesWsAction implements ProjectBadgesWsAction {
  protected final ProjectBadgesSupport support;
  protected final SvgGenerator svgGenerator;

  protected AbstractProjectBadgesWsAction(ProjectBadgesSupport support, SvgGenerator svgGenerator) {
    this.support = support;
    this.svgGenerator = svgGenerator;
  }

  @Override
  public void handle(Request request, Response response) throws IOException {
    response.setHeader("Cache-Control", "no-cache");
    response.stream().setMediaType(SVG);
    try {
      support.validateToken(request);
      String result = getBadge(request);
      String eTag = getETag(result);
      Optional<String> requestedETag = request.header("If-None-Match");
      if (requestedETag.filter(eTag::equals).isPresent()) {
        response.stream().setStatus(304);
        return;
      }
      response.setHeader("ETag", eTag);
      write(result, response.stream().output(), UTF_8);
    } catch (ProjectBadgesException | ForbiddenException | NotFoundException e) {
      // There is an issue, so do not return any ETag but make this response expire now
      SimpleDateFormat sdf = new SimpleDateFormat(RFC1123_DATE, Locale.US);
      response.setHeader("Expires", sdf.format(new Date()));
      write(svgGenerator.generateError(e.getMessage()), response.stream().output(), UTF_8);
    }
  }

  protected abstract String getBadge(Request request);
}
