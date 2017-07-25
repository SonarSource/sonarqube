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
import { scaleLinear, scaleOrdinal } from 'd3-scale';
import Spinner from './../../components/Spinner';
import { getLeakValue } from '../../utils';
import { Treemap } from '../../../../components/charts/treemap';
import { getChildren } from '../../../../api/components';
import { formatMeasure } from '../../../../helpers/measures';
import {
  translate,
  translateWithParameters,
  getLocalizedMetricName
} from '../../../../helpers/l10n';
import { getComponentUrl } from '../../../../helpers/urls';
import Workspace from '../../../../components/workspace/main';

const HEIGHT = 500;

export default class MeasureTreemap extends React.PureComponent {
  state = {
    fetching: true,
    components: [],
    breadcrumbs: []
  };

  componentDidMount() {
    const { component } = this.props;

    this.mounted = true;
    this.fetchComponents(component.key);
  }

  componentDidUpdate(nextProps) {
    if (nextProps.metric !== this.props.metric) {
      this.fetchComponents(this.props.component.key);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchComponents(componentKey) {
    const { metric } = this.props;
    const metrics = ['ncloc', metric.key];
    const options = {
      s: 'metric',
      metricSort: 'ncloc',
      asc: false
    };

    return getChildren(componentKey, metrics, options).then(r => {
      const components = r.components.map(component => {
        const measures = {};
        const key = component.refKey || component.key;

        component.measures.forEach(measure => {
          const shouldUseLeak = measure.metric.indexOf('new_') === 0;
          measures[measure.metric] = shouldUseLeak ? getLeakValue(measure) : measure.value;
        });
        return { ...component, measures, key };
      });

      this.setState({
        components,
        fetching: false
      });
    });
  }

  getTooltip(component) {
    const { metric } = this.props;

    let inner = [
      component.name,
      `${translate('metric.ncloc.name')}: ${formatMeasure(component.measures['ncloc'], 'INT')}`
    ];

    const colorMeasure = component.measures[metric.key];
    const formatted = colorMeasure != null ? formatMeasure(colorMeasure, metric.type) : '—';
    inner.push(`${getLocalizedMetricName(metric)}: ${formatted}`);
    inner = inner.join('<br>');
    return `<div class="text-left">${inner}</div>`;
  }
  getPercentColorScale(metric) {
    const color = scaleLinear().domain([0, 25, 50, 75, 100]);
    color.range(
      metric.direction === 1
        ? ['#d4333f', '#ed7d20', '#eabe06', '#b0d513', '#00aa00']
        : ['#00aa00', '#b0d513', '#eabe06', '#ed7d20', '#d4333f']
    );
    return color;
  }
  getRatingColorScale() {
    return scaleLinear()
      .domain([1, 2, 3, 4, 5])
      .range(['#00aa00', '#b0d513', '#eabe06', '#ed7d20', '#d4333f']);
  }
  getLevelColorScale() {
    return scaleOrdinal()
      .domain(['ERROR', 'WARN', 'OK', 'NONE'])
      .range(['#d4333f', '#ed7d20', '#00aa00', '#b4b4b4']);
  }
  getScale() {
    const { metric } = this.props;
    if (metric.type === 'LEVEL') {
      return this.getLevelColorScale();
    }
    if (metric.type === 'RATING') {
      return this.getRatingColorScale();
    }
    return this.getPercentColorScale(metric);
  }
  handleRectangleClick(node) {
    const isFile = node.qualifier === 'FIL' || node.qualifier === 'UTS';
    if (isFile) {
      Workspace.openComponent({ key: node.key });
      return;
    }
    this.fetchComponents(node.key).then(() => {
      let nextBreadcrumbs = [...this.state.breadcrumbs];
      const index = this.state.breadcrumbs.findIndex(b => b.key === node.key);
      if (index !== -1) {
        nextBreadcrumbs = nextBreadcrumbs.slice(0, index);
      }
      nextBreadcrumbs = [
        ...nextBreadcrumbs,
        {
          key: node.key,
          name: node.name,
          qualifier: node.qualifier
        }
      ];
      this.setState({ breadcrumbs: nextBreadcrumbs });
    });
  }
  handleReset() {
    const { component } = this.props;
    this.fetchComponents(component.key).then(() => {
      this.setState({ breadcrumbs: [] });
    });
  }
  renderTreemap() {
    const { metric } = this.props;
    const colorScale = this.getScale();
    const items = this.state.components
      .filter(component => component.measures['ncloc'])
      .map(component => {
        const colorMeasure = component.measures[metric.key];
        return {
          id: component.id,
          key: component.key,
          name: component.name,
          qualifier: component.qualifier,
          size: component.measures['ncloc'],
          color: colorMeasure != null ? colorScale(colorMeasure) : '#777',
          tooltip: this.getTooltip(component),
          label: component.name,
          link: getComponentUrl(component.key)
        };
      });
    return (
      <Treemap
        items={items}
        breadcrumbs={this.state.breadcrumbs}
        height={HEIGHT}
        canBeClicked={() => true}
        onRectangleClick={this.handleRectangleClick.bind(this)}
        onReset={this.handleReset.bind(this)}
      />
    );
  }
  render() {
    const { metric } = this.props;
    const { fetching } = this.state;
    if (fetching) {
      return (
        <div className="measure-details-treemap">
          <div className="note text-center" style={{ lineHeight: `${HEIGHT}px` }}>
            <Spinner />
          </div>
        </div>
      );
    }
    return (
      <div className="measure-details-treemap">
        <ul className="list-inline note measure-details-treemap-legend">
          <li>
            {translateWithParameters(
              'component_measures.legend.color_x',
              getLocalizedMetricName(metric)
            )}
          </li>
          <li>
            {translateWithParameters(
              'component_measures.legend.size_x',
              translate('metric.ncloc.name')
            )}
          </li>
        </ul>
        {this.renderTreemap()}
      </div>
    );
  }
}
