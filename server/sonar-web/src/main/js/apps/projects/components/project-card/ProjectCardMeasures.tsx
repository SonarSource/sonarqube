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
import {
  CoverageIndicator,
  DuplicationsIndicator,
  MetricsLabel,
  MetricsRatingBadge,
  Note,
  PageContentFontWrapper,
} from 'design-system';
import * as React from 'react';
import Measure from '../../../../components/measure/Measure';
import { duplicationRatingConverter } from '../../../../components/measure/utils';
import { translate } from '../../../../helpers/l10n';
import { formatRating } from '../../../../helpers/measures';
import { isDefined } from '../../../../helpers/types';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey, MetricType } from '../../../../types/metrics';
import { Dict } from '../../../../types/types';
import ProjectCardMeasure from './ProjectCardMeasure';

export interface ProjectCardMeasuresProps {
  isNewCode: boolean;
  measures: Dict<string | undefined>;
  componentQualifier: ComponentQualifier;
}

function renderCoverage(props: ProjectCardMeasuresProps) {
  const { measures, isNewCode } = props;
  const coverageMetric = isNewCode ? MetricKey.new_coverage : MetricKey.coverage;

  return (
    <ProjectCardMeasure metricKey={coverageMetric} label={translate('metric.coverage.name')}>
      <div>
        {measures[coverageMetric] && <CoverageIndicator value={measures[coverageMetric]} />}
        <Measure
          metricKey={coverageMetric}
          metricType={MetricType.Percent}
          value={measures[coverageMetric]}
          className="sw-ml-2 sw-body-md-highlight"
        />
      </div>
    </ProjectCardMeasure>
  );
}

function renderDuplication(props: ProjectCardMeasuresProps) {
  const { measures, isNewCode } = props;
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
          metricKey={duplicationMetric}
          metricType={MetricType.Percent}
          value={measures[duplicationMetric]}
          className="sw-ml-2 sw-body-md-highlight"
        />
      </div>
    </ProjectCardMeasure>
  );
}

function renderRatings(props: ProjectCardMeasuresProps) {
  const { isNewCode, measures } = props;

  const measureList = [
    {
      iconLabel: translate('metric.bugs.name'),
      noShrink: true,
      metricKey: isNewCode ? MetricKey.new_bugs : MetricKey.bugs,
      metricRatingKey: isNewCode ? MetricKey.new_reliability_rating : MetricKey.reliability_rating,
      metricType: MetricType.ShortInteger,
    },
    {
      iconLabel: translate('metric.vulnerabilities.name'),
      metricKey: isNewCode ? MetricKey.new_vulnerabilities : MetricKey.vulnerabilities,
      metricRatingKey: isNewCode ? MetricKey.new_security_rating : MetricKey.security_rating,
      metricType: MetricType.ShortInteger,
    },
    {
      iconKey: 'security_hotspots',
      iconLabel: translate('projects.security_hotspots_reviewed'),
      metricKey: isNewCode
        ? MetricKey.new_security_hotspots_reviewed
        : MetricKey.security_hotspots_reviewed,
      metricRatingKey: isNewCode
        ? MetricKey.new_security_review_rating
        : MetricKey.security_review_rating,
      metricType: MetricType.Percent,
    },
    {
      iconLabel: translate('metric.code_smells.name'),
      metricKey: isNewCode ? MetricKey.new_code_smells : MetricKey.code_smells,
      metricRatingKey: isNewCode ? MetricKey.new_maintainability_rating : MetricKey.sqale_rating,
      metricType: MetricType.ShortInteger,
    },
  ];

  return measureList.map((measure) => {
    const { iconLabel, metricKey, metricRatingKey, metricType } = measure;
    const value = formatRating(measures[metricRatingKey]);

    return (
      <ProjectCardMeasure key={metricKey} metricKey={metricKey} label={iconLabel}>
        <MetricsRatingBadge label={metricKey} rating={value as MetricsLabel} />
        <Measure
          metricKey={metricKey}
          metricType={metricType}
          value={measures[metricKey]}
          className="sw-ml-2 sw-body-md-highlight"
        />
      </ProjectCardMeasure>
    );
  });
}

export default function ProjectCardMeasures(props: ProjectCardMeasuresProps) {
  const { isNewCode, measures, componentQualifier } = props;

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
    ...renderRatings(props),
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
