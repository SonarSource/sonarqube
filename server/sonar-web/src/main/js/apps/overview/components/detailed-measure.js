/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';

import { formatMeasure, formatMeasureVariation, localizeMetric } from '../../../helpers/measures';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { getShortType } from '../helpers/metrics';


export const DetailedMeasure = React.createClass({
  renderLeak () {
    if (!this.props.leakPeriodDate) {
      return null;
    }
    let leak = this.props.leak[this.props.metric];
    let formatted = leak != null ? formatMeasureVariation(leak, getShortType(this.props.type)) : '—';
    return <div className="overview-detailed-measure-leak">
      <span className="overview-detailed-measure-value">{formatted}</span>
    </div>;
  },

  render () {
    let measure = this.props.measures[this.props.metric];
    if (measure == null) {
      return null;
    }

    return <div className="overview-detailed-measure">
      <div className="overview-detailed-measure-nutshell">
        <span className="overview-detailed-measure-name">{localizeMetric(this.props.metric)}</span>
        <span className="overview-detailed-measure-value">
          <DrilldownLink component={this.props.component.key} metric={this.props.metric}>
            {formatMeasure(measure, this.props.type)}
          </DrilldownLink>
        </span>
        {this.props.children}
      </div>
      {this.renderLeak()}
    </div>;
  }
});
