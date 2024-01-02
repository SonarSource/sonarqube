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
import classNames from 'classnames';
import { differenceInMilliseconds } from 'date-fns';
import * as React from 'react';
import DateTimeFormatter from '../../../../components/intl/DateTimeFormatter';
import Measure from '../../../../components/measure/Measure';
import CoverageRating from '../../../../components/ui/CoverageRating';
import DuplicationsRating from '../../../../components/ui/DuplicationsRating';
import Rating from '../../../../components/ui/Rating';
import { parseDate } from '../../../../helpers/dates';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { isDefined } from '../../../../helpers/types';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey } from '../../../../types/metrics';
import { Dict } from '../../../../types/types';
import { formatDuration } from '../../utils';
import ProjectCardMeasure from './ProjectCardMeasure';

export interface ProjectCardMeasuresProps {
  isNewCode: boolean;
  measures: Dict<string | undefined>;
  componentQualifier: ComponentQualifier;
  newCodeStartingDate?: string;
}

function renderCoverage(props: ProjectCardMeasuresProps) {
  const { measures, isNewCode } = props;
  const coverageMetric = isNewCode ? MetricKey.new_coverage : MetricKey.coverage;

  return (
    <ProjectCardMeasure metricKey={coverageMetric} label={translate('metric.coverage.name')}>
      <div className="display-flex-center">
        <Measure
          className="big"
          metricKey={coverageMetric}
          metricType="PERCENT"
          value={measures[coverageMetric]}
        />
        {measures[coverageMetric] && (
          <span className="spacer-left project-card-measure-secondary-info">
            <CoverageRating value={measures[coverageMetric]} />
          </span>
        )}
      </div>
    </ProjectCardMeasure>
  );
}

function renderDuplication(props: ProjectCardMeasuresProps) {
  const { measures, isNewCode } = props;
  const duplicationMetric = isNewCode
    ? MetricKey.new_duplicated_lines_density
    : MetricKey.duplicated_lines_density;

  return (
    <ProjectCardMeasure
      metricKey={duplicationMetric}
      label={translate('metric.duplicated_lines_density.short_name')}
    >
      <div className="display-flex-center">
        <Measure
          className="big"
          metricKey={duplicationMetric}
          metricType="PERCENT"
          value={measures[duplicationMetric]}
        />
        {measures[duplicationMetric] != null && (
          <span className="spacer-left project-card-measure-secondary-info">
            <DuplicationsRating value={Number(measures[duplicationMetric])} />
          </span>
        )}
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
      metricType: 'SHORT_INT',
    },
    {
      iconLabel: translate('metric.vulnerabilities.name'),
      metricKey: isNewCode ? MetricKey.new_vulnerabilities : MetricKey.vulnerabilities,
      metricRatingKey: isNewCode ? MetricKey.new_security_rating : MetricKey.security_rating,
      metricType: 'SHORT_INT',
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
      metricType: 'PERCENT',
    },
    {
      iconLabel: translate('metric.code_smells.name'),
      metricKey: isNewCode ? MetricKey.new_code_smells : MetricKey.code_smells,
      metricRatingKey: isNewCode ? MetricKey.new_maintainability_rating : MetricKey.sqale_rating,
      metricType: 'SHORT_INT',
    },
  ];

  return measureList.map((measure) => {
    const { iconKey, iconLabel, metricKey, metricRatingKey, metricType, noShrink } = measure;

    return (
      <ProjectCardMeasure
        className={classNames({ 'flex-0': noShrink })}
        key={metricKey}
        metricKey={metricKey}
        iconKey={iconKey}
        label={iconLabel}
      >
        <Measure
          className="spacer-right big project-card-measure-secondary-info"
          metricKey={metricKey}
          metricType={metricType}
          value={measures[metricKey]}
        />
        <span className="big">
          <Rating value={measures[metricRatingKey]} />
        </span>
      </ProjectCardMeasure>
    );
  });
}

export default function ProjectCardMeasures(props: ProjectCardMeasuresProps) {
  const { isNewCode, measures, componentQualifier, newCodeStartingDate } = props;

  const { ncloc } = measures;

  if (!isNewCode && !ncloc) {
    return (
      <div className="note big-spacer-top">
        {componentQualifier === ComponentQualifier.Application
          ? translate('portfolio.app.empty')
          : translate('overview.project.main_branch_empty')}
      </div>
    );
  }

  const newCodeTimespan = newCodeStartingDate
    ? differenceInMilliseconds(Date.now(), parseDate(newCodeStartingDate))
    : 0;

  const measureList = [
    ...renderRatings(props),
    renderCoverage(props),
    renderDuplication(props),
  ].filter(isDefined);

  return (
    <>
      {isNewCode && newCodeTimespan !== undefined && newCodeStartingDate && (
        <DateTimeFormatter date={newCodeStartingDate}>
          {(formattedNewCodeStartingDate) => (
            <p className="spacer-top spacer-bottom" title={formattedNewCodeStartingDate}>
              {translateWithParameters(
                'projects.new_code_period_x',
                formatDuration(newCodeTimespan)
              )}
            </p>
          )}
        </DateTimeFormatter>
      )}
      <div className="display-flex-row display-flex-space-between">
        {measureList.map((measure, i) => (
          // eslint-disable-next-line react/no-array-index-key
          <React.Fragment key={i}>
            {i > 0 && <span className="bordered-left little-spacer" />}
            {measure}
          </React.Fragment>
        ))}
      </div>
    </>
  );
}
