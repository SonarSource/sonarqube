/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { translate } from 'sonar-ui-common/helpers/l10n';
import { formatMeasure } from 'sonar-ui-common/helpers/measures';
import { getLeakValue } from '../../../components/measure/utils';
import CoverageRating from '../../../components/ui/CoverageRating';
import { findMeasure } from '../../../helpers/measures';
import { MetricKey } from '../../../types/metrics';

export interface SecurityHotspotsReviewedProps {
  measures: T.MeasureEnhanced[];
  useDiffMetric?: boolean;
}

export default function SecurityHotspotsReviewed(props: SecurityHotspotsReviewedProps) {
  const { measures, useDiffMetric = false } = props;
  const metric = useDiffMetric
    ? MetricKey.new_security_hotspots_reviewed
    : MetricKey.security_hotspots_reviewed;
  const measure = findMeasure(measures, metric);

  let value;
  if (measure) {
    value = useDiffMetric ? getLeakValue(measure) : measure.value;
  }

  return (
    <>
      {value === undefined ? (
        <span aria-label={translate('no_data')} className="overview-measures-empty-value" />
      ) : (
        <>
          <CoverageRating value={value} />
          <span className="huge spacer-left">{formatMeasure(value, 'PERCENT')}</span>
        </>
      )}
      <span className="big-spacer-left">
        {translate('overview.measures.security_hotspots_reviewed')}
      </span>
    </>
  );
}
