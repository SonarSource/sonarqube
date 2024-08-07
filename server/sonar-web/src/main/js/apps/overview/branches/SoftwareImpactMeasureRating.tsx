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
import { RatingEnum } from 'design-system/lib';
import * as React from 'react';
import { useCallback } from 'react';
import { useIntl } from 'react-intl';
import RatingComponent from '../../../app/components/metrics/RatingComponent';
import { MetricKey } from '../../../sonar-aligned/types/metrics';
import { SoftwareImpactSeverity, SoftwareQuality } from '../../../types/clean-code-taxonomy';

export interface SoftwareImpactMeasureRatingProps {
  componentKey: string;
  ratingMetricKey: MetricKey;
  softwareQuality: SoftwareQuality;
}

export function SoftwareImpactMeasureRating(props: Readonly<SoftwareImpactMeasureRatingProps>) {
  const { ratingMetricKey, componentKey, softwareQuality } = props;

  const intl = useIntl();

  const getSoftwareImpactRatingTooltip = useCallback(
    (rating: RatingEnum) => {
      if (rating === undefined) {
        return null;
      }

      function ratingToWorseSeverity(rating: string): SoftwareImpactSeverity {
        return (
          {
            B: SoftwareImpactSeverity.Low,
            C: SoftwareImpactSeverity.Medium,
            D: SoftwareImpactSeverity.High,
            E: SoftwareImpactSeverity.High,
          }[rating] ?? SoftwareImpactSeverity.Low
        );
      }

      const maintainabilityMessageId =
        softwareQuality === SoftwareQuality.Maintainability
          ? `.${SoftwareQuality.Maintainability}`
          : '';

      const softwareQualityLabel = intl.formatMessage({
        id: `software_quality.${softwareQuality}`,
      });
      const severityLabel = intl.formatMessage({
        id: `overview.measures.software_impact.severity.${ratingToWorseSeverity(
          rating,
        )}.improve_tooltip`,
      });

      return intl.formatMessage(
        {
          id:
            rating === 'A'
              ? `overview.measures.software_impact.improve_rating_tooltip${maintainabilityMessageId}.A`
              : `overview.measures.software_impact.improve_rating_tooltip${maintainabilityMessageId}`,
        },
        {
          softwareQuality: softwareQualityLabel,
          _softwareQuality: softwareQualityLabel.toLowerCase(),
          ratingLabel: rating,
          severity: severityLabel,
        },
      );
    },
    [intl, softwareQuality],
  );

  const getLabel = useCallback(
    (rating: RatingEnum) =>
      intl.formatMessage(
        {
          id: 'overview.project.software_impact.has_rating',
        },
        {
          softwareQuality: intl.formatMessage({ id: `software_quality.${softwareQuality}` }),
          rating,
        },
      ),
    [intl, softwareQuality],
  );

  return (
    <RatingComponent
      size="md"
      className="sw-text-sm"
      ratingMetric={ratingMetricKey}
      componentKey={componentKey}
      getLabel={getLabel}
      getTooltip={getSoftwareImpactRatingTooltip}
    />
  );
}

export default SoftwareImpactMeasureRating;
