/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.webhook;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;
import org.sonar.api.measures.Metric;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.QualityGate;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProjectAnalysisTest {

  private final CeTask ceTask = new CeTask("id", CeTask.Status.SUCCESS);
  private final Project project = new Project("uuid", "key", "name");
  private final Analysis analysis = new Analysis("analysis_uuid", 1_500L, "sha1");
  private final Branch branch = new Branch(true, "name", Branch.Type.BRANCH);
  private final EvaluatedQualityGate qualityGate = EvaluatedQualityGate.newBuilder()
    .setQualityGate(new QualityGate("id", "name", emptySet()))
    .setStatus(Metric.Level.ERROR)
    .build();
  private final Map<String, String> properties = ImmutableMap.of("a", "b");
  private ProjectAnalysis underTest = new ProjectAnalysis(project, ceTask, analysis, branch, qualityGate, 1L, properties);

  @Test
  public void constructor_throws_NPE_if_project_is_null() {
    assertThatThrownBy(() -> {
      new ProjectAnalysis(null,
        ceTask,
        analysis,
        branch,
        qualityGate,
        1L,
        emptyMap());
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("project can't be null");
  }

  @Test
  public void constructor_throws_NPE_if_properties_is_null() {
    assertThatThrownBy(() -> {
      new ProjectAnalysis(project,
        ceTask,
        analysis,
        branch,
        qualityGate,
        1L,
        null);
    })
      .isInstanceOf(NullPointerException.class)
      .hasMessage("properties can't be null");
  }

  @Test
  public void verify_getters() {
    assertThat(underTest.getCeTask()).containsSame(ceTask);
    assertThat(underTest.getProject()).isSameAs(project);
    assertThat(underTest.getBranch()).containsSame(branch);
    assertThat(underTest.getQualityGate()).containsSame(qualityGate);
    assertThat(underTest.getProperties()).isEqualTo(properties);
    assertThat(underTest.getAnalysis()).contains(analysis);

    ProjectAnalysis underTestWithNulls = new ProjectAnalysis(project, null, null, null, null, null, emptyMap());
    assertThat(underTestWithNulls.getCeTask()).isEmpty();
    assertThat(underTestWithNulls.getBranch()).isEmpty();
    assertThat(underTestWithNulls.getQualityGate()).isEmpty();
    assertThat(underTestWithNulls.getProperties()).isEmpty();
    assertThat(underTestWithNulls.getAnalysis()).isEmpty();
  }

  @Test
  public void defines_equals_based_on_all_fields() {
    assertThat(underTest)
      .isEqualTo(underTest)
      .isEqualTo(new ProjectAnalysis(project, ceTask, analysis, branch, qualityGate, 1L, properties))
      .isNotNull()
      .isNotEqualTo(new Object())
      .isNotEqualTo(new ProjectAnalysis(project, new CeTask("2", CeTask.Status.SUCCESS), analysis, branch, qualityGate, 1L, properties))
      .isNotEqualTo(new ProjectAnalysis(new Project("A", "B", "C"), ceTask, analysis, branch, qualityGate, 1L, properties))
      .isNotEqualTo(new ProjectAnalysis(new Project("A", "B", "C"), ceTask, analysis, branch, qualityGate, 1L, properties))
      .isNotEqualTo(new ProjectAnalysis(project, null, null, null, qualityGate, 1L, properties))
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, null, null, qualityGate, 1L, properties))
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, new Analysis("foo", 1_500L, "sha1"), null, qualityGate, 1L, properties))
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, analysis, null, qualityGate, 1L, properties))
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, analysis, new Branch(false, "B", Branch.Type.BRANCH), qualityGate, 1L, properties))
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, analysis, branch, null, 1L, properties));
    EvaluatedQualityGate otherQualityGate = EvaluatedQualityGate.newBuilder()
      .setQualityGate(new QualityGate("A", "B", emptySet()))
      .setStatus(Metric.Level.ERROR)
      .build();
    assertThat(underTest)
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, analysis, branch, otherQualityGate, 1L, properties))
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, analysis, branch, qualityGate, null, properties))
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, analysis, branch, qualityGate, 2L, properties))
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, analysis, branch, qualityGate, 1L, emptyMap()))
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, analysis, branch, qualityGate, 1L, ImmutableMap.of("A", "B")));
  }

  @Test
  public void defines_hashcode_based_on_all_fields() {
    assertThat(underTest)
      .hasSameHashCodeAs(underTest)
      .hasSameHashCodeAs(new ProjectAnalysis(project, ceTask, analysis, branch, qualityGate, 1L, properties));

    assertThat(underTest.hashCode())
      .isNotEqualTo(new Object().hashCode())
      .isNotEqualTo(new ProjectAnalysis(project, new CeTask("2", CeTask.Status.SUCCESS), analysis, branch, qualityGate, 1L, properties).hashCode())
      .isNotEqualTo(new ProjectAnalysis(new Project("A", "B", "C"), ceTask, analysis, branch, qualityGate, 1L, properties).hashCode())
      .isNotEqualTo(new ProjectAnalysis(new Project("A", "B", "C"), ceTask, analysis, branch, qualityGate, 1L, properties).hashCode())
      .isNotEqualTo(new ProjectAnalysis(project, null, null, null, qualityGate, 1L, properties).hashCode())
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, null, null, qualityGate, 1L, properties).hashCode())
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, new Analysis("foo", 1_500L, "sha1"), null, qualityGate, 1L, properties).hashCode())
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, analysis, null, qualityGate, 1L, properties).hashCode())
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, analysis, new Branch(false, "B", Branch.Type.BRANCH), qualityGate, 1L, properties).hashCode())
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, analysis, branch, null, 1L, properties).hashCode());

    EvaluatedQualityGate otherQualityGate = EvaluatedQualityGate.newBuilder()
      .setQualityGate(new QualityGate("A", "B", emptySet()))
      .setStatus(Metric.Level.ERROR)
      .build();

    assertThat(underTest.hashCode())
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, analysis, branch, otherQualityGate, 1L, properties).hashCode())
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, analysis, branch, this.qualityGate, null, properties).hashCode())
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, analysis, branch, this.qualityGate, 2L, properties).hashCode())
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, analysis, branch, this.qualityGate, 1L, emptyMap()).hashCode())
      .isNotEqualTo(new ProjectAnalysis(project, ceTask, analysis, branch, this.qualityGate, 1L, ImmutableMap.of("B", "C")).hashCode());
  }

  @Test
  public void verify_toString() {
    assertThat(underTest).hasToString(
      "ProjectAnalysis{project=Project{uuid='uuid', key='key', name='name'}, ceTask=CeTask{id='id', status=SUCCESS}, branch=Branch{main=true, name='name', type=BRANCH}, qualityGate=EvaluatedQualityGate{qualityGate=QualityGate{id=id, name='name', conditions=[]}, status=ERROR, evaluatedConditions=[]}, updatedAt=1, properties={a=b}, analysis=Analysis{uuid='analysis_uuid', date=1500, revision=sha1}}");
  }
}
