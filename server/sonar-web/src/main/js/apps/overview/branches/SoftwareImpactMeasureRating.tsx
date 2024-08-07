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
import RatingComponent from '../../../app/components/metrics/RatingComponent';
import { MetricKey } from '../../../sonar-aligned/types/metrics';
import { SoftwareQuality } from '../../../types/clean-code-taxonomy';

export interface SoftwareImpactMeasureRatingProps {
  componentKey: string;
  ratingMetricKey: MetricKey;
  softwareQuality: SoftwareQuality;
}

export function SoftwareImpactMeasureRating(props: Readonly<SoftwareImpactMeasureRatingProps>) {
  const { ratingMetricKey, componentKey } = props;

  // const intl = useIntl();

  // const additionalInfo = (
  //   <SoftwareImpactRatingTooltipContent rating={rating} softwareQuality={softwareQuality} />
  // );

  return (
    <>
      {/* <Tooltip content={additionalInfo}> */}
      <RatingComponent
        size="md"
        className="sw-text-sm"
        ratingMetric={ratingMetricKey}
        componentKey={componentKey}
        // label={intl.formatMessage(
        //   {
        //     id: 'overview.project.software_impact.has_rating',
        //   },
        //   {
        //     softwareQuality: intl.formatMessage({ id: `software_quality.${softwareQuality}` }),
        //     rating,
        //   },
        // )}
      />
      {/* </Tooltip> */}
      {/* The badge is not interactive, so show the tooltip content for screen-readers only */}
      {/* <span className="sw-sr-only">{additionalInfo}</span> */}
    </>
  );
}

export default SoftwareImpactMeasureRating;
