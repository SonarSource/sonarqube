/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.sticker.ws;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.sticker.ws.SvgGenerator.Color;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.write;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.COVERAGE_KEY;
import static org.sonar.api.measures.CoreMetrics.DUPLICATED_LINES_DENSITY_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SQALE_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.api.measures.CoreMetrics.TESTS_KEY;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES_KEY;
import static org.sonar.api.measures.Metric.Level;
import static org.sonar.api.measures.Metric.ValueType;
import static org.sonar.api.measures.Metric.Level.ERROR;
import static org.sonar.api.measures.Metric.Level.OK;
import static org.sonar.api.measures.Metric.Level.WARN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.A;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.B;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.C;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.D;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.E;
import static org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating.valueOf;
import static org.sonar.server.sticker.ws.NumberFormatter.formatNumber;
import static org.sonar.server.sticker.ws.StickersWs.SVG_MEDIA_TYPE;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class MeasureAction implements StickersWsAction {

  private static final String PARAM_COMPONENT = "component";
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_METRIC = "metric";

  private static final Map<String, String> METRIC_NAME_BY_KEY = ImmutableMap.<String, String>builder()
    .put(ALERT_STATUS_KEY, "quality gate")
    .put(COVERAGE_KEY, "coverage")
    .put(RELIABILITY_RATING_KEY, "reliability")
    .put(SECURITY_RATING_KEY, "security")
    .put(SQALE_RATING_KEY, "maintainability")
    .put(BUGS_KEY, "bugs")
    .put(VULNERABILITIES_KEY, "vulnerabilities")
    .put(CODE_SMELLS_KEY, "code smells")
    .put(DUPLICATED_LINES_DENSITY_KEY, "duplicated lines")
    .put(TECHNICAL_DEBT_KEY, "technical debt")
    .put(TESTS_KEY, "unit tests")
    .build();

  private static final Map<Level, String> QUALITY_GATE_MESSAGE_BY_STATUS = new EnumMap<>(ImmutableMap.of(
    OK, "passed",
    WARN, "warning",
    ERROR, "failed"));

  private static final Map<Level, Color> COLOR_BY_QUALITY_GATE_STATUS = new EnumMap<>(ImmutableMap.of(
    OK, Color.QUALITY_GATE_OK,
    WARN, Color.QUALITY_GATE_WARN,
    ERROR, Color.QUALITY_GATE_ERROR));

  private static final Map<Rating, Color> COLOR_BY_RATING = new EnumMap<>(ImmutableMap.of(
    A, Color.RATING_A,
    B, Color.RATING_B,
    C, Color.RATING_C,
    D, Color.RATING_D,
    E, Color.RATING_E));

  private static final DecimalFormat PERCENT_FORMATTER = new DecimalFormat("#.#");

  private final UserSession userSession;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final SvgGenerator svgGenerator;
  private final Durations durations;

  public MeasureAction(UserSession userSession, DbClient dbClient, ComponentFinder componentFinder, SvgGenerator svgGenerator, Durations durations) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.svgGenerator = svgGenerator;
    this.durations = durations;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("measure")
      .setHandler(this)
      .setDescription("Generate badge for measure as an SVG")
      .setSince("7.1")
      .setResponseExample(Resources.getResource(getClass(), "measure-example.svg"));
    action.createParam(PARAM_COMPONENT)
      .setDescription("Project key")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
    action
      .createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setExampleValue(KEY_BRANCH_EXAMPLE_001);
    action.createParam(PARAM_METRIC)
      .setDescription("Metric key")
      .setRequired(true)
      .setPossibleValues(METRIC_NAME_BY_KEY.keySet());
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    response.stream().setMediaType(SVG_MEDIA_TYPE);
    String projectKey = request.mandatoryParam(PARAM_COMPONENT);
    String branch = request.param(PARAM_BRANCH);
    String metricKey = request.mandatoryParam(PARAM_METRIC);
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto project = componentFinder.getByKeyAndOptionalBranch(dbSession, projectKey, branch);
      userSession.checkComponentPermission(USER, project);
      MetricDto metric = dbClient.metricDao().selectByKey(dbSession, metricKey);
      checkState(metric != null && metric.isEnabled(), "Metric '%s' hasn't been found", metricKey);
      LiveMeasureDto measure = getMeasure(dbSession, project, metricKey);
      write(generateSvg(metric, measure), response.stream().output(), UTF_8);
    } catch (StickerException | ForbiddenException | NotFoundException e) {
      write(svgGenerator.generateError(e.getMessage()), response.stream().output(), UTF_8);
    }
  }

  private LiveMeasureDto getMeasure(DbSession dbSession, ComponentDto project, String metricKey) {
    return dbClient.liveMeasureDao().selectMeasure(dbSession, project.uuid(), metricKey)
      .orElseThrow(() -> new StickerException(format("Measure '%s' has not been found for project '%s' and branch '%s'", metricKey, project.getKey(), project.getBranch())));
  }

  private String generateSvg(MetricDto metric, LiveMeasureDto measure) {
    String metricType = metric.getValueType();
    switch (ValueType.valueOf(metricType)) {
      case INT:
        return generateBadge(metric, formatNumber(getNonNullValue(measure, LiveMeasureDto::getValue).longValue()), Color.DEFAULT);
      case PERCENT:
        return generateBadge(metric, PERCENT_FORMATTER.format(getNonNullValue(measure, LiveMeasureDto::getValue)) + "%", Color.DEFAULT);
      case LEVEL:
        return generateQualityGate(metric, measure);
      case WORK_DUR:
        return generateBadge(metric, durations.format(Duration.create(getNonNullValue(measure, LiveMeasureDto::getValue).longValue())), Color.DEFAULT);
      case RATING:
        return generateRating(metric, measure);
      default:
        throw new IllegalStateException(format("Invalid metric type '%s'", metricType));
    }
  }

  private String generateQualityGate(MetricDto metric, LiveMeasureDto measure) {
    Level qualityGate = Level.valueOf(getNonNullValue(measure, LiveMeasureDto::getTextValue));
    return generateBadge(metric, QUALITY_GATE_MESSAGE_BY_STATUS.get(qualityGate), COLOR_BY_QUALITY_GATE_STATUS.get(qualityGate));
  }

  private String generateRating(MetricDto metric, LiveMeasureDto measure) {
    Rating rating = valueOf(getNonNullValue(measure, LiveMeasureDto::getValue).intValue());
    return generateBadge(metric, rating.name(), COLOR_BY_RATING.get(rating));
  }

  private String generateBadge(MetricDto metric, String value, Color color) {
    return svgGenerator.generateBadge(METRIC_NAME_BY_KEY.get(metric.getKey()), value, color);
  }

  private static <PARAM> PARAM getNonNullValue(LiveMeasureDto measure, Function<LiveMeasureDto, PARAM> function) {
    PARAM value = function.apply(measure);
    checkState(value != null, "Measure not found");
    return value;
  }

}
