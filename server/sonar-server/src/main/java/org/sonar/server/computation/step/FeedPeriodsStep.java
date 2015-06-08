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

package org.sonar.server.computation.step;

import com.google.common.base.Strings;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodFinder;
import org.sonar.server.computation.period.PeriodsHolderImpl;
import org.sonar.server.db.DbClient;

/**
 * Populates the {@link org.sonar.server.computation.period.PeriodsHolder}
 *
 * Here is how these periods are computed :
 * - Read the 5 period properties ${@link CoreProperties#TIMEMACHINE_PERIOD_PREFIX}
 * - Try to find the matching snapshots from the properties
 * - If a snapshot is found, a new period is added to the repository
 */
public class FeedPeriodsStep implements ComputationStep {
  private static final Logger LOG = LoggerFactory.getLogger(PeriodsHolderImpl.class);

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DateUtils.DATE_FORMAT);

  private static final int NUMBER_OF_PERIODS = 5;

  private final DbClient dbClient;
  private final Settings settings;
  private final TreeRootHolder treeRootHolder;
  private final PeriodFinder periodFinder;
  private final BatchReportReader batchReportReader;
  private final PeriodsHolderImpl periodsHolder;

  public FeedPeriodsStep(DbClient dbClient, Settings settings, TreeRootHolder treeRootHolder, PeriodFinder periodFinder, BatchReportReader batchReportReader,
    PeriodsHolderImpl periodsHolder) {
    this.dbClient = dbClient;
    this.settings = settings;
    this.treeRootHolder = treeRootHolder;
    this.periodFinder = periodFinder;
    this.batchReportReader = batchReportReader;
    this.periodsHolder = periodsHolder;
  }

  @Override
  public void execute() {
    periodsHolder.setPeriods(createPeriods());
  }

  private List<Period> createPeriods() {
    List<Period> periods = new ArrayList<>();
    DbSession session = dbClient.openSession(false);
    try {
      Component project = treeRootHolder.getRoot();
      ComponentDto projectDto = dbClient.componentDao().selectNullableByKey(session, project.getKey());
      // No project on first analysis, no period
      if (projectDto != null) {
        BatchReport.Component batchProject = batchReportReader.readComponent(project.getRef());
        PeriodResolver periodResolver = new PeriodResolver(session, projectDto.getId(), batchReportReader.readMetadata().getAnalysisDate(), batchProject.getVersion(),
          // TODO qualifier will be different for Views
          Qualifiers.PROJECT);

        for (int index = 1; index <= NUMBER_OF_PERIODS; index++) {
          Period period = periodResolver.resolve(index);
          // SONAR-4700 Add a past snapshot only if it exists
          if (period != null) {
            periods.add(period.setIndex(index));
            LOG.debug(period.toString());
          }
        }
      }
    } finally {
      session.close();
    }
    return periods;
  }

  private class PeriodResolver {

    private final DbSession session;
    private final long projectId;
    private final long analysisDate;
    private final String currentVersion;
    private final String qualifier;

    public PeriodResolver(DbSession session, long projectId, long analysisDate, String currentVersion, String qualifier) {
      this.session = session;
      this.projectId = projectId;
      this.analysisDate = analysisDate;
      this.currentVersion = currentVersion;
      this.qualifier = qualifier;
    }

    @CheckForNull
    private Period resolve(int index) {
      String propertyValue = getPropertyValue(qualifier, settings, index);
      Period period = resolve(index, propertyValue);
      if (period == null && StringUtils.isNotBlank(propertyValue)) {
        LOG.debug("Property " + CoreProperties.TIMEMACHINE_PERIOD_PREFIX + index + " is not valid: " + propertyValue);
      }
      return period;
    }

    @CheckForNull
    private Period resolve(int index, String property) {
      if (StringUtils.isBlank(property)) {
        return null;
      }

      Integer days = tryToResolveByDays(property);
      if (days != null) {
        return periodFinder.findByDays(session, projectId, analysisDate, days);
      }
      Date date = tryToResolveByDate(property);
      if (date != null) {
        return periodFinder.findByDate(session, projectId, date);
      }
      if (StringUtils.equals(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, property)) {
        return periodFinder.findByPreviousAnalysis(session, projectId, analysisDate);
      }
      if (StringUtils.equals(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION, property)) {
        return periodFinder.findByPreviousVersion(session, projectId, currentVersion);
      }
      return periodFinder.findByVersion(session, projectId, property);
    }

    @CheckForNull
    private Integer tryToResolveByDays(String property) {
      try {
        return Integer.parseInt(property);
      } catch (NumberFormatException e) {
        return null;
      }
    }

    @CheckForNull
    private Date tryToResolveByDate(String property) {
      try {
        return DATE_FORMAT.parse(property);
      } catch (ParseException e) {
        return null;
      }
    }
  }

  private static String getPropertyValue(@Nullable String qualifier, Settings settings, int index) {
    String value = settings.getString(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + index);
    // For periods 4 and 5 we're also searching for a property prefixed by the qualifier
    if (index > 3 && Strings.isNullOrEmpty(value)) {
      value = settings.getString(CoreProperties.TIMEMACHINE_PERIOD_PREFIX + index + "." + qualifier);
    }
    return value;
  }

  @Override
  public String getDescription() {
    return "Feed differential periods";
  }
}
