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
import { MetricsRatingBadge, Tooltip } from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { formatRating } from '../../../helpers/measures';
import { SoftwareQuality } from '../../../types/clean-code-taxonomy';
import SoftwareImpactRatingTooltip from './SoftwareImpactRatingTooltip';

export interface SoftwareImpactMeasureRatingProps {
  softwareQuality: SoftwareQuality;
  value?: string;
}

export function SoftwareImpactMeasureRating(props: Readonly<SoftwareImpactMeasureRatingProps>) {
  const { softwareQuality, value } = props;

  const intl = useIntl();

  const rating = formatRating(value);

  return (
    <Tooltip
      overlay={SoftwareImpactRatingTooltip({
        rating,
        softwareQuality,
      })}
    >
      <MetricsRatingBadge
        size="lg"
        className="sw-text-sm"
        rating={rating}
        label={intl.formatMessage(
          {
            id: 'metric.has_rating_X',
          },
          { 0: value },
        )}
      />
    </Tooltip>
  );
}

export default SoftwareImpactMeasureRating;
