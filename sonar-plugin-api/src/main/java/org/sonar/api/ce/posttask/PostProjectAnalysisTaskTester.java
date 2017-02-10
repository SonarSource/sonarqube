/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.api.ce.posttask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

/**
 * This class can be used to test {@link PostProjectAnalysisTask} implementations, see example below:
 * <pre>
 * import static org.assertj.core.api.Assertions.assertThat;
 * import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newCeTaskBuilder;
 * import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newConditionBuilder;
 * import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newProjectBuilder;
 * import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newQualityGateBuilder;
 *
 * public class CaptorPostProjectAnalysisTaskTest {
 *   private class CaptorPostProjectAnalysisTask implements PostProjectAnalysisTask {
 *     private ProjectAnalysis projectAnalysis;
 *
 *    {@literal @}Override
 *     public void finished(ProjectAnalysis analysis) {
 *       this.projectAnalysis = analysis;
 *     }
 *   }
 *
 *  {@literal @}Test
 *   public void execute_is_passed_a_non_null_ProjectAnalysis_object() {
 *     CaptorPostProjectAnalysisTask postProjectAnalysisTask = new CaptorPostProjectAnalysisTask();
 *
 *     PostProjectAnalysisTaskTester.of(postProjectAnalysisTask)
 *       .withCeTask(
 *           newCeTaskBuilder()
 *               .setId("id")
 *               .setStatus(CeTask.Status.SUCCESS)
 *               .build())
 *       .withProject(
 *         PostProjectAnalysisTaskTester.newProjectBuilder()
 *           .setUuid("uuid")
 *           .setKey("key")
 *           .setName("name")
 *           .build())
 *       .at(new Date())
 *       .withQualityGate(
 *         newQualityGateBuilder()
 *           .setId("id")
 *           .setName("name")
 *           .setStatus(QualityGate.Status.OK)
 *           .add(
 *             newConditionBuilder()
 *               .setMetricKey("metric key")
 *               .setOperator(QualityGate.Operator.GREATER_THAN)
 *               .setErrorThreshold("12")
 *               .setOnLeakPeriod(true)
 *               .build(QualityGate.EvaluationStatus.OK, "value"))
 *           .build())
 *       .execute();
 *
 *     assertThat(postProjectAnalysisTask.projectAnalysis).isNotNull();
 *   }
 * }
 * </pre>
 *
 * @since 5.5
 */
public class PostProjectAnalysisTaskTester {
  private static final String DATE_CAN_NOT_BE_NULL = "date cannot be null";
  private static final String PROJECT_CAN_NOT_BE_NULL = "project cannot be null";
  private static final String CE_TASK_CAN_NOT_BE_NULL = "ceTask cannot be null";
  private static final String STATUS_CAN_NOT_BE_NULL = "status cannot be null";
  private static final String SCANNER_CONTEXT_CAN_NOT_BE_NULL = "scannerContext cannot be null";

  private final PostProjectAnalysisTask underTest;
  @CheckForNull
  private CeTask ceTask;
  @CheckForNull
  private Project project;
  @CheckForNull
  private Date date;
  @CheckForNull
  private QualityGate qualityGate;
  private ScannerContext scannerContext;

  private PostProjectAnalysisTaskTester(PostProjectAnalysisTask underTest) {
    this.underTest = requireNonNull(underTest, "PostProjectAnalysisTask instance cannot be null");
  }

  public static PostProjectAnalysisTaskTester of(PostProjectAnalysisTask underTest) {
    return new PostProjectAnalysisTaskTester(underTest);
  }

  public static CeTaskBuilder newCeTaskBuilder() {
    return new CeTaskBuilder();
  }

  public static ProjectBuilder newProjectBuilder() {
    return new ProjectBuilder();
  }

  public static QualityGateBuilder newQualityGateBuilder() {
    return new QualityGateBuilder();
  }

  public static ConditionBuilder newConditionBuilder() {
    return new ConditionBuilder();
  }

  public static ScannerContextBuilder newScannerContextBuilder() {
    return new ScannerContextBuilder();
  }

  public PostProjectAnalysisTaskTester withCeTask(CeTask ceTask) {
    this.ceTask = requireNonNull(ceTask, CE_TASK_CAN_NOT_BE_NULL);
    return this;
  }

  public PostProjectAnalysisTaskTester withProject(Project project) {
    this.project = requireNonNull(project, PROJECT_CAN_NOT_BE_NULL);
    return this;
  }

  /**
      * @since 6.1
   */
  public PostProjectAnalysisTaskTester withScannerContext(ScannerContext scannerContext) {
    this.scannerContext = requireNonNull(scannerContext, SCANNER_CONTEXT_CAN_NOT_BE_NULL);
    return this;
  }

  public PostProjectAnalysisTaskTester at(Date date) {
    this.date = requireNonNull(date, DATE_CAN_NOT_BE_NULL);
    return this;
  }

  public PostProjectAnalysisTaskTester withQualityGate(@Nullable QualityGate qualityGate) {
    this.qualityGate = qualityGate;
    return this;
  }

  public void execute() {
    this.ceTask = requireNonNull(ceTask, CE_TASK_CAN_NOT_BE_NULL);
    this.project = requireNonNull(project, PROJECT_CAN_NOT_BE_NULL);
    this.date = requireNonNull(date, DATE_CAN_NOT_BE_NULL);

    this.underTest.finished(
      new PostProjectAnalysisTask.ProjectAnalysis() {
        @Override
        public ScannerContext getScannerContext() {
          return scannerContext;
        }

        @Override
        public CeTask getCeTask() {
          return ceTask;
        }

        @Override
        public Project getProject() {
          return project;
        }

        @Override
        public QualityGate getQualityGate() {
          return qualityGate;
        }

        @Override
        public Date getDate() {
          return date;
        }

        @Override
        public Optional<Date> getAnalysisDate() {
          return Optional.of(date);
        }

        @Override
        public String toString() {
          return "ProjectAnalysis{" +
            "ceTask=" + ceTask +
            ", project=" + project +
            ", date=" + date.getTime() +
            ", analysisDate=" + date.getTime() +
            ", qualityGate=" + qualityGate +
            '}';
        }
      });

  }

  public static final class CeTaskBuilder {
    private static final String ID_CAN_NOT_BE_NULL = "id cannot be null";

    @CheckForNull
    private String id;
    @CheckForNull
    private CeTask.Status status;

    private CeTaskBuilder() {
      // prevents instantiation outside PostProjectAnalysisTaskTester
    }

    public CeTaskBuilder setId(String id) {
      this.id = requireNonNull(id, ID_CAN_NOT_BE_NULL);
      return this;
    }

    public CeTaskBuilder setStatus(CeTask.Status status) {
      this.status = requireNonNull(status, STATUS_CAN_NOT_BE_NULL);
      return this;
    }

    public CeTask build() {
      requireNonNull(id, ID_CAN_NOT_BE_NULL);
      requireNonNull(status, STATUS_CAN_NOT_BE_NULL);
      return new CeTask() {
        @Override
        public String getId() {
          return id;
        }

        @Override
        public Status getStatus() {
          return status;
        }

        @Override
        public String toString() {
          return "CeTask{" +
            "id='" + id + '\'' +
            ", status=" + status +
            '}';
        }
      };
    }
  }

  public static final class ProjectBuilder {
    private static final String UUID_CAN_NOT_BE_NULL = "uuid cannot be null";
    private static final String KEY_CAN_NOT_BE_NULL = "key cannot be null";
    private static final String NAME_CAN_NOT_BE_NULL = "name cannot be null";
    private String uuid;
    private String key;
    private String name;

    private ProjectBuilder() {
      // prevents instantiation outside PostProjectAnalysisTaskTester
    }

    public ProjectBuilder setUuid(String uuid) {
      this.uuid = requireNonNull(uuid, UUID_CAN_NOT_BE_NULL);
      return this;
    }

    public ProjectBuilder setKey(String key) {
      this.key = requireNonNull(key, KEY_CAN_NOT_BE_NULL);
      return this;
    }

    public ProjectBuilder setName(String name) {
      this.name = requireNonNull(name, NAME_CAN_NOT_BE_NULL);
      return this;
    }

    public Project build() {
      requireNonNull(uuid, UUID_CAN_NOT_BE_NULL);
      requireNonNull(key, KEY_CAN_NOT_BE_NULL);
      requireNonNull(name, NAME_CAN_NOT_BE_NULL);
      return new Project() {
        @Override
        public String getUuid() {
          return uuid;
        }

        @Override
        public String getKey() {
          return key;
        }

        @Override
        public String getName() {
          return name;
        }

        @Override
        public String toString() {
          return "Project{" +
            "uuid='" + uuid + '\'' +
            ", key='" + key + '\'' +
            ", name='" + name + '\'' +
            '}';
        }

      };
    }
  }

  public static final class QualityGateBuilder {
    private static final String ID_CAN_NOT_BE_NULL = "id cannot be null";
    private static final String NAME_CAN_NOT_BE_NULL = "name cannot be null";

    private String id;
    private String name;
    private QualityGate.Status status;
    private final List<QualityGate.Condition> conditions = new ArrayList<>();

    private QualityGateBuilder() {
      // prevents instantiation outside PostProjectAnalysisTaskTester
    }

    public QualityGateBuilder setId(String id) {
      this.id = requireNonNull(id, ID_CAN_NOT_BE_NULL);
      return this;
    }

    public QualityGateBuilder setName(String name) {
      this.name = requireNonNull(name, NAME_CAN_NOT_BE_NULL);
      return this;
    }

    public QualityGateBuilder setStatus(QualityGate.Status status) {
      this.status = requireNonNull(status, STATUS_CAN_NOT_BE_NULL);
      return this;
    }

    public QualityGateBuilder add(QualityGate.Condition condition) {
      conditions.add(requireNonNull(condition, "condition cannot be null"));
      return this;
    }

    public QualityGateBuilder clearConditions() {
      this.conditions.clear();
      return this;
    }

    public QualityGate build() {
      requireNonNull(id, ID_CAN_NOT_BE_NULL);
      requireNonNull(name, NAME_CAN_NOT_BE_NULL);
      requireNonNull(status, STATUS_CAN_NOT_BE_NULL);

      return new QualityGate() {
        @Override
        public String getId() {
          return id;
        }

        @Override
        public String getName() {
          return name;
        }

        @Override
        public Status getStatus() {
          return status;
        }

        @Override
        public Collection<Condition> getConditions() {
          return conditions;
        }

        @Override
        public String toString() {
          return "QualityGate{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", status=" + status +
            ", conditions=" + conditions +
            '}';
        }
      };
    }
  }

  public static final class ConditionBuilder {
    private static final String METRIC_KEY_CAN_NOT_BE_NULL = "metricKey cannot be null";
    private static final String OPERATOR_CAN_NOT_BE_NULL = "operator cannot be null";

    private String metricKey;
    private QualityGate.Operator operator;
    private String errorThreshold;
    private String warningThreshold;
    private boolean onLeakPeriod;

    private ConditionBuilder() {
      // prevents instantiation outside PostProjectAnalysisTaskTester
    }

    public ConditionBuilder setMetricKey(String metricKey) {
      this.metricKey = requireNonNull(metricKey, METRIC_KEY_CAN_NOT_BE_NULL);
      return this;
    }

    public ConditionBuilder setOperator(QualityGate.Operator operator) {
      this.operator = requireNonNull(operator, OPERATOR_CAN_NOT_BE_NULL);
      return this;
    }

    public ConditionBuilder setErrorThreshold(@Nullable String errorThreshold) {
      this.errorThreshold = errorThreshold;
      return this;
    }

    public ConditionBuilder setWarningThreshold(@Nullable String warningThreshold) {
      this.warningThreshold = warningThreshold;
      return this;
    }

    public ConditionBuilder setOnLeakPeriod(boolean onLeakPeriod) {
      this.onLeakPeriod = onLeakPeriod;
      return this;
    }

    public QualityGate.Condition buildNoValue() {
      checkCommonProperties();
      return new QualityGate.Condition() {
        @Override
        public QualityGate.EvaluationStatus getStatus() {
          return QualityGate.EvaluationStatus.NO_VALUE;
        }

        @Override
        public String getMetricKey() {
          return metricKey;
        }

        @Override
        public QualityGate.Operator getOperator() {
          return operator;
        }

        @Override
        public String getErrorThreshold() {
          return errorThreshold;
        }

        @Override
        public String getWarningThreshold() {
          return warningThreshold;
        }

        @Override
        public boolean isOnLeakPeriod() {
          return onLeakPeriod;
        }

        @Override
        public String getValue() {
          throw new IllegalStateException("There is no value when status is NO_VALUE");
        }

        @Override
        public String toString() {
          return "Condition{" +
            "status=" + QualityGate.EvaluationStatus.NO_VALUE +
            ", metricKey='" + metricKey + '\'' +
            ", operator=" + operator +
            ", errorThreshold='" + errorThreshold + '\'' +
            ", warningThreshold='" + warningThreshold + '\'' +
            ", onLeakPeriod=" + onLeakPeriod +
            '}';
        }
      };
    }

    public QualityGate.Condition build(final QualityGate.EvaluationStatus status, final String value) {
      checkCommonProperties();
      requireNonNull(status, STATUS_CAN_NOT_BE_NULL);
      checkArgument(status != QualityGate.EvaluationStatus.NO_VALUE, "status cannot be NO_VALUE, use method buildNoValue() instead");
      requireNonNull(value, "value cannot be null, use method buildNoValue() instead");
      return new QualityGate.Condition() {
        @Override
        public QualityGate.EvaluationStatus getStatus() {
          return status;
        }

        @Override
        public String getMetricKey() {
          return metricKey;
        }

        @Override
        public QualityGate.Operator getOperator() {
          return operator;
        }

        @Override
        public String getErrorThreshold() {
          return errorThreshold;
        }

        @Override
        public String getWarningThreshold() {
          return warningThreshold;
        }

        @Override
        public boolean isOnLeakPeriod() {
          return onLeakPeriod;
        }

        @Override
        public String getValue() {
          return value;
        }

        @Override
        public String toString() {
          return "Condition{" +
            "status=" + status +
            ", metricKey='" + metricKey + '\'' +
            ", operator=" + operator +
            ", errorThreshold='" + errorThreshold + '\'' +
            ", warningThreshold='" + warningThreshold + '\'' +
            ", onLeakPeriod=" + onLeakPeriod +
            ", value='" + value + '\'' +
            '}';
        }
      };
    }

    private void checkCommonProperties() {
      requireNonNull(metricKey, METRIC_KEY_CAN_NOT_BE_NULL);
      requireNonNull(operator, OPERATOR_CAN_NOT_BE_NULL);
      checkState(errorThreshold != null || warningThreshold != null, "At least one of errorThreshold and warningThreshold must be non null");
    }
  }

  public static final class ScannerContextBuilder {
    private final Map<String, String> properties = new HashMap<>();

    private ScannerContextBuilder() {
      // prevents instantiation outside PostProjectAnalysisTaskTester
    }

    public ScannerContextBuilder addProperties(Map<String, String> map) {
      properties.putAll(map);
      return this;
    }

    public ScannerContext build() {
      return () -> properties;
    }
  }
}
