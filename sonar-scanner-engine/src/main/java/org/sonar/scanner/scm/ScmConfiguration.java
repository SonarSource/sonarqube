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
package org.sonar.scanner.scm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.sonar.api.CoreProperties;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.api.config.Configuration;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.config.ScannerProperties;

import static org.sonar.api.CoreProperties.SCM_PROVIDER_KEY;

@Properties({
  @Property(
    key = ScmConfiguration.FORCE_RELOAD_KEY,
    defaultValue = "false",
    name = "Force reloading of SCM information for all files",
    description = "By default only files modified since previous analysis are inspected. Set this parameter to true to force the reloading.",
    category = CoreProperties.CATEGORY_SCM,
    project = false,
    module = false,
    global = false,
    type = PropertyType.BOOLEAN)
})
public class ScmConfiguration implements Startable {
  private static final Logger LOG = Loggers.get(ScmConfiguration.class);

  public static final String FORCE_RELOAD_KEY = "sonar.scm.forceReloadAll";

  static final String MESSAGE_SCM_STEP_IS_DISABLED_BY_CONFIGURATION = "SCM Step is disabled by configuration";
  static final String MESSAGE_SCM_EXLUSIONS_IS_DISABLED_BY_CONFIGURATION = "Exclusions based on SCM info is disabled by configuration";

  private final Configuration settings;
  private final AnalysisWarnings analysisWarnings;
  private final Map<String, ScmProvider> providerPerKey = new LinkedHashMap<>();
  private final AnalysisMode analysisMode;
  private final InputModuleHierarchy moduleHierarchy;

  private ScmProvider provider;

  public ScmConfiguration(InputModuleHierarchy moduleHierarchy, AnalysisMode analysisMode, Configuration settings, AnalysisWarnings analysisWarnings, ScmProvider... providers) {
    this.moduleHierarchy = moduleHierarchy;
    this.analysisMode = analysisMode;
    this.settings = settings;
    this.analysisWarnings = analysisWarnings;
    for (ScmProvider scmProvider : providers) {
      providerPerKey.put(scmProvider.key(), scmProvider);
    }
  }

  public ScmConfiguration(InputModuleHierarchy moduleHierarchy, AnalysisMode analysisMode, Configuration settings, AnalysisWarnings analysisWarnings) {
    this(moduleHierarchy, analysisMode, settings, analysisWarnings, new ScmProvider[0]);
  }

  @Override
  public void start() {
    if (analysisMode.isIssues()) {
      return;
    }
    if (isDisabled()) {
      LOG.debug(MESSAGE_SCM_STEP_IS_DISABLED_BY_CONFIGURATION);
      return;
    }
    if (settings.hasKey(SCM_PROVIDER_KEY)) {
      settings.get(SCM_PROVIDER_KEY).ifPresent(this::setProviderIfSupported);
    } else {
      autodetection();
      if (this.provider == null) {
        considerOldScmUrl();
      }
      if (this.provider == null) {
        String message = "SCM provider autodetection failed. Please use \"" + SCM_PROVIDER_KEY + "\" to define SCM of " +
          "your project, or disable the SCM Sensor in the project settings.";
        LOG.warn(message);
        analysisWarnings.addUnique(message);
      }
    }
    if (isExclusionDisabled()) {
      LOG.info(MESSAGE_SCM_EXLUSIONS_IS_DISABLED_BY_CONFIGURATION);
    }
  }

  private void setProviderIfSupported(String forcedProviderKey) {
    if (providerPerKey.containsKey(forcedProviderKey)) {
      this.provider = providerPerKey.get(forcedProviderKey);
    } else {
      String supportedProviders = providerPerKey.isEmpty() ? "No SCM provider installed"
        : ("Supported SCM providers are " + providerPerKey.keySet().stream().collect(Collectors.joining(",")));
      throw new IllegalArgumentException("SCM provider was set to \"" + forcedProviderKey + "\" but no SCM provider found for this key. " + supportedProviders);
    }
  }

  private void considerOldScmUrl() {
    settings.get(ScannerProperties.LINKS_SOURCES_DEV).ifPresent(url -> {
      if (StringUtils.startsWith(url, "scm:")) {
        String[] split = url.split(":");
        if (split.length > 1) {
          setProviderIfSupported(split[1]);
        }
      }
    });
  }

  private void autodetection() {
    for (ScmProvider installedProvider : providerPerKey.values()) {
      if (installedProvider.supports(moduleHierarchy.root().getBaseDir().toFile())) {
        if (this.provider == null) {
          this.provider = installedProvider;
        } else {
          throw MessageException.of("SCM provider autodetection failed. Both " + this.provider.key() + " and " + installedProvider.key()
            + " claim to support this project. Please use \"" + SCM_PROVIDER_KEY + "\" to define SCM of your project.");
        }
      }
    }
  }

  @CheckForNull
  public ScmProvider provider() {
    return provider;
  }

  public boolean isDisabled() {
    return settings.getBoolean(CoreProperties.SCM_DISABLED_KEY).orElse(false);
  }

  public boolean isExclusionDisabled() {
    return isDisabled() || settings.getBoolean(CoreProperties.SCM_EXCLUSIONS_DISABLED_KEY).orElse(false);
  }

  public boolean forceReloadAll() {
    return settings.getBoolean(FORCE_RELOAD_KEY).orElse(false);
  }

  @Override
  public void stop() {
    // Nothing to do
  }

}
