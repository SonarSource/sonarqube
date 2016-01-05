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
import _ from 'underscore';
import React from 'react';

import { Histogram } from '../../../components/charts/histogram';
import { formatMeasure } from '../../../helpers/measures';
import { collapsePath } from '../../../helpers/path';
import { getComponentDrilldownUrl } from '../../../helpers/urls';
import { getChildren } from '../../../api/components';


const HEIGHT = 302;
const METRIC = 'ncloc';


export const NclocDistribution = React.createClass({
  propTypes: {
    component: React.PropTypes.object.isRequired
  },

  getInitialState() {
    return { loading: true, files: [] };
  },

  componentDidMount () {
    this.requestComponents();
  },

  requestComponents () {
    let metrics = [METRIC];
    return getChildren(this.props.component.key, metrics).then(r => {
      let components = r.map(component => {
        let measures = {};
        (component.msr || []).forEach(measure => {
          measures[measure.key] = measure.val;
        });
        return _.extend(component, { measures });
      });
      this.setState({ loading: false, components });
    });
  },

  handleBarClick(d) {
    window.location = getComponentDrilldownUrl(d.component.key, 'ncloc');
  },

  renderLoading () {
    return <div className="overview-chart-placeholder" style={{ height: HEIGHT }}>
      <i className="spinner"/>
    </div>;
  },

  renderBarChart () {
    if (this.state.loading) {
      return this.renderLoading();
    }

    let data = this.state.components.map((component, index) => {
      return {
        x: parseInt(component.measures[METRIC], 10),
        y: index,
        value: component.name,
        component: component
      };
    });

    data = _.sortBy(data, d => -d.x);

    let yTicks = data.map(d => {
      return {
        label: collapsePath(d.value, 20),
        tooltip: d.value
      };
    });

    let yValues = data.map(d => formatMeasure(d.x, 'SHORT_INT'));

    return <Histogram data={data}
                      yTicks={yTicks}
                      yValues={yValues}
                      height={data.length * 25}
                      barsWidth={10}
                      onBarClick={this.handleBarClick}
                      padding={[0, 50, 0, 240]}/>;
  },

  render () {
    return <div className="overview-domain-chart">
      <div className="overview-card-header">
        <h2 className="overview-title">{window.t('overview.chart.components')}</h2>
        <span className="small">
          {window.tp('overview.chart.legend.size_x', window.t('metric.ncloc.name'))}
        </span>
      </div>
      <div className="overview-bar-chart">
        {this.renderBarChart()}
      </div>
    </div>;
  }
});
