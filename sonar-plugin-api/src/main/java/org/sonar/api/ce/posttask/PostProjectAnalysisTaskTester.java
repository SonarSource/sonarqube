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
package org.sonar.api.ce.posttask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.utils.Preconditions.checkArgument;
import static org.sonar.api.utils.Preconditions.checkState;

/**
 * This class can be used to test {@link PostProjectAnalysisTask} implementations, see example below:
 * <pre>
 * import static org.assertj.core.api.Assertions.assertThat;
 * import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newCeTaskBuilder;
 * import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newConditionBuilder;
 * import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newProjectBuilder;
 * import static org.sonar.api.ce.posttask.PostProjectAnalysisTaskTester.newQualityGateBuilder;
 * public class CaptorPostProjectAnalysisTaskTest {
 *   private class CaptorPostProjectAnalysisTask implements PostProjectAnalysisTask {
 *     private ProjectAnalysis projectAnalysis;
 *    {@literal @}Override
 *     public void finished(ProjectAnalysis analysis) {
 *       this.projectAnalysis = analysis;
 *     }
 *   }
 *  {@literal @}Test
 *   public void execute_is_passed_a_non_null_ProjectAnalysis_object() {
 *     CaptorPostProjectAnalysisTask postProjectAnalysisTask = new CaptorPostProjectAnalysisTask();
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
 *       .withAnalysisUuid("uuid")
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
 *               .build(QualityGate.EvaluationStatus.OK, "value"))
 *           .build())
 *       .execute();
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
  private static final String KEY_CAN_NOT_BE_NULL = "key cannot be null";
  private static final String NAME_CAN_NOT_BE_NULL = "name cannot be null";

  private final PostProjectAnalysisTask underTest;
  @Nullable
  private Organization organization;
  @CheckForNull
  private CeTask ceTask;
  @CheckForNull
  private Project project;
  @CheckForNull
  private Date date;
  @CheckForNull
  private QualityGate qualityGate;
  @CheckForNull
  private Branch branch;
  private ScannerContext scannerContext;
  private String analysisUuid;
  @CheckForNull
  private Map<String, Object> stats;

  private PostProjectAnalysisTaskTester(PostProjectAnalysisTask underTest) {
    this.underTest = requireNonNull(underTest, "PostProjectAnalysisTask instance cannot be null");
  }

  public static PostProjectAnalysisTaskTester of(PostProjectAnalysisTask underTest) {
    return new PostProjectAnalysisTaskTester(underTest);
  }

  /**
   * @since 7.0
   */
  public static OrganizationBuilder newOrganizationBuilder() {
    return new OrganizationBuilder();
  }

  public static CeTaskBuilder newCeTaskBuilder() {
    return new CeTaskBuilder();
  }

  public static ProjectBuilder newProjectBuilder() {
    return new ProjectBuilder();
  }

  public static BranchBuilder newBranchBuilder() {
    return new BranchBuilder();
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

  /**
   * @since 7.0
   */
  public PostProjectAnalysisTaskTester withOrganization(@Nullable Organization organization) {
    this.organization = organization;
    return this;
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

  public PostProjectAnalysisTaskTester withBranch(@Nullable Branch b) {
    this.branch = b;
    return this;
  }

  /**
   * @since 6.6
   */
  public PostProjectAnalysisTaskTester withAnalysisUuid(@Nullable String analysisUuid) {
    this.analysisUuid = analysisUuid;
    return this;
  }

  public PostProjectAnalysisTask.ProjectAnalysis execute() {
    requireNonNull(ceTask, CE_TASK_CAN_NOT_BE_NULL);
    requireNonNull(project, PROJECT_CAN_NOT_BE_NULL);
    requireNonNull(date, DATE_CAN_NOT_BE_NULL);

    Analysis analysis = null;
    if (analysisUuid != null) {
      analysis = new AnalysisBuilder()
        .setDate(date)
        .setAnalysisUuid(analysisUuid)
        .build();
    }

    PostProjectAnalysisTask.ProjectAnalysis projectAnalysis = new ProjectAnalysisBuilder()
      .setOrganization(organization)
      .setCeTask(ceTask)
      .setProject(project)
      .setBranch(branch)
      .setQualityGate(qualityGate)
      .setAnalysis(analysis)
      .setScannerContext(scannerContext)
      .setDate(date)
      .build();

    stats = new HashMap<>();
    PostProjectAnalysisTask.LogStatistics logStatistics = new PostProjectAnalysisTask.LogStatistics() {
      @Override
      public PostProjectAnalysisTask.LogStatistics add(String key, Object value) {
        requireNonNull(key, "Statistic has null key");
        requireNonNull(value, () -> format("Statistic with key [%s] has null value", key));
        checkArgument(!key.equalsIgnoreCase("time"), "Statistic with key [time] is not accepted");
        checkArgument(!stats.containsKey(key), "Statistic with key [%s] is already present", key);
        stats.put(key, value);
        return this;
      }
    };

    this.underTest.finished(new PostProjectAnalysisTask.Context() {
      @Override
      public PostProjectAnalysisTask.ProjectAnalysis getProjectAnalysis() {
        return projectAnalysis;
      }
      @Override
      public PostProjectAnalysisTask.LogStatistics getLogStatistics() {
        return logStatistics;
      }
    });

    return projectAnalysis;
  }

  public Map<String, Object> getLogStatistics() {
    checkState(stats != null, "execute must be called first");
    return stats;
  }

  public static final class OrganizationBuilder {
    @CheckForNull
    private String name;
    @CheckForNull
    private String key;

    private OrganizationBuilder() {
      // prevents instantiation
    }

    public OrganizationBuilder setName(String name) {
      this.name = requireNonNull(name, NAME_CAN_NOT_BE_NULL);
      return this;
    }

    public OrganizationBuilder setKey(String key) {
      this.key = requireNonNull(key, KEY_CAN_NOT_BE_NULL);
      return this;
    }

    public Organization build() {
      requireNonNull(this.name, NAME_CAN_NOT_BE_NULL);
      requireNonNull(this.key, KEY_CAN_NOT_BE_NULL);
      return new Organization() {
        @Override
        public String getName() {
          return name;
        }

        @Override
        public String getKey() {
          return key;
        }

        @Override
        public String toString() {
          return "Organization{" +
            "name='" + name + '\'' +
            ", key='" + key + '\'' +
            '}';
        }
      };
    }
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

  public static final class BranchBuilder {
    private boolean isMain = true;
    private String name = null;
    private Branch.Type type = Branch.Type.LONG;

    private BranchBuilder() {
      // prevents instantiation outside PostProjectAnalysisTaskTester
    }

    public BranchBuilder setName(@Nullable String s) {
      this.name = s;
      return this;
    }

    public BranchBuilder setType(Branch.Type t) {
      this.type = Objects.requireNonNull(t);
      return this;
    }

    public BranchBuilder setIsMain(boolean b) {
      this.isMain = b;
      return this;
    }

    public Branch build() {
      return new Branch() {

        @Override
        public boolean isMain() {
          return isMain;
        }

        @Override
        public Optional<String> getName() {
          return Optional.ofNullable(name);
        }

        @Override
        public Type getType() {
          return type;
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
    private static final String ERROR_THRESHOLD_CAN_NOT_BE_NULL = "errorThreshold cannot be null";

    private String metricKey;
    private QualityGate.Operator operator;
    private String errorThreshold;

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

    public ConditionBuilder setErrorThreshold(String errorThreshold) {
      this.errorThreshold = requireNonNull(errorThreshold, ERROR_THRESHOLD_CAN_NOT_BE_NULL);
      return this;
    }

    /**
     * @deprecated in 7.6. This method has no longer any effect.
     */
    @Deprecated
    public ConditionBuilder setWarningThreshold(@Nullable String warningThreshold) {
      return this;
    }

    /**
     * @deprecated in 7.6. This method has no longer any effect.
     * Conditions "on leak period" were removed. Use "New X" conditions instead.
     */
    @Deprecated
    public ConditionBuilder setOnLeakPeriod(boolean onLeakPeriod) {
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

        @Deprecated
        @Override
        public String getWarningThreshold() {
          return null;
        }

        /**
         * @deprecated in 7.6. Conditions "on leak period" were removed. Use "New X" conditions instead.
         */
        @Deprecated
        @Override
        public boolean isOnLeakPeriod() {
          return false;
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

        @Deprecated
        @Override
        public String getWarningThreshold() {
          return null;
        }

        /**
         * @deprecated in 7.6. Conditions "on leak period" were removed. Use "New X" conditions instead.
         */
        @Deprecated
        @Override
        public boolean isOnLeakPeriod() {
          return false;
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
            ", value='" + value + '\'' +
            '}';
        }
      };
    }

    private void checkCommonProperties() {
      requireNonNull(metricKey, METRIC_KEY_CAN_NOT_BE_NULL);
      requireNonNull(operator, OPERATOR_CAN_NOT_BE_NULL);
      requireNonNull(errorThreshold, ERROR_THRESHOLD_CAN_NOT_BE_NULL);
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

  public static final class ProjectAnalysisBuilder {
    private Organization organization;
    private CeTask ceTask;
    private Project project;
    private Branch branch;
    private QualityGate qualityGate;
    private Analysis analysis;
    private ScannerContext scannerContext;
    private Date date;

    private ProjectAnalysisBuilder() {
      // prevents instantiation outside PostProjectAnalysisTaskTester
    }

    public ProjectAnalysisBuilder setOrganization(@Nullable Organization organization) {
      this.organization = organization;
      return this;
    }

    public ProjectAnalysisBuilder setCeTask(CeTask ceTask) {
      this.ceTask = ceTask;
      return this;
    }

    public ProjectAnalysisBuilder setProject(Project project) {
      this.project = project;
      return this;
    }

    public ProjectAnalysisBuilder setBranch(@Nullable Branch branch) {
      this.branch = branch;
      return this;
    }

    public ProjectAnalysisBuilder setQualityGate(QualityGate qualityGate) {
      this.qualityGate = qualityGate;
      return this;
    }

    public ProjectAnalysisBuilder setAnalysis(@Nullable Analysis analysis) {
      this.analysis = analysis;
      return this;
    }

    public ProjectAnalysisBuilder setScannerContext(ScannerContext scannerContext) {
      this.scannerContext = scannerContext;
      return this;
    }

    public ProjectAnalysisBuilder setDate(Date date) {
      this.date = date;
      return this;
    }

    public PostProjectAnalysisTask.ProjectAnalysis build() {
      return new PostProjectAnalysisTask.ProjectAnalysis() {
        @Override
        public Optional<Organization> getOrganization() {
          return Optional.ofNullable(organization);
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
        public Optional<Branch> getBranch() {
          return Optional.ofNullable(branch);
        }

        @CheckForNull
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
          return getAnalysis().map(Analysis::getDate);
        }

        @Override
        public Optional<Analysis> getAnalysis() {
          return Optional.ofNullable(analysis);
        }

        @Override
        public ScannerContext getScannerContext() {
          return scannerContext;
        }

        @Override
        public String getScmRevisionId() {
          return null;
        }

        @Override
        public String toString() {
          return "ProjectAnalysis{" +
            "organization=" + organization +
            ", ceTask=" + ceTask +
            ", project=" + project +
            ", date=" + date.getTime() +
            ", analysisDate=" + date.getTime() +
            ", qualityGate=" + qualityGate +
            '}';
        }
      };
    }
  }

  public static final class AnalysisBuilder {
    private String analysisUuid;
    private Date date;
    @Nullable
    private String revision;

    private AnalysisBuilder() {
      // prevents instantiation outside PostProjectAnalysisTaskTester
    }

    public AnalysisBuilder setAnalysisUuid(String analysisUuid) {
      this.analysisUuid = analysisUuid;
      return this;
    }

    public AnalysisBuilder setDate(Date date) {
      this.date = date;
      return this;
    }

    public AnalysisBuilder setRevision(@Nullable String s) {
      this.revision = s;
      return this;
    }

    public Analysis build() {
      return new Analysis() {

        @Override
        public String getAnalysisUuid() {
          return analysisUuid;
        }

        @Override
        public Date getDate() {
          return date;
        }

        @Override
        public Optional<String> getRevision() {
          return Optional.ofNullable(revision);
        }
      };
    }
  }
}
