/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.plugins.ws;

import java.io.InputStream;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.plugins.PluginFilesAndMd5.FileAndMd5;
import org.sonar.server.plugins.ServerPlugin;
import org.sonar.server.plugins.ServerPluginRepository;

public class DownloadAction implements PluginsWsAction {

  private static final String PACK200 = "pack200";
  private static final String ACCEPT_COMPRESSIONS_PARAM = "acceptCompressions";
  private static final String PLUGIN_PARAM = "plugin";

  private final ServerPluginRepository pluginRepository;

  public DownloadAction(ServerPluginRepository pluginRepository) {
    this.pluginRepository = pluginRepository;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("download")
      .setSince("7.2")
      .setDescription("Download plugin JAR, for usage by scanner engine")
      .setInternal(true)
      .setResponseExample(getClass().getResource("example-download.json"))
      .setHandler(this);

    action.createParam(PLUGIN_PARAM)
      .setRequired(true)
      .setDescription("The key identifying the plugin to download")
      .setExampleValue("cobol");

    action.createParam(ACCEPT_COMPRESSIONS_PARAM)
      .setExampleValue(PACK200);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String pluginKey = request.mandatoryParam(PLUGIN_PARAM);

    Optional<ServerPlugin> file = pluginRepository.findPlugin(pluginKey);
    if (!file.isPresent()) {
      throw new NotFoundException("Plugin " + pluginKey + " not found");
    }

    FileAndMd5 downloadedFile;
    FileAndMd5 compressedJar = file.get().getCompressed();
    if (compressedJar != null && PACK200.equals(request.param(ACCEPT_COMPRESSIONS_PARAM))) {
      response.stream().setMediaType("application/octet-stream");

      response.setHeader("Sonar-Compression", PACK200);
      response.setHeader("Sonar-UncompressedMD5", file.get().getJar().getMd5());
      downloadedFile = compressedJar;
    } else {
      response.stream().setMediaType("application/java-archive");
      downloadedFile = file.get().getJar();
    }
    response.setHeader("Sonar-MD5", downloadedFile.getMd5());
    try (InputStream input = FileUtils.openInputStream(downloadedFile.getFile())) {
      IOUtils.copyLarge(input, response.stream().output());
    }
  }
}
