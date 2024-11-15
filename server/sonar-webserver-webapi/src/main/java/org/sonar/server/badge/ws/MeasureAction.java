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

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.badge.ws.SvgGenerator.Color;
import org.sonar.server.measure.Rating;
import org.sonar.server.telemetry.TelemetryBadgeProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES_KEY;
import static org.sonar.api.measures.Metric.Level;
import static org.sonar.api.measures.Metric.ValueType;
import static org.sonar.api.measures.Metric.Level.ERROR;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_RATING_KEY;
import static org.sonar.server.badge.ws.SvgFormatter.formatDuration;
import static org.sonar.server.badge.ws.SvgFormatter.formatNumeric;
import static org.sonar.server.badge.ws.SvgFormatter.formatPercent;
import static org.sonar.server.measure.Rating.A;
import static org.sonar.server.measure.Rating.B;
import static org.sonar.server.measure.Rating.C;
import static org.sonar.server.measure.Rating.D;
import static org.sonar.server.measure.Rating.E;

public class MeasureAction extends AbstractProjectBadgesWsAction {

  private static final String PARAM_METRIC = "metric";

  private static final Map<String, String> METRIC_NAME_BY_KEY = ImmutableMap.<String, String>builder()
    .put(COVERAGE_KEY, "coverage")
    .put(DUPLICATED_LINES_DENSITY_KEY, "duplicated lines")
    .put(NCLOC_KEY, "lines of code")
    .put(ALERT_STATUS_KEY, "quality gate")
    .put(SECURITY_HOTSPOTS_KEY, "security hotspots")

    // Standard mode
    .put(BUGS_KEY, "bugs")
    .put(CODE_SMELLS_KEY, "code smells")
    .put(VULNERABILITIES_KEY, "vulnerabilities")
    .put(SQALE_RATING_KEY, "maintainability")
    .put(RELIABILITY_RATING_KEY, "reliability")
    .put(SECURITY_RATING_KEY, "security")
    .put(TECHNICAL_DEBT_KEY, "technical debt")

    // MQR mode
    .put(SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY, "reliability issues")
    .put(SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY, "maintainability issues")
    .put(SOFTWARE_QUALITY_SECURITY_ISSUES_KEY, "security issues")
    .put(SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY, "maintainability")
    .put(SOFTWARE_QUALITY_RELIABILITY_RATING_KEY, "reliability")
    .put(SOFTWARE_QUALITY_SECURITY_RATING_KEY, "security")
    .put(SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY, "technical debt")
    .build();

  private static final String[] UNDEPRECATED_METRIC_KEYS = {BUGS_KEY, CODE_SMELLS_KEY, SECURITY_HOTSPOTS_KEY, VULNERABILITIES_KEY};

  private static final Map<Level, String> QUALITY_GATE_MESSAGE_BY_STATUS = new EnumMap<>(Map.of(
    OK, "passed",
    ERROR, "failed"));

  private static final Map<Level, Color> COLOR_BY_QUALITY_GATE_STATUS = new EnumMap<>(Map.of(
    OK, Color.QUALITY_GATE_OK,
    ERROR, Color.QUALITY_GATE_ERROR));

  private static final Map<Rating, Color> COLOR_BY_RATING = new EnumMap<>(Map.of(
    A, Color.RATING_A,
    B, Color.RATING_B,
    C, Color.RATING_C,
    D, Color.RATING_D,
    E, Color.RATING_E));

  private final DbClient dbClient;
  private final TelemetryBadgeProvider telemetryBadgeProvider;

  public MeasureAction(DbClient dbClient, ProjectBadgesSupport support, SvgGenerator svgGenerator,
    TelemetryBadgeProvider telemetryBadgeProvider) {
    super(support, svgGenerator);
    this.dbClient = dbClient;
    this.telemetryBadgeProvider = telemetryBadgeProvider;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("measure")
      .setHandler(this)
      .setDescription("Generate badge for project's measure as an SVG.<br/>" +
        "Requires 'Browse' permission on the specified project.")
      .setSince("7.1")
      .setChangelog(new Change("10.8", format("The following metric keys are not deprecated anymore: %s", String.join(", ",
        UNDEPRECATED_METRIC_KEYS))))
      .setChangelog(new Change("10.4", format("The following metric keys are now deprecated: %s", String.join(", ",
        UNDEPRECATED_METRIC_KEYS))))
      .setResponseExample(Resources.getResource(getClass(), "measure-example.svg"));
    support.addProjectAndBranchParams(action);
    action.createParam(PARAM_METRIC)
      .setDescription("Metric key")
      .setRequired(true)
      .setPossibleValues(METRIC_NAME_BY_KEY.keySet());
  }

  @Override
  protected String getBadge(Request request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String metricKey = request.mandatoryParam(PARAM_METRIC);
      BranchDto branch = support.getBranch(dbSession, request);
      MetricDto metric = dbClient.metricDao().selectByKey(dbSession, metricKey);
      checkState(metric != null && metric.isEnabled(), "Metric '%s' hasn't been found", metricKey);
      MeasureDto measure = getMeasure(dbSession, branch);
      telemetryBadgeProvider.incrementForMetric(metricKey);
      return generateSvg(metric, measure);
    }
  }

  private MeasureDto getMeasure(DbSession dbSession, BranchDto branch) {
    return dbClient.measureDao().selectByComponentUuid(dbSession, branch.getUuid())
      .orElseThrow(() -> new ProjectBadgesException("Measure has not been found"));
  }

  private String generateSvg(MetricDto metric, MeasureDto measure) {
    String metricType = metric.getValueType();
    switch (ValueType.valueOf(metricType)) {
      case INT:
        return generateBadge(metric, formatNumeric(getNonNullValue(measure, m -> m.getLong(metric.getKey()))), Color.DEFAULT);
      case PERCENT:
        return generateBadge(metric, formatPercent(getNonNullValue(measure, m -> m.getDouble(metric.getKey()))), Color.DEFAULT);
      case LEVEL:
        return generateQualityGate(metric, measure);
      case WORK_DUR:
        return generateBadge(metric, formatDuration(getNonNullValue(measure, m -> m.getLong(metric.getKey()))), Color.DEFAULT);
      case RATING:
        return generateRating(metric, measure);
      default:
        throw new IllegalStateException(format("Invalid metric type '%s'", metricType));
    }
  }

  private String generateQualityGate(MetricDto metric, MeasureDto measure) {
    Level qualityGate = Level.valueOf(getNonNullValue(measure, m -> m.getString(metric.getKey())));
    return generateBadge(metric, QUALITY_GATE_MESSAGE_BY_STATUS.get(qualityGate), COLOR_BY_QUALITY_GATE_STATUS.get(qualityGate));
  }

  private String generateRating(MetricDto metric, MeasureDto measure) {
    Rating rating = Rating.valueOf(getNonNullValue(measure, m -> m.getInt(metric.getKey())).intValue());
    return generateBadge(metric, rating.name(), COLOR_BY_RATING.get(rating));
  }

  private String generateBadge(MetricDto metric, String value, Color color) {
    return svgGenerator.generateBadge(METRIC_NAME_BY_KEY.get(metric.getKey()), value, color);
  }

  private static <P> P getNonNullValue(MeasureDto measure, Function<MeasureDto, P> function) {
    P value = function.apply(measure);
    checkState(value != null, "Measure has not been found");
    return value;
  }

}
