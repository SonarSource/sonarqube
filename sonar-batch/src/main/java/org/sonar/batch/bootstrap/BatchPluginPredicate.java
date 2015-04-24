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
package org.sonar.batch.bootstrap;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import javax.annotation.Nonnull;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

/**
 * Filters the plugins to be enabled during analysis
 */
public class BatchPluginPredicate implements Predicate<String>, BatchComponent {

  private static final Logger LOG = Loggers.get(BatchPluginPredicate.class);

  private static final String CORE_PLUGIN_KEY = "core";
  private static final String BUILDBREAKER_PLUGIN_KEY = "buildbreaker";
  private static final String PROPERTY_IS_DEPRECATED_MSG = "Property {0} is deprecated. Please use {1} instead.";

  private final Set<String> whites = newHashSet(), blacks = newHashSet();
  private final DefaultAnalysisMode mode;

  public BatchPluginPredicate(Settings settings, DefaultAnalysisMode mode) {
    this.mode = mode;
    if (settings.hasKey(CoreProperties.BATCH_INCLUDE_PLUGINS)) {
      whites.addAll(Arrays.asList(settings.getStringArray(CoreProperties.BATCH_INCLUDE_PLUGINS)));
    }
    if (settings.hasKey(CoreProperties.BATCH_EXCLUDE_PLUGINS)) {
      blacks.addAll(Arrays.asList(settings.getStringArray(CoreProperties.BATCH_EXCLUDE_PLUGINS)));
    }
    if (mode.isPreview()) {
      // These default values are not supported by Settings because the class CorePlugin
      // is not loaded yet.
      if (settings.hasKey(CoreProperties.DRY_RUN_INCLUDE_PLUGINS)) {
        LOG.warn(MessageFormat.format(PROPERTY_IS_DEPRECATED_MSG, CoreProperties.DRY_RUN_INCLUDE_PLUGINS, CoreProperties.PREVIEW_INCLUDE_PLUGINS));
        whites.addAll(propertyValues(settings,
          CoreProperties.DRY_RUN_INCLUDE_PLUGINS, CoreProperties.PREVIEW_INCLUDE_PLUGINS_DEFAULT_VALUE));
      } else {
        whites.addAll(propertyValues(settings,
          CoreProperties.PREVIEW_INCLUDE_PLUGINS, CoreProperties.PREVIEW_INCLUDE_PLUGINS_DEFAULT_VALUE));
      }
      if (settings.hasKey(CoreProperties.DRY_RUN_EXCLUDE_PLUGINS)) {
        LOG.warn(MessageFormat.format(PROPERTY_IS_DEPRECATED_MSG, CoreProperties.DRY_RUN_EXCLUDE_PLUGINS, CoreProperties.PREVIEW_EXCLUDE_PLUGINS));
        blacks.addAll(propertyValues(settings,
          CoreProperties.DRY_RUN_EXCLUDE_PLUGINS, CoreProperties.PREVIEW_EXCLUDE_PLUGINS_DEFAULT_VALUE));
      } else {
        blacks.addAll(propertyValues(settings,
          CoreProperties.PREVIEW_EXCLUDE_PLUGINS, CoreProperties.PREVIEW_EXCLUDE_PLUGINS_DEFAULT_VALUE));
      }
    }
    if (!whites.isEmpty()) {
      LOG.info("Include plugins: " + Joiner.on(", ").join(whites));
    }
    if (!blacks.isEmpty()) {
      LOG.info("Exclude plugins: " + Joiner.on(", ").join(blacks));
    }
  }

  @Override
  public boolean apply(@Nonnull String pluginKey) {
    if (CORE_PLUGIN_KEY.equals(pluginKey)) {
      return !mode.isMediumTest();
    }

    if (BUILDBREAKER_PLUGIN_KEY.equals(pluginKey) && mode.isPreview()) {
      LOG.info("Build Breaker plugin is no more supported in preview/incremental mode");
      return false;
    }

    // FIXME what happens if there are only white-listed plugins ?
    List<String> mergeList = newArrayList(blacks);
    mergeList.removeAll(whites);
    return mergeList.isEmpty() || !mergeList.contains(pluginKey);
  }

  Set<String> getWhites() {
    return whites;
  }

  Set<String> getBlacks() {
    return blacks;
  }

  static List<String> propertyValues(Settings settings, String key, String defaultValue) {
    String s = StringUtils.defaultIfEmpty(settings.getString(key), defaultValue);
    return Lists.newArrayList(Splitter.on(",").trimResults().split(s));
  }
}
