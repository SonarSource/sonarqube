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

import { BubbleChart } from '../../../components/charts/bubble-chart';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getFiles } from '../../../api/components';
import { formatMeasure } from '../../../helpers/measures';
import Workspace from '../../../components/workspace/main';


const HEIGHT = 360;
const BUBBLES_LIMIT = 500;


function getMeasure (component, metric) {
  return component.measures[metric] || 0;
}


export class DomainBubbleChart extends React.Component {
  constructor (props) {
    super(props);
    this.state = {
      loading: true,
      files: [],
      xMetric: this.getMetricObject(props.metrics, props.xMetric),
      yMetric: this.getMetricObject(props.metrics, props.yMetric),
      sizeMetrics: props.sizeMetrics.map(this.getMetricObject.bind(null, props.metrics))
    };
  }

  componentDidMount () {
    this.requestFiles();
  }

  requestFiles () {
    let metrics = [].concat(this.props.xMetric, this.props.yMetric, this.props.sizeMetrics);
    return getFiles(this.props.component.key, metrics).then(r => {
      let files = r.map(file => {
        let measures = {};
        (file.msr || []).forEach(measure => {
          measures[measure.key] = measure.val;
        });
        return _.extend(file, { measures });
      });
      this.setState({
        loading: false,
        files: this.limitFiles(files),
        total: files.length
      });
    });
  }

  limitFiles (files) {
    const comparator = file => -1 * this.getSizeMetricsValue(file);
    return _.sortBy(files, comparator).slice(0, BUBBLES_LIMIT);
  }

  getMetricObject (metrics, metricKey) {
    return _.findWhere(metrics, { key: metricKey });
  }

  getSizeMetricsValue (component) {
    return this.props.sizeMetrics.reduce((previousValue, currentValue) => {
      return previousValue + getMeasure(component, currentValue);
    }, 0);
  }

  getSizeMetricsTitle () {
    return this.state.sizeMetrics.map(metric => metric.name).join(' & ');
  }

  getTooltip (component) {
    let sizeMetricsTitle = this.getSizeMetricsTitle();
    let sizeMetricsType = this.state.sizeMetrics[0].type;

    /* eslint max-len: 0 */
    let inner = [
      component.name,
      `${this.state.xMetric.name}: ${formatMeasure(getMeasure(component, this.props.xMetric), this.state.xMetric.type)}`,
      `${this.state.yMetric.name}: ${formatMeasure(getMeasure(component, this.props.yMetric), this.state.yMetric.type)}`,
      `${sizeMetricsTitle}: ${formatMeasure(this.getSizeMetricsValue(component), sizeMetricsType)}`
    ].join('<br>');
    return `<div class="text-left">${inner}</div>`;
  }

  handleBubbleClick (uuid) {
    Workspace.openComponent({ uuid });
  }

  renderLoading () {
    return <div className="overview-chart-placeholder" style={{ height: HEIGHT }}>
      <i className="spinner"/>
    </div>;
  }

  renderBubbleChart () {
    if (this.state.loading) {
      return this.renderLoading();
    }

    let items = this.state.files.map(component => {
      return {
        x: getMeasure(component, this.props.xMetric),
        y: getMeasure(component, this.props.yMetric),
        size: this.getSizeMetricsValue(component),
        link: component.uuid,
        tooltip: this.getTooltip(component)
      };
    });
    let formatXTick = (tick) => formatMeasure(tick, this.state.xMetric.type);
    let formatYTick = (tick) => formatMeasure(tick, this.state.yMetric.type);
    return <BubbleChart items={items}
                        height={HEIGHT}
                        padding={[25, 60, 50, 60]}
                        formatXTick={formatXTick}
                        formatYTick={formatYTick}
                        onBubbleClick={this.handleBubbleClick}/>;
  }

  render () {
    if (this.props.component.qualifier === 'DEV' || this.props.component.qualifier === 'VW') {
      return null;
    }

    return <div className="overview-domain-chart">
      <div className="overview-card-header">
        <h2 className="overview-title">{translate('overview.chart.files')}</h2>
        <ul className="list-inline small">
          <li>
            {translateWithParameters('overview.chart.legend.size_x', this.getSizeMetricsTitle())}
          </li>
        </ul>
      </div>
      <div className="overview-bubble-chart">
        <div className="note" style={{ position: 'relative', top: '10px', left: '10px' }}>
          {this.state.yMetric.name}
        </div>
        {this.renderBubbleChart()}
        <div className="note text-right" style={{ position: 'relative', top: '-10px', left: '-10px' }}>
          {this.state.xMetric.name}
        </div>
        {this.state.total > BUBBLES_LIMIT &&
        <div className="note text-center">{translateWithParameters('overview.chart.files.limit_message', BUBBLES_LIMIT)}</div>}
      </div>
    </div>;
  }
}

DomainBubbleChart.propTypes = {
  xMetric: React.PropTypes.string.isRequired,
  yMetric: React.PropTypes.string.isRequired,
  sizeMetrics: React.PropTypes.arrayOf(React.PropTypes.string).isRequired
};
