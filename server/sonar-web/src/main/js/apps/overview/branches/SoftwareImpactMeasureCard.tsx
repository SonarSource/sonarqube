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
import styled from '@emotion/styled';
import classNames from 'classnames';
import {
  BasicSeparator,
  LightGreyCard,
  NakedLink,
  TextBold,
  TextSubdued,
  themeColor,
} from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { DEFAULT_ISSUES_QUERY } from '../../../components/shared/utils';
import { formatMeasure } from '../../../helpers/measures';
import { getComponentIssuesUrl } from '../../../helpers/urls';
import {
  SoftwareImpactMeasureData,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../../../types/clean-code-taxonomy';
import { MetricKey, MetricType } from '../../../types/metrics';
import { Component, MeasureEnhanced } from '../../../types/types';
import { softwareQualityToMeasure } from '../utils';
import SoftwareImpactMeasureBreakdownCard from './SoftwareImpactMeasureBreakdownCard';
import SoftwareImpactMeasureRating from './SoftwareImpactMeasureRating';

export interface SoftwareImpactBreakdownCardProps {
  component: Component;
  softwareQuality: SoftwareQuality;
  ratingMetricKey: MetricKey;
  measures: MeasureEnhanced[];
}

export function SoftwareImpactMeasureCard(props: Readonly<SoftwareImpactBreakdownCardProps>) {
  const { component, softwareQuality, ratingMetricKey, measures } = props;

  const intl = useIntl();

  // Find measure for this software quality
  const metricKey = softwareQualityToMeasure(softwareQuality);
  const measureRaw = measures.find((m) => m.metric.key === metricKey);
  const measure = JSON.parse(measureRaw?.value ?? 'null') as SoftwareImpactMeasureData;

  // Find rating measure
  const ratingMeasure = measures.find((m) => m.metric.key === ratingMetricKey);

  const totalLinkHref = getComponentIssuesUrl(component.key, {
    ...DEFAULT_ISSUES_QUERY,
    impactSoftwareQualities: softwareQuality,
  });

  // We highlight the highest severity breakdown card with non-zero count
  const highlightedSeverity =
    measure &&
    [SoftwareImpactSeverity.High, SoftwareImpactSeverity.Medium, SoftwareImpactSeverity.Low].find(
      (severity) => measure[severity] > 0,
    );

  return (
    <LightGreyCard
      data-testid={`overview__software-impact-card-${softwareQuality}`}
      className="sw-w-1/3 sw-overflow-hidden sw-rounded-2 sw-p-4 sw-flex-col"
    >
      <TextBold name={intl.formatMessage({ id: `software_quality.${softwareQuality}` })} />
      <BasicSeparator className="sw--mx-4" />
      <div className="sw-flex sw-flex-col sw-gap-3">
        <div
          className={classNames('sw-flex sw-gap-1 sw-items-end', {
            'sw-opacity-60': !measure,
          })}
        >
          {measure ? (
            <NakedLink
              data-testid={`overview__software-impact-${softwareQuality}`}
              aria-label={intl.formatMessage(
                {
                  id: `overview.measures.software_impact.see_list_of_x_open_issues`,
                },
                {
                  count: measure.total,
                  softwareQuality: intl.formatMessage({
                    id: `software_quality.${softwareQuality}`,
                  }),
                },
              )}
              className="sw-text-xl"
              to={totalLinkHref}
            >
              {formatMeasure(measure.total, MetricType.ShortInteger)}
            </NakedLink>
          ) : (
            <StyledDash className="sw-self-center sw-font-bold" name="-" />
          )}
          <TextSubdued className="sw-body-sm sw-mb-2">
            {intl.formatMessage({ id: 'overview.measures.software_impact.total_open_issues' })}
          </TextSubdued>
          <div className="sw-flex-grow sw-flex sw-justify-end">
            <SoftwareImpactMeasureRating
              softwareQuality={softwareQuality}
              value={ratingMeasure?.value}
            />
          </div>
        </div>
        <div className="sw-flex sw-gap-2">
          {[
            SoftwareImpactSeverity.High,
            SoftwareImpactSeverity.Medium,
            SoftwareImpactSeverity.Low,
          ].map((severity) => (
            <SoftwareImpactMeasureBreakdownCard
              key={severity}
              component={component}
              softwareQuality={softwareQuality}
              value={measure?.[severity]?.toString()}
              severity={severity}
              active={highlightedSeverity === severity}
            />
          ))}
        </div>
      </div>
      {!measure && (
        <>
          <BasicSeparator className="sw--mx-4 sw-mb-0 sw-mt-3" />
          <StyledInfoSection className="sw--ml-4 sw--mr-4 sw--mb-4 sw-text-xs sw-p-4 sw-flex sw-gap-1 sw-flex-wrap">
            <span>{intl.formatMessage({ id: 'overview.project.no_data' })}</span>
            <span>
              {intl.formatMessage({
                id: `overview.run_analysis_to_compute.${component.qualifier}`,
              })}
            </span>
          </StyledInfoSection>
        </>
      )}
    </LightGreyCard>
  );
}

const StyledDash = styled(TextBold)`
  font-size: 36px;
`;

const StyledInfoSection = styled.div`
  background-color: ${themeColor('overviewSoftwareImpactSeverityNeutral')};
`;

export default SoftwareImpactMeasureCard;
