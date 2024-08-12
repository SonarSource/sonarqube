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
import { keyBy } from 'lodash';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import { isDiffMetric } from '../../../helpers/measures';
import { mockMeasure } from '../../../helpers/testMocks';
import { SoftwareImpactSeverity } from '../../../types/clean-code-taxonomy';
import { IssueDeprecatedStatus, IssueType, RawIssue } from '../../../types/issues';
import { Measure } from '../../../types/types';
import { ComponentTree } from './components';
import { IssueData } from './issues';
import { listAllComponent, listAllComponentTrees } from './utils';

const MAX_RATING = 5;
export type MeasureRecords = Record<string, Record<string, Measure>>;

export function mockFullMeasureData(tree: ComponentTree, issueList: IssueData[]) {
  const measures: MeasureRecords = {};
  listAllComponentTrees(tree).forEach((tree) => {
    measures[tree.component.key] = keyBy(
      Object.values(MetricKey).map((metricKey) => mockComponentMeasure(tree, issueList, metricKey)),
      'metric',
    );
  });
  return measures;
}

function mockComponentMeasure(tree: ComponentTree, issueList: IssueData[], metricKey: MetricKey) {
  const componentKeys = listAllComponent(tree).map(({ key }) => key);

  switch (metricKey) {
    case MetricKey.ncloc:
      return mockMeasure({
        metric: metricKey,
        value: '16000',
      });

    case MetricKey.ncloc_language_distribution:
      return mockMeasure({
        metric: metricKey,
        value: 'java=10000;javascript=5000;css=1000',
      });

    case MetricKey.security_issues:
      return mockMeasure({
        metric: metricKey,
        value: JSON.stringify({
          total: 1,
          [SoftwareImpactSeverity.High]: 0,
          [SoftwareImpactSeverity.Medium]: 1,
          [SoftwareImpactSeverity.Low]: 0,
        }),
      });

    case MetricKey.new_security_issues:
      return mockMeasure({
        metric: metricKey,
        period: {
          index: 1,
          value: JSON.stringify({
            total: 3,
            [SoftwareImpactSeverity.High]: 2,
            [SoftwareImpactSeverity.Medium]: 0,
            [SoftwareImpactSeverity.Low]: 1,
          }),
        },
        value: undefined,
      });

    case MetricKey.reliability_issues:
      return mockMeasure({
        metric: metricKey,
        value: JSON.stringify({
          total: 3,
          [SoftwareImpactSeverity.High]: 0,
          [SoftwareImpactSeverity.Medium]: 2,
          [SoftwareImpactSeverity.Low]: 1,
        }),
      });

    case MetricKey.new_reliability_issues:
      return mockMeasure({
        metric: metricKey,
        period: {
          index: 1,
          value: JSON.stringify({
            total: 2,
            [SoftwareImpactSeverity.High]: 0,
            [SoftwareImpactSeverity.Medium]: 1,
            [SoftwareImpactSeverity.Low]: 1,
          }),
        },
        value: undefined,
      });

    case MetricKey.maintainability_issues:
      return mockMeasure({
        metric: metricKey,
        value: JSON.stringify({
          total: 2,
          [SoftwareImpactSeverity.High]: 0,
          [SoftwareImpactSeverity.Medium]: 0,
          [SoftwareImpactSeverity.Low]: 1,
        }),
      });

    case MetricKey.new_maintainability_issues:
      return mockMeasure({
        metric: metricKey,
        period: {
          index: 1,
          value: JSON.stringify({
            total: 5,
            [SoftwareImpactSeverity.High]: 2,
            [SoftwareImpactSeverity.Medium]: 2,
            [SoftwareImpactSeverity.Low]: 1,
          }),
        },
        value: undefined,
      });
  }

  const issues = issueList
    .map(({ issue }) => issue)
    .filter(({ component }) => componentKeys.includes(component))
    .filter(({ status }) =>
      [
        IssueDeprecatedStatus.Open,
        IssueDeprecatedStatus.Reopened,
        IssueDeprecatedStatus.Confirmed,
      ].includes(status as IssueDeprecatedStatus),
    );

  if (isIssueType(metricKey)) {
    switch (metricKey) {
      case MetricKey.bugs:
        return mockMeasure({
          metric: metricKey,
          period: undefined,
          value: String(issues.filter(({ type }) => type === IssueType.Bug).length),
        });

      case MetricKey.new_bugs:
        return mockMeasure({
          metric: metricKey,
          period: {
            index: 0,
            value: String(issues.filter(({ type }) => type === IssueType.Bug).length),
          },
          value: undefined,
        });

      case MetricKey.code_smells:
        return mockMeasure({
          metric: metricKey,
          period: undefined,
          value: String(issues.filter(({ type }) => type === IssueType.CodeSmell).length),
        });

      case MetricKey.new_code_smells:
        return mockMeasure({
          metric: metricKey,
          period: {
            index: 0,
            value: String(issues.filter(({ type }) => type === IssueType.CodeSmell).length),
          },
          value: undefined,
        });

      case MetricKey.vulnerabilities:
        return mockMeasure({
          metric: metricKey,
          period: undefined,
          value: String(issues.filter(({ type }) => type === IssueType.Vulnerability).length),
        });

      case MetricKey.new_vulnerabilities:
        return mockMeasure({
          metric: metricKey,
          period: {
            index: 0,
            value: String(issues.filter(({ type }) => type === IssueType.Vulnerability).length),
          },
          value: undefined,
        });

      case MetricKey.open_issues:
        return mockMeasure({
          metric: metricKey,
          value: String(issues.length),
        });
    }
  }

  if (isIssueRelatedRating(metricKey)) {
    switch (metricKey) {
      case MetricKey.reliability_rating:
        return mockMeasure({
          metric: metricKey,
          period: undefined,
          ...computeRating(issues, IssueType.Bug),
        });

      case MetricKey.software_quality_reliability_rating:
        return mockMeasure({
          metric: metricKey,
          period: undefined,
          ...computeRating(issues, IssueType.Bug),
        });

      case MetricKey.new_reliability_rating:
        return mockMeasure({
          metric: metricKey,
          period: {
            index: 0,
            ...computeRating(issues, IssueType.Bug),
          },
          value: undefined,
        });

      case MetricKey.new_software_quality_reliability_rating:
        return mockMeasure({
          metric: metricKey,
          period: {
            index: 0,
            ...computeRating(issues, IssueType.Bug),
          },
          value: undefined,
        });

      case MetricKey.sqale_rating:
        return mockMeasure({
          metric: metricKey,
          period: undefined,
          ...computeRating(issues, IssueType.CodeSmell),
        });

      case MetricKey.software_quality_maintainability_rating:
        return mockMeasure({
          metric: metricKey,
          period: undefined,
          ...computeRating(issues, IssueType.CodeSmell),
        });

      case MetricKey.new_maintainability_rating:
        return mockMeasure({
          metric: metricKey,
          period: {
            index: 0,
            ...computeRating(issues, IssueType.CodeSmell),
          },
          value: undefined,
        });

      case MetricKey.new_software_quality_maintainability_rating:
        return mockMeasure({
          metric: metricKey,
          period: {
            index: 0,
            ...computeRating(issues, IssueType.CodeSmell),
          },
          value: undefined,
        });

      case MetricKey.security_rating:
        return mockMeasure({
          metric: metricKey,
          period: undefined,
          ...computeRating(issues, IssueType.Vulnerability),
        });

      case MetricKey.software_quality_security_rating:
        return mockMeasure({
          metric: metricKey,
          period: undefined,
          ...computeRating(issues, IssueType.Vulnerability),
        });

      case MetricKey.new_security_rating:
        return mockMeasure({
          metric: metricKey,
          period: {
            index: 0,
            ...computeRating(issues, IssueType.Vulnerability),
          },
          value: undefined,
        });

      case MetricKey.new_software_quality_security_rating:
        return mockMeasure({
          metric: metricKey,
          period: {
            index: 0,
            ...computeRating(issues, IssueType.Vulnerability),
          },
          value: undefined,
        });
    }
  }

  // Defaults.
  if (isDiffMetric(metricKey)) {
    return mockMeasure({
      metric: metricKey,
      value: undefined,
    });
  }
  return mockMeasure({
    metric: metricKey,
    period: undefined,
  });
}

export function getMetricTypeFromKey(metricKey: string) {
  if (/(coverage|duplication)$/.test(metricKey)) {
    return MetricType.Percent;
  } else if (metricKey.includes('_rating')) {
    return MetricType.Rating;
  } else if (
    [
      MetricKey.reliability_issues,
      MetricKey.new_reliability_issues,
      MetricKey.security_issues,
      MetricKey.new_security_issues,
      MetricKey.maintainability_issues,
      MetricKey.new_maintainability_issues,
    ].includes(metricKey as MetricKey)
  ) {
    return MetricType.Data;
  }
  return MetricType.Integer;
}

function isIssueType(metricKey: MetricKey) {
  return [
    MetricKey.bugs,
    MetricKey.new_bugs,
    MetricKey.code_smells,
    MetricKey.new_code_smells,
    MetricKey.vulnerabilities,
    MetricKey.new_vulnerabilities,
    MetricKey.open_issues,
  ].includes(metricKey);
}

function isIssueRelatedRating(metricKey: MetricKey) {
  return [
    MetricKey.reliability_rating,
    MetricKey.software_quality_reliability_rating,
    MetricKey.new_reliability_rating,
    MetricKey.new_software_quality_reliability_rating,
    MetricKey.sqale_rating,
    MetricKey.software_quality_maintainability_rating,
    MetricKey.new_maintainability_rating,
    MetricKey.new_software_quality_maintainability_rating,
    MetricKey.security_rating,
    MetricKey.software_quality_security_rating,
    MetricKey.new_security_rating,
    MetricKey.new_software_quality_security_rating,
  ].includes(metricKey);
}

/**
 * Ratings are not only based on the number of issues, but also their severity, and sometimes their
 * ratio to the LOC. But using the number will suffice as an approximation in our tests.
 */
function computeRating(issues: RawIssue[], type: IssueType) {
  const value = Math.max(Math.min(issues.filter((i) => i.type === type).length, MAX_RATING), 1);
  return {
    value: `${value}.0`,
    bestValue: value === 1,
  };
}
