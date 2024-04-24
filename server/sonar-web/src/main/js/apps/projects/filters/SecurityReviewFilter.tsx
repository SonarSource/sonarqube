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
import { MetricsRatingBadge, RatingEnum } from 'design-system';
import * as React from 'react';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { MetricType } from '../../../types/metrics';
import { Dict, RawQuery } from '../../../types/types';
import { Facet } from '../types';
import RangeFacetBase from './RangeFacetBase';

export interface Props {
  facet?: Facet;
  maxFacetValue?: number;
  onQueryChange: (change: RawQuery) => void;
  property?: string;
  value?: any;
}

const labels: Dict<string> = {
  1: '≥ 80%',
  2: '70% - 80%',
  3: '50% - 70%',
  4: '30% - 50%',
  5: '< 30%',
};

export default function SecurityReviewFilter(props: Props) {
  const { facet, maxFacetValue, property = 'security_review', value } = props;

  return (
    <RangeFacetBase
      facet={facet}
      header={translate('metric_domain.SecurityReview')}
      highlightUnder={1}
      maxFacetValue={maxFacetValue}
      onQueryChange={props.onQueryChange}
      options={[1, 2, 3, 4, 5]}
      property={property}
      renderAccessibleLabel={renderAccessibleLabel}
      renderOption={renderOption}
      value={value}
    />
  );
}

function renderAccessibleLabel(option: number) {
  if (option === 1) {
    return translateWithParameters(
      'projects.facets.rating_label_single_x',
      translate('metric_domain.SecurityReview'),
      formatMeasure(option, MetricType.Rating),
    );
  }

  return translateWithParameters(
    'projects.facets.rating_label_multi_x',
    translate('metric_domain.SecurityReview'),
    formatMeasure(option, MetricType.Rating),
  );
}

function renderOption(option: number) {
  const ratingFormatted = formatMeasure(option, MetricType.Rating);

  return (
    <div className="sw-flex sw-items-center">
      <MetricsRatingBadge
        label={ratingFormatted}
        rating={ratingFormatted as RatingEnum}
        size="xs"
      />
      <span className="sw-ml-2">{labels[option]}</span>
    </div>
  );
}
