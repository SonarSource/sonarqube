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
import { ColorFilterOption, ColorsLegend } from 'design-system';
import * as React from 'react';
import { translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import { MetricType } from '../../../types/metrics';

export interface ColorRatingsLegendProps {
  className?: string;
  filters: { [rating: number]: boolean };
  onRatingClick: (selection: number) => void;
}

const RATINGS = [1, 2, 3, 4, 5];

export default function ColorRatingsLegend(props: ColorRatingsLegendProps) {
  const { className, filters } = props;

  const ratingsColors = RATINGS.map((rating) => {
    const formattedMeasure = formatMeasure(rating, MetricType.Rating);
    return {
      overlay: translateWithParameters('component_measures.legend.help_x', formattedMeasure),
      ariaLabel: translateWithParameters('component_measures.legend.help_x', formattedMeasure),
      label: formattedMeasure,
      value: rating,
      selected: !filters[rating],
    };
  });

  const handleColorClick = (color: ColorFilterOption) => {
    props.onRatingClick(color.value as number);
  };

  return (
    <ColorsLegend className={className} colors={ratingsColors} onColorClick={handleColorClick} />
  );
}
