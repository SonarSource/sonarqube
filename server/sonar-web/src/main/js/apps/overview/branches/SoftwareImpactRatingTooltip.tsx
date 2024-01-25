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
import { useIntl } from 'react-intl';
import { SoftwareImpactSeverity, SoftwareQuality } from '../../../types/clean-code-taxonomy';

export interface SoftwareImpactRatingTooltipProps {
  rating?: string;
  softwareQuality: SoftwareQuality;
}

export function SoftwareImpactRatingTooltip(props: Readonly<SoftwareImpactRatingTooltipProps>) {
  const { rating, softwareQuality } = props;

  const intl = useIntl();

  if (!rating || rating === 'A') {
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

  const softwareQualityLabel = intl.formatMessage({
    id: `software_quality.${softwareQuality}`,
  });
  const severityLabel = intl.formatMessage({
    id: `overview.measures.software_impact.severity.${ratingToWorseSeverity(
      rating,
    )}.improve_tooltip`,
  });

  return (
    <div className="sw-flex sw-flex-col sw-gap-1">
      <span className="sw-font-semibold">
        {intl.formatMessage({
          id: 'overview.measures.software_impact.improve_rating_tooltip.title',
        })}
      </span>

      <span>
        {intl.formatMessage(
          {
            id: 'overview.measures.software_impact.improve_rating_tooltip.content.1',
          },
          {
            softwareQuality: softwareQualityLabel,
            ratingLabel: rating,
            severity: severityLabel,
          },
        )}
      </span>

      <span className="sw-mt-4">
        {intl.formatMessage({
          id: 'overview.measures.software_impact.improve_rating_tooltip.content.2',
        })}
      </span>
    </div>
  );
}

export default SoftwareImpactRatingTooltip;
