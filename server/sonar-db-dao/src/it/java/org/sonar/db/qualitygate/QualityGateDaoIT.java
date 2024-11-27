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
package org.sonar.db.qualitygate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.qualitygate.QualityGateFindingDto.PERCENT_VALUE_TYPE;
import static org.sonar.db.qualitygate.QualityGateFindingDto.RATING_VALUE_TYPE;

class QualityGateDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final QualityGateDbTester qualityGateDbTester = new QualityGateDbTester(db);
  private final DbSession dbSession = db.getSession();
  private final QualityGateDao underTest = db.getDbClient().qualityGateDao();

  @Test
  void insert() {
    QualityGateDto newQgate = new QualityGateDto()
      .setName("My Quality Gate")
      .setBuiltIn(false)
      .setUpdatedAt(new Date());

    underTest.insert(dbSession, newQgate);
    dbSession.commit();

    QualityGateDto reloaded = underTest.selectByUuid(dbSession, newQgate.getUuid());
    assertThat(reloaded.getName()).isEqualTo("My Quality Gate");
    assertThat(reloaded.getUuid()).isEqualTo(newQgate.getUuid());
    assertThat(reloaded.isBuiltIn()).isFalse();
    assertThat(reloaded.isAiCodeSupported()).isFalse();
    assertThat(reloaded.getCreatedAt()).isNotNull();
    assertThat(reloaded.getUpdatedAt()).isNotNull();
  }

  @Test
  void insert_built_in() {
    underTest.insert(db.getSession(), new QualityGateDto().setName("test").setBuiltIn(true));

    QualityGateDto reloaded = underTest.selectByName(db.getSession(), "test");

    assertThat(reloaded.isBuiltIn()).isTrue();
  }

  @Test
  void insert_ai_code_supported() {
    underTest.insert(db.getSession(), new QualityGateDto().setName("test").setAiCodeSupported(true));

    QualityGateDto reloaded = underTest.selectByName(db.getSession(), "test");

    assertThat(reloaded.isAiCodeSupported()).isTrue();
  }

  @Test
  void select_all() {
    QualityGateDto qualityGate1 = qualityGateDbTester.insertQualityGate();
    QualityGateDto qualityGate2 = qualityGateDbTester.insertQualityGate();
    QualityGateDto qualityGateOnOtherOrg = qualityGateDbTester.insertQualityGate();

    assertThat(underTest.selectAll(dbSession))
      .extracting(QualityGateDto::getUuid)
      .containsExactlyInAnyOrder(qualityGate1.getUuid(), qualityGate2.getUuid(), qualityGateOnOtherOrg.getUuid());
  }

  @Test
  void testSelectByName() {
    insertQualityGates();
    assertThat(underTest.selectByName(dbSession, "Balanced").getName()).isEqualTo("Balanced");
    assertThat(underTest.selectByName(dbSession, "Unknown")).isNull();
  }

  @Test
  void testSelectById() {
    insertQualityGates();
    assertThat(underTest.selectByUuid(dbSession, underTest.selectByName(dbSession, "Very strict").getUuid()).getName()).isEqualTo("Very " +
      "strict");
    assertThat(underTest.selectByUuid(dbSession, "-1")).isNull();
  }

  @Test
  void testSelectByUuid() {
    insertQualityGates();
    assertThat(underTest.selectByUuid(dbSession, underTest.selectByName(dbSession, "Very strict").getUuid()).getName()).isEqualTo("Very " +
      "strict");
    assertThat(underTest.selectByUuid(dbSession, "not-existing-uuid")).isNull();
  }

  @Test
  void select_by_project_uuid() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate();
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate();

    QualityGateDto qualityGate3 = db.qualityGates().insertQualityGate();

    db.qualityGates().associateProjectToQualityGate(project, qualityGate1);

    assertThat(underTest.selectByProjectUuid(dbSession, project.getUuid()).getUuid()).isEqualTo(qualityGate1.getUuid());
    assertThat(underTest.selectByProjectUuid(dbSession, "not-existing-uuid")).isNull();
  }

  @Test
  void selectQualityGateFindings_returns_all_quality_gate_details_for_project() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project).setBranchType(BranchType.BRANCH);
    QualityGateDto gate = db.qualityGates().insertQualityGate();
    db.qualityGates().setDefaultQualityGate(gate);

    MetricDto metric1 = db.measures().insertMetric(m -> m.setValueType(Metric.ValueType.PERCENT.name()).setShortName("metric 1"));
    QualityGateConditionDto condition1 = db.qualityGates().addCondition(gate, metric1, c -> c.setErrorThreshold("13"));

    MetricDto metric2 = db.measures().insertMetric(m -> m.setValueType(Metric.ValueType.RATING.name()).setShortName("metric 2"));
    QualityGateConditionDto condition2 = db.qualityGates().addCondition(gate, metric2, c -> c.setErrorThreshold("1"));

    MetricDto metric3 = db.measures().insertMetric(m -> m.setValueType(Metric.ValueType.INT.name()).setShortName("metric 3"));
    QualityGateConditionDto condition3 = db.qualityGates().addCondition(gate, metric3, c -> c.setErrorThreshold("0"));

    db.qualityGates().associateProjectToQualityGate(project, gate);
    db.commit();

    List<QualityGateFindingDto> findings = new ArrayList<>();
    underTest.selectQualityGateFindings(db.getSession(), gate.getUuid(), result -> findings.add(result.getResultObject()));

    // check fields
    assertThat(findings).hasSize(3);
    assertThat(findings.stream().map(QualityGateFindingDto::getDescription).collect(Collectors.toSet())).containsExactlyInAnyOrder(metric1.getShortName(), metric2.getShortName(),
      metric3.getShortName());

    QualityGateFindingDto finding1 = findings.stream().filter(f -> f.getDescription().equals(metric1.getShortName())).findFirst().get();
    validateQualityGateFindingFields(finding1, metric1, condition1);

    QualityGateFindingDto finding2 = findings.stream().filter(f -> f.getDescription().equals(metric2.getShortName())).findFirst().get();
    validateQualityGateFindingFields(finding2, metric2, condition2);

    QualityGateFindingDto finding3 = findings.stream().filter(f -> f.getDescription().equals(metric3.getShortName())).findFirst().get();
    validateQualityGateFindingFields(finding3, metric3, condition3);
  }

  @Test
  void delete() {
    QualityGateDto qualityGate = qualityGateDbTester.insertQualityGate();
    QualityGateDto otherQualityGate = qualityGateDbTester.insertQualityGate();

    underTest.delete(qualityGate, dbSession);
    dbSession.commit();

    assertThat(underTest.selectByUuid(dbSession, qualityGate.getUuid())).isNull();
    assertThat(underTest.selectByUuid(dbSession, otherQualityGate.getUuid())).isNotNull();
  }

  @Test
  void delete_by_uuids() {
    QualityGateDto qualityGate1 = qualityGateDbTester.insertQualityGate();
    QualityGateDto qualityGate2 = qualityGateDbTester.insertQualityGate();

    underTest.deleteByUuids(dbSession, asList(qualityGate1.getUuid(), qualityGate2.getUuid()));
    dbSession.commit();

    assertThat(underTest.selectAll(dbSession).stream())
      .extracting(QualityGateDto::getUuid)
      .doesNotContain(qualityGate1.getUuid(), qualityGate2.getUuid());
  }

  @Test
  void delete_by_uuids_does_nothing_on_empty_list() {
    int nbOfQualityGates = db.countRowsOfTable(dbSession, "quality_gates");
    underTest.deleteByUuids(dbSession, Collections.emptyList());
    dbSession.commit();

    assertThat(db.countRowsOfTable(dbSession, "quality_gates")).isEqualTo(nbOfQualityGates);
  }

  @Test
  void update() {
    QualityGateDto qualityGate = qualityGateDbTester.insertQualityGate(qg -> qg.setName("old name"));

    underTest.update(qualityGate.setName("Not so strict"), dbSession);
    dbSession.commit();

    QualityGateDto reloaded = underTest.selectByUuid(dbSession, qualityGate.getUuid());
    assertThat(reloaded.getName()).isEqualTo("Not so strict");
  }

  @Test
  void selectBuiltIn() {
    QualityGateDto builtInQualityGate = qualityGateDbTester.insertQualityGate(qg -> qg.setName("Built in").setBuiltIn(true));
    QualityGateDto qualityGate = qualityGateDbTester.insertQualityGate(qg -> qg.setName("Random quality gate").setBuiltIn(false));
    dbSession.commit();

    List<QualityGateDto> result = underTest.selectBuiltIn(dbSession);

    assertThat(result)
      .extracting(QualityGateDto::getUuid, QualityGateDto::getName)
      .containsExactly(tuple(builtInQualityGate.getUuid(), builtInQualityGate.getName()));
  }

  @Test
  void ensureOnlySonarWayQualityGatesAreBuiltIn() {
    String builtInQgName = "Sonar Way";
    QualityGateDto builtInQualityGate = qualityGateDbTester.insertQualityGate(qg -> qg.setName(builtInQgName).setBuiltIn(true));
    QualityGateDto qualityGate1 = qualityGateDbTester.insertQualityGate(qg -> qg.setName("QG1").setBuiltIn(true));
    QualityGateDto qualityGate2 = qualityGateDbTester.insertQualityGate(qg -> qg.setName("QG2"));

    underTest.ensureOnlySonarWayQualityGatesAreBuiltIn(dbSession, builtInQgName);
    dbSession.commit();

    QualityGateDto reloaded = underTest.selectByName(dbSession, builtInQgName);
    assertThat(reloaded.getUuid()).isEqualTo(builtInQualityGate.getUuid());
    assertThat(reloaded.getName()).isEqualTo(builtInQualityGate.getName());
    assertThat(reloaded.isBuiltIn()).isTrue();
  }

  private void insertQualityGates() {
    qualityGateDbTester.insertQualityGate(g -> g.setName("Very strict").setBuiltIn(false));
    qualityGateDbTester.insertQualityGate(g -> g.setName("Balanced").setBuiltIn(false));
    qualityGateDbTester.insertQualityGate(g -> g.setName("Lenient").setBuiltIn(false));
  }

  private String getOperatorDescription(String operator, String valueType) {
    if (RATING_VALUE_TYPE.equals(valueType)) {
      return QualityGateFindingDto.RatingType.valueOf(operator).getDescription();
    }

    return QualityGateFindingDto.PercentageType.valueOf(operator).getDescription();
  }

  private String getErrorThreshold(String errorThreshold, String valueType) {
    if (RATING_VALUE_TYPE.equals(valueType)) {
      return QualityGateFindingDto.RatingValue.valueOf(Integer.parseInt(errorThreshold));
    }

    if (PERCENT_VALUE_TYPE.equals(valueType)) {
      return errorThreshold + "%";
    }

    return errorThreshold;
  }

  private void validateQualityGateFindingFields(QualityGateFindingDto finding, MetricDto metric, QualityGateConditionDto condition) {
    assertThat(finding.getDescription()).isEqualTo(metric.getShortName());
    assertThat(finding.getOperatorDescription()).isEqualTo(getOperatorDescription(condition.getOperator(), metric.getValueType()));
    assertThat(finding.getErrorThreshold()).isEqualTo(getErrorThreshold(condition.getErrorThreshold(), metric.getValueType()));
  }
}
