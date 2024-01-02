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
import classNames from 'classnames';
import * as React from 'react';
import Tooltip from '../../components/controls/Tooltip';
import { RATING_COLORS } from '../../helpers/constants';
import { translateWithParameters } from '../../helpers/l10n';
import { formatMeasure } from '../../helpers/measures';
import Checkbox from '../controls/Checkbox';
import './ColorBoxLegend.css';

export interface ColorRatingsLegendProps {
  className?: string;
  filters: { [rating: number]: boolean };
  onRatingClick: (selection: number) => void;
}

const RATINGS = [1, 2, 3, 4, 5];

export default function ColorRatingsLegend(props: ColorRatingsLegendProps) {
  const { className, filters } = props;
  return (
    <ul className={classNames('color-box-legend', className)}>
      {RATINGS.map((rating) => (
        <li key={rating}>
          <Tooltip
            overlay={translateWithParameters(
              'component_measures.legend.help_x',
              formatMeasure(rating, 'RATING')
            )}
          >
            <Checkbox
              className="display-flex-center"
              checked={!filters[rating]}
              onCheck={() => props.onRatingClick(rating)}
            >
              <span
                className="color-box-legend-rating little-spacer-left"
                style={{
                  borderColor: RATING_COLORS[rating - 1].stroke,
                  backgroundColor: RATING_COLORS[rating - 1].fillTransparent,
                }}
              >
                {formatMeasure(rating, 'RATING')}
              </span>
            </Checkbox>
          </Tooltip>
        </li>
      ))}
    </ul>
  );
}
