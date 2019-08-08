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
package org.sonar.server.updatecenter.ws;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.platform.ServerFileSystem;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.sonar.api.server.ws.Request.Part;

public class UploadAction implements UpdateCenterWsAction {

  public static final String PARAM_FILE = "file";

  private final UserSession userSession;
  private final File downloadDir;

  public UploadAction(UserSession userSession, ServerFileSystem fileSystem) {
    this.userSession = userSession;
    this.downloadDir = fileSystem.getDownloadedPluginsDir();
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("upload")
      .setDescription("Upload a plugin.<br /> Requires 'Administer System' permission.")
      .setSince("6.0")
      .setPost(true)
      .setInternal(true)
      .setHandler(this);

    action.createParam(PARAM_FILE)
      .setDescription("The jar file of the plugin to install")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();

    Part part = request.mandatoryParamAsPart(PARAM_FILE);
    String fileName = part.getFileName();
    checkArgument(fileName.endsWith(".jar"), "Only jar file is allowed");
    InputStream inputStream = part.getInputStream();
    try {
      File destPlugin = new File(downloadDir, fileName);
      Files.copy(inputStream, destPlugin.toPath(), REPLACE_EXISTING);
      response.noContent();
    } finally {
      closeQuietly(inputStream);
    }
  }
}
