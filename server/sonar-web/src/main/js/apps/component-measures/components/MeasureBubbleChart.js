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

import Spinner from './Spinner';
import { BubbleChart } from '../../../components/charts/bubble-chart';
import bubbles from '../bubbles';
import { getFiles } from '../../../api/components';
import { formatMeasure } from '../../../helpers/measures';
import Workspace from '../../../components/workspace/main';

const HEIGHT = 360;
const BUBBLES_LIMIT = 500;

function getMeasure (component, metric) {
  return Number(component.measures[metric]) || 0;
}

export default class MeasureBubbleChart extends React.Component {
  state = {
    fetching: true,
    files: []
  };

  componentWillMount () {
    const { metric } = this.props;
    const { metrics } = this.context;
    const conf = bubbles[metric.key];

    this.xMetric = metrics.find(m => m.key === conf.x);
    this.yMetric = metrics.find(m => m.key === conf.y);
    this.sizeMetric = metrics.find(m => m.key === conf.size);
  }

  componentDidMount () {
    this.mounted = true;
    this.fetchFiles();
  }

  componentDidUpdate (nextProps, nextState, nextContext) {
    if ((nextProps.metric !== this.props.metric) ||
        (nextContext.component !== this.context.component)) {
      this.fetchFiles();
    }
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  fetchFiles () {
    const { component } = this.context;
    const metrics = [this.xMetric.key, this.yMetric.key, this.sizeMetric.key];
    const options = {
      s: 'metric',
      metricSort: this.sizeMetric.key,
      asc: false,
      ps: BUBBLES_LIMIT
    };

    getFiles(component.key, metrics, options).then(r => {
      const files = r.map(file => {
        const measures = {};

        file.measures.forEach(measure => {
          measures[measure.metric] = measure.value;
        });
        return { ...file, measures };
      });

      this.setState({
        files,
        fetching: false,
        total: files.length
      });
    });
  }

  getTooltip (component) {
    const inner = [
      component.name,
      `${this.xMetric.name}: ${formatMeasure(getMeasure(component, this.xMetric.key), this.xMetric.type)}`,
      `${this.yMetric.name}: ${formatMeasure(getMeasure(component, this.yMetric.key), this.yMetric.type)}`,
      `${this.sizeMetric.name}: ${formatMeasure(getMeasure(component, this.sizeMetric.key), this.sizeMetric.type)}`
    ].join('<br>');

    return `<div class="text-left">${inner}</div>`;
  }

  handleBubbleClick (id) {
    Workspace.openComponent({ uuid: id });
  }

  renderBubbleChart () {
    const items = this.state.files.map(file => {
      return {
        x: getMeasure(file, this.xMetric.key),
        y: getMeasure(file, this.yMetric.key),
        size: getMeasure(file, this.sizeMetric.key),
        link: file.id,
        tooltip: this.getTooltip(file)
      };
    });

    const formatXTick = (tick) => formatMeasure(tick, this.xMetric.type);
    const formatYTick = (tick) => formatMeasure(tick, this.yMetric.type);

    return (
        <BubbleChart
            items={items}
            height={HEIGHT}
            padding={[25, 60, 50, 60]}
            formatXTick={formatXTick}
            formatYTick={formatYTick}
            onBubbleClick={this.handleBubbleClick.bind(this)}/>
    );
  }

  render () {
    const { fetching } = this.state;

    if (fetching) {
      return (
          <div className="measure-details-bubble-chart">
            <div className="note text-center" style={{ lineHeight: `${HEIGHT}px` }}>
              <Spinner/>
            </div>
          </div>
      );
    }

    return (
        <div className="measure-details-bubble-chart">
          {this.renderBubbleChart()}

          <div className="measure-details-bubble-chart-axis x">{this.xMetric.name}</div>
          <div className="measure-details-bubble-chart-axis y">{this.yMetric.name}</div>
          <div className="measure-details-bubble-chart-axis size">Size: {this.sizeMetric.name}</div>
        </div>
    );
  }
}

MeasureBubbleChart.contextTypes = {
  component: React.PropTypes.object,
  metrics: React.PropTypes.array
};
