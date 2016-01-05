/*
 * SonarQube :: Web
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

import { getPeriodLabel, getPeriodDate } from '../helpers/periods';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { formatMeasure } from '../../../helpers/measures';


const Measure = React.createClass({
  render() {
    if (this.props.value == null || isNaN(this.props.value)) {
      return null;
    }
    let formatted = formatMeasure(this.props.value, this.props.type);
    return <span>{formatted}</span>;
  }
});


export default React.createClass({
  render() {
    let metricName = window.t('metric', this.props.condition.metric.name, 'name');
    let threshold = this.props.condition.level === 'ERROR' ?
                    this.props.condition.error : this.props.condition.warning;
    let period = this.props.condition.period ?
                 getPeriodLabel(this.props.component.periods, this.props.condition.period) : null;
    let periodDate = getPeriodDate(this.props.component.periods, this.props.condition.period);

    let classes = 'alert_' + this.props.condition.level.toUpperCase();

    return (
        <li className="overview-gate-condition">
          <div className="little-spacer-bottom">{period}</div>

          <div style={{ display: 'flex', alignItems: 'center' }}>
            <div className="overview-gate-condition-value">
              <DrilldownLink component={this.props.component.key} metric={this.props.condition.metric.name}
                             period={this.props.condition.period} periodDate={periodDate}>
              <span className={classes}>
                <Measure value={this.props.condition.actual} type={this.props.condition.metric.type}/>
              </span>
              </DrilldownLink>&nbsp;
            </div>

            <div className="overview-gate-condition-metric">
              <div>{metricName}</div>
              <div>
                {window.t('quality_gates.operator', this.props.condition.op, 'short')}
                {' '}
                <Measure value={threshold} type={this.props.condition.metric.type}/>
              </div>
            </div>
          </div>
        </li>
    );
  }
});
