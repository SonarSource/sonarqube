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
package org.sonar.server.plugins.ws;

import java.util.Objects;
import java.util.Optional;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.extension.PluginRiskConsent;
import org.sonar.core.platform.EditionProvider.Edition;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.server.plugins.PluginDownloader;
import org.sonar.server.plugins.UpdateCenterMatrixFactory;
import org.sonar.server.user.UserSession;
import org.sonar.updatecenter.common.PluginUpdate;
import org.sonar.updatecenter.common.UpdateCenter;

import static java.lang.String.format;
import static org.sonar.core.config.CorePropertyDefinitions.PLUGINS_RISK_CONSENT;
import static org.sonar.server.plugins.edition.EditionBundledPlugins.isEditionBundled;

/**
 * Implementation of the {@code install} action for the Plugins WebService.
 */
public class InstallAction implements PluginsWsAction {

  private static final String BR_HTML_TAG = "<br/>";
  private static final String PARAM_KEY = "key";

  private final UpdateCenterMatrixFactory updateCenterFactory;
  private final PluginDownloader pluginDownloader;
  private final UserSession userSession;
  private final Configuration configuration;
  private final PlatformEditionProvider editionProvider;

  public InstallAction(UpdateCenterMatrixFactory updateCenterFactory, PluginDownloader pluginDownloader,
    UserSession userSession, Configuration configuration,
    PlatformEditionProvider editionProvider) {
    this.updateCenterFactory = updateCenterFactory;
    this.pluginDownloader = pluginDownloader;
    this.userSession = userSession;
    this.configuration = configuration;
    this.editionProvider = editionProvider;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("install")
      .setPost(true)
      .setSince("5.2")
      .setDescription("Installs the latest version of a plugin specified by its key." +
        BR_HTML_TAG +
        "Plugin information is retrieved from Update Center." +
        BR_HTML_TAG +
        "Fails if used on commercial editions or plugin risk consent has not been accepted." +
        BR_HTML_TAG +
        "Requires user to be authenticated with Administer System permissions")
      .setHandler(this);

    action.createParam(PARAM_KEY).setRequired(true)
      .setDescription("The key identifying the plugin to install");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();
    checkEdition();

    if (!hasPluginInstallConsent()) {
      throw new IllegalArgumentException("Can't install plugin without accepting firstly plugins risk consent");
    }

    String key = request.mandatoryParam(PARAM_KEY);
    PluginUpdate pluginUpdate = findAvailablePluginByKey(key);
    pluginDownloader.download(key, pluginUpdate.getRelease().getVersion());
    response.noContent();
  }

  private void checkEdition() {
    Edition edition = editionProvider.get().orElse(Edition.COMMUNITY);
    if (Edition.COMMUNITY != edition) {
      throw new IllegalArgumentException("This WS is unsupported in commercial edition. Please install plugin manually.");
    }
  }

  private boolean hasPluginInstallConsent() {
    Optional<String> pluginRiskConsent = configuration.get(PLUGINS_RISK_CONSENT);
    return pluginRiskConsent.filter(s -> PluginRiskConsent.valueOf(s) == PluginRiskConsent.ACCEPTED).isPresent();
  }

  private PluginUpdate findAvailablePluginByKey(String key) {
    PluginUpdate pluginUpdate = null;

    Optional<UpdateCenter> updateCenter = updateCenterFactory.getUpdateCenter(false);
    if (updateCenter.isPresent()) {
      pluginUpdate = updateCenter.get().findAvailablePlugins()
        .stream()
        .filter(Objects::nonNull)
        .filter(u -> key.equals(u.getPlugin().getKey()))
        .findFirst()
        .orElse(null);
    }

    if (pluginUpdate == null) {
      throw new IllegalArgumentException(
        format("No plugin with key '%s' or plugin '%s' is already installed in latest version", key, key));
    }
    if (isEditionBundled(pluginUpdate.getPlugin())) {
      throw new IllegalArgumentException(format(
        "SonarSource commercial plugin with key '%s' can only be installed as part of a SonarSource edition",
        pluginUpdate.getPlugin().getKey()));
    }

    return pluginUpdate;
  }
}
