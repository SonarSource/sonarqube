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
import { Card, CardSeparator, NakedLink, TextBold, TextSubdued, themeBorder } from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { DEFAULT_ISSUES_QUERY } from '../../../components/shared/utils';
import { formatMeasure, formatRating } from '../../../helpers/measures';
import { getComponentIssuesUrl } from '../../../helpers/urls';
import {
  SoftwareImpactMeasureData,
  SoftwareImpactSeverity,
  SoftwareQuality,
} from '../../../types/clean-code-taxonomy';
import { MetricKey, MetricType } from '../../../types/metrics';
import { Component, MeasureEnhanced } from '../../../types/types';
import { getSoftwareImpactSeverityValue, softwareQualityToMeasure } from '../utils';
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
  const measure = JSON.parse(measureRaw?.value ?? 'null') as SoftwareImpactMeasureData | null;

  // Hide this card if there is no measure
  if (!measure) {
    return null;
  }

  // Find rating measure
  const ratingMeasure = measures.find((m) => m.metric.key === ratingMetricKey);
  const ratingLabel = ratingMeasure?.value ? formatRating(ratingMeasure.value) : undefined;

  const totalLinkHref = getComponentIssuesUrl(component.key, {
    ...DEFAULT_ISSUES_QUERY,
    impactSoftwareQualities: softwareQuality,
  });

  // We highlight the highest severity breakdown card with non-zero count if the rating is not A
  const issuesBySeverity = {
    [SoftwareImpactSeverity.High]: measure.high,
    [SoftwareImpactSeverity.Medium]: measure.medium,
    [SoftwareImpactSeverity.Low]: measure.low,
  };
  const shouldHighlightSeverity = measure && (!ratingLabel || ratingLabel !== 'A');
  const highlightedSeverity = shouldHighlightSeverity
    ? Object.entries(issuesBySeverity).find(([_, issuesCount]) => issuesCount > 0)?.[0]
    : null;

  return (
    <StyledCard className="sw-w-1/3 sw-rounded-2 sw-p-4 sw-flex-col">
      <TextBold name={intl.formatMessage({ id: `software_quality.${softwareQuality}` })} />
      <CardSeparator className="sw--mx-4" />
      <div className="sw-flex sw-flex-col sw-gap-3">
        <div className="sw-flex sw-gap-1 sw-items-end">
          <NakedLink
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
              value={getSoftwareImpactSeverityValue(severity, measure)}
              severity={severity}
              active={highlightedSeverity === severity}
            />
          ))}
        </div>
      </div>
    </StyledCard>
  );
}

const StyledCard = styled(Card)`
  border: ${themeBorder('default')};
`;

export default SoftwareImpactMeasureCard;
