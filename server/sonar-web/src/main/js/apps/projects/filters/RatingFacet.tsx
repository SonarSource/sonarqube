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
import { MetricsEnum, MetricsRatingBadge } from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import { MetricType } from '../../../types/metrics';
import { RawQuery } from '../../../types/types';
import { Facet } from '../types';
import RangeFacetBase from './RangeFacetBase';

interface Props {
  facet?: Facet;
  maxFacetValue?: number;
  name: string;
  onQueryChange: (change: RawQuery) => void;
  property: string;
  value?: any;
}

export default function RatingFacet(props: Props) {
  const { facet, maxFacetValue, name, property, value } = props;

  const renderAccessibleLabel = React.useCallback(
    (option: number) => {
      if (option === 1) {
        return translateWithParameters(
          'projects.facets.rating_label_single_x',
          translate('metric_domain', name),
          formatMeasure(option, MetricType.Rating),
        );
      }

      return translateWithParameters(
        'projects.facets.rating_label_multi_x',
        translate('metric_domain', name),
        formatMeasure(option, MetricType.Rating),
      );
    },
    [name],
  );

  return (
    <RangeFacetBase
      facet={facet}
      header={translate('metric_domain', name)}
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

function renderOption(option: number) {
  const ratingFormatted = formatMeasure(option, MetricType.Rating);

  return (
    <MetricsRatingBadge label={ratingFormatted} rating={ratingFormatted as MetricsEnum} size="xs" />
  );
}
