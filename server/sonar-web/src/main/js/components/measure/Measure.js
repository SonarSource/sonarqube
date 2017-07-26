/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import Rating from '../ui/Rating';
import Level from '../ui/Level';
import Tooltips from '../controls/Tooltip';
import { formatMeasure, isDiffMetric } from '../../helpers/measures';
import { formatLeak, getRatingTooltip } from './utils';
import type { MeasureEnhanced } from './types';

type Props = {
  className?: string,
  measure: MeasureEnhanced,
  decimals?: ?number
};

export default class Measure extends React.PureComponent {
  props: Props;

  renderRating() {
    const { measure } = this.props;
    const metric = measure.metric;
    const value = isDiffMetric(metric.key) ? measure.leak : measure.value;
    const tooltip = getRatingTooltip(metric.key, value);
    const rating = <Rating value={value} />;

    if (tooltip) {
      return (
        <Tooltips overlay={tooltip}>
          <span className={this.props.className}>
            {rating}
          </span>
        </Tooltips>
      );
    }

    return rating;
  }

  render() {
    const { className, decimals, measure } = this.props;
    const metric = measure.metric;

    if (metric.type === 'RATING') {
      return this.renderRating();
    }

    if (metric.type === 'LEVEL') {
      return <Level className={className} level={measure.value} />;
    }

    const formattedValue = isDiffMetric(metric.key)
      ? formatLeak(measure.leak, metric, { decimals })
      : formatMeasure(measure.value, metric.type, { decimals });
    return (
      <span className={className}>
        {formattedValue != null ? formattedValue : '–'}
      </span>
    );
  }
}
