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
package org.sonar.server.v2.api.analysis.controller;

import java.util.List;
import org.sonar.server.v2.api.analysis.response.JreInfoRestResponse;
import org.sonar.server.v2.api.analysis.service.JresHandler;
import org.springframework.core.io.InputStreamResource;

public class DefaultJresController implements JresController {

  private final JresHandler jresHandler;

  public DefaultJresController(JresHandler jresHandler) {
    this.jresHandler = jresHandler;
  }

  @Override
  public List<JreInfoRestResponse> getJresMetadata(String os, String arch) {
    return jresHandler.getJresMetadata(os, arch);
  }

  @Override
  public JreInfoRestResponse getJreMetadata(String id) {
    return jresHandler.getJreMetadata(id);
  }

  @Override
  public InputStreamResource downloadJre(String id) {
    JreInfoRestResponse jreInfoRestResponse = jresHandler.getJreMetadata(id);
    return new InputStreamResource(jresHandler.getJreBinary(jreInfoRestResponse.filename()));
  }

}
