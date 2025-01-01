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

import * as React from 'react';
import {
  CoverageIndicator,
  DuplicationsIndicator,
  Note,
  PageContentFontWrapper,
} from '~design-system';
import Measure from '~sonar-aligned/components/measure/Measure';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { MetricKey, MetricType } from '~sonar-aligned/types/metrics';
import RatingComponent from '../../../../app/components/metrics/RatingComponent';
import { duplicationRatingConverter } from '../../../../components/measure/utils';
import { translate } from '../../../../helpers/l10n';
import { isDefined } from '../../../../helpers/types';
import { useStandardExperienceModeQuery } from '../../../../queries/mode';
import { Dict } from '../../../../types/types';
import ProjectCardMeasure from './ProjectCardMeasure';

export interface ProjectCardMeasuresProps {
  // eslint-disable-next-line react/no-unused-prop-types
  componentKey: string;
  componentQualifier: ComponentQualifier;
  isNewCode: boolean;
  measures: Dict<string | undefined>;
}

function renderNewIssues(props: ProjectCardMeasuresProps) {
  const { measures, isNewCode, componentKey } = props;

  if (!isNewCode) {
    return null;
  }

  return (
    <ProjectCardMeasure
      metricKey={MetricKey.new_violations}
      label={translate(`metric.${MetricKey.new_violations}.description`)}
    >
      <Measure
        componentKey={componentKey}
        metricKey={MetricKey.new_violations}
        metricType={MetricType.ShortInteger}
        value={measures[MetricKey.new_violations]}
        className="sw-ml-2 sw-typo-lg-semibold"
      />
    </ProjectCardMeasure>
  );
}

function renderCoverage(props: ProjectCardMeasuresProps) {
  const { measures, isNewCode, componentKey } = props;
  const coverageMetric = isNewCode ? MetricKey.new_coverage : MetricKey.coverage;

  return (
    <ProjectCardMeasure metricKey={coverageMetric} label={translate('metric.coverage.name')}>
      <div>
        {measures[coverageMetric] && <CoverageIndicator value={measures[coverageMetric]} />}
        <Measure
          componentKey={componentKey}
          metricKey={coverageMetric}
          metricType={MetricType.Percent}
          value={measures[coverageMetric]}
          className="sw-ml-2 sw-typo-lg-semibold"
        />
      </div>
    </ProjectCardMeasure>
  );
}

function renderDuplication(props: ProjectCardMeasuresProps) {
  const { measures, isNewCode, componentKey } = props;
  const duplicationMetric = isNewCode
    ? MetricKey.new_duplicated_lines_density
    : MetricKey.duplicated_lines_density;

  const rating =
    measures[duplicationMetric] !== undefined
      ? duplicationRatingConverter(Number(measures[duplicationMetric]))
      : undefined;

  return (
    <ProjectCardMeasure
      metricKey={duplicationMetric}
      label={translate('metric.duplicated_lines_density.short_name')}
    >
      <div>
        {measures[duplicationMetric] != null && <DuplicationsIndicator rating={rating} />}
        <Measure
          componentKey={componentKey}
          metricKey={duplicationMetric}
          metricType={MetricType.Percent}
          value={measures[duplicationMetric]}
          className="sw-ml-2 sw-typo-lg-semibold"
        />
      </div>
    </ProjectCardMeasure>
  );
}

function renderRatings(props: ProjectCardMeasuresProps, isStandardMode: boolean) {
  const { isNewCode, measures, componentKey } = props;

  const measuresByCodeLeak = isNewCode
    ? []
    : [
        {
          iconLabel: translate(
            `metric.${isStandardMode ? MetricKey.vulnerabilities : MetricKey.software_quality_security_issues}.short_name`,
          ),
          noShrink: true,
          metricKey:
            isStandardMode || measures[MetricKey.software_quality_security_issues] === undefined
              ? MetricKey.vulnerabilities
              : MetricKey.software_quality_security_issues,
          metricRatingKey: MetricKey.security_rating,
          metricType: MetricType.ShortInteger,
        },
        {
          iconLabel: translate(
            `metric.${isStandardMode ? MetricKey.bugs : MetricKey.software_quality_reliability_issues}.short_name`,
          ),
          metricKey:
            isStandardMode || measures[MetricKey.software_quality_reliability_issues] === undefined
              ? MetricKey.bugs
              : MetricKey.software_quality_reliability_issues,
          metricRatingKey: MetricKey.reliability_rating,
          metricType: MetricType.ShortInteger,
        },
        {
          iconLabel: translate(
            `metric.${isStandardMode ? MetricKey.code_smells : MetricKey.software_quality_maintainability_issues}.short_name`,
          ),
          metricKey:
            isStandardMode ||
            measures[MetricKey.software_quality_maintainability_issues] === undefined
              ? MetricKey.code_smells
              : MetricKey.software_quality_maintainability_issues,
          metricRatingKey: MetricKey.sqale_rating,
          metricType: MetricType.ShortInteger,
        },
      ];

  const measureList = [
    ...measuresByCodeLeak,
    {
      iconKey: MetricKey.security_hotspots,
      iconLabel: translate('projects.security_hotspots_reviewed'),
      metricKey: isNewCode
        ? MetricKey.new_security_hotspots_reviewed
        : MetricKey.security_hotspots_reviewed,
      metricRatingKey: isNewCode
        ? MetricKey.new_security_review_rating
        : MetricKey.security_review_rating,
      metricType: MetricType.Percent,
    },
  ];

  return measureList.map((measure) => {
    const { iconLabel, metricKey, metricRatingKey, metricType } = measure;

    return (
      <ProjectCardMeasure key={metricKey} metricKey={metricKey} label={iconLabel}>
        <RatingComponent ratingMetric={metricRatingKey} componentKey={componentKey} />
        <Measure
          componentKey={componentKey}
          metricKey={metricKey}
          metricType={metricType}
          value={measures[metricKey]}
          className="sw-ml-2 sw-typo-lg-semibold"
        />
      </ProjectCardMeasure>
    );
  });
}

export default function ProjectCardMeasures(props: ProjectCardMeasuresProps) {
  const { isNewCode, measures, componentQualifier } = props;
  const { data: isStandardMode } = useStandardExperienceModeQuery();

  const { ncloc } = measures;

  if (!isNewCode && !ncloc) {
    return (
      <Note className="sw-py-4">
        {componentQualifier === ComponentQualifier.Application
          ? translate('portfolio.app.empty')
          : translate('overview.project.main_branch_empty')}
      </Note>
    );
  }

  const measureList = [
    renderNewIssues(props),
    ...renderRatings(props, !!isStandardMode),
    renderCoverage(props),
    renderDuplication(props),
  ].filter(isDefined);

  return (
    <PageContentFontWrapper className="sw-flex sw-gap-8">
      {measureList.map((measure, i) => (
        // eslint-disable-next-line react/no-array-index-key
        <React.Fragment key={i}>{measure}</React.Fragment>
      ))}
    </PageContentFontWrapper>
  );
}
