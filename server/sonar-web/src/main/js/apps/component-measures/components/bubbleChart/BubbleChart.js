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
import React from 'react';
import Spinner from './../Spinner';
import OriginalBubbleChart from '../../../../components/charts/BubbleChart';
import bubbles from '../../config/bubbles';
import { getComponentLeaves } from '../../../../api/components';
import { formatMeasure } from '../../../../helpers/measures';
import Workspace from '../../../../components/workspace/main';
import { getComponentUrl } from '../../../../helpers/urls';
import { getLocalizedMetricName, translateWithParameters } from '../../../../helpers/l10n';

const HEIGHT = 500;
const BUBBLES_LIMIT = 500;

function getMeasure(component, metric) {
  return Number(component.measures[metric]) || 0;
}

export default class BubbleChart extends React.Component {
  state = {
    fetching: 0,
    files: []
  };

  componentWillMount() {
    this.updateMetrics(this.props);
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchFiles();
  }

  componentWillUpdate(nextProps) {
    this.updateMetrics(nextProps);
  }

  componentDidUpdate(nextProps) {
    if (nextProps.domainName !== this.props.domainName) {
      this.fetchFiles();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  updateMetrics(props) {
    const { metrics, domainName } = props;
    const conf = bubbles[domainName];
    this.xMetric = metrics.find(m => m.key === conf.x);
    this.yMetric = metrics.find(m => m.key === conf.y);
    this.sizeMetric = metrics.find(m => m.key === conf.size);
  }

  fetchFiles() {
    const { component } = this.props;
    const metrics = [this.xMetric.key, this.yMetric.key, this.sizeMetric.key];
    const options = {
      s: 'metric',
      metricSort: this.sizeMetric.key,
      asc: false,
      ps: BUBBLES_LIMIT
    };

    if (this.mounted) {
      this.setState({ fetching: this.state.fetching + 1 });
    }

    getComponentLeaves(component.key, metrics, options).then(r => {
      const files = r.components.map(file => {
        const measures = {};

        file.measures.forEach(measure => {
          measures[measure.metric] = measure.value;
        });
        return { ...file, measures };
      });

      if (this.mounted) {
        this.setState({
          files,
          fetching: this.state.fetching - 1,
          total: files.length
        });
      }
    });
  }

  getTooltip(component) {
    const x = formatMeasure(getMeasure(component, this.xMetric.key), this.xMetric.type);
    const y = formatMeasure(getMeasure(component, this.yMetric.key), this.yMetric.type);
    const size = formatMeasure(getMeasure(component, this.sizeMetric.key), this.sizeMetric.type);
    const inner = [
      component.name,
      `${this.xMetric.name}: ${x}`,
      `${this.yMetric.name}: ${y}`,
      `${this.sizeMetric.name}: ${size}`
    ].join('<br>');

    return `<div class="text-left">${inner}</div>`;
  }

  handleBubbleClick(component) {
    if (['FIL', 'UTS'].includes(component.qualifier)) {
      Workspace.openComponent({ key: component.key });
    } else {
      window.location = getComponentUrl(component.refKey || component.key);
    }
  }

  renderBubbleChart() {
    const items = this.state.files.map(file => {
      return {
        x: getMeasure(file, this.xMetric.key),
        y: getMeasure(file, this.yMetric.key),
        size: getMeasure(file, this.sizeMetric.key),
        link: file,
        tooltip: this.getTooltip(file)
      };
    });

    const formatXTick = tick => formatMeasure(tick, this.xMetric.type);
    const formatYTick = tick => formatMeasure(tick, this.yMetric.type);

    return (
      <OriginalBubbleChart
        items={items}
        height={HEIGHT}
        padding={[25, 60, 50, 60]}
        formatXTick={formatXTick}
        formatYTick={formatYTick}
        onBubbleClick={this.handleBubbleClick.bind(this)}
      />
    );
  }

  render() {
    const { fetching } = this.state;

    if (fetching) {
      return (
        <div className="measure-details-bubble-chart">
          <div className="note text-center" style={{ lineHeight: `${HEIGHT}px` }}>
            <Spinner />
          </div>
        </div>
      );
    }

    return (
      <div className="measure-details-bubble-chart">
        <div>
          {this.renderBubbleChart()}
        </div>

        <div className="measure-details-bubble-chart-axis x">
          {getLocalizedMetricName(this.xMetric)}
        </div>
        <div className="measure-details-bubble-chart-axis y">
          {getLocalizedMetricName(this.yMetric)}
        </div>
        <div className="measure-details-bubble-chart-axis size">
          {translateWithParameters(
            'component_measures.legend.size_x',
            getLocalizedMetricName(this.sizeMetric)
          )}
        </div>
      </div>
    );
  }
}
