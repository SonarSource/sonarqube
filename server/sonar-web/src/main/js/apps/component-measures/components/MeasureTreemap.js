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
import { Treemap } from '../../../components/charts/treemap';
import { getChildren } from '../../../api/components';
import { formatMeasure } from '../../../helpers/measures';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getComponentUrl } from '../../../helpers/urls';
import Workspace from '../../../components/workspace/main';

const HEIGHT = 360;

export default class MeasureTreemap extends React.Component {
  state = {
    fetching: true,
    components: [],
    breadcrumbs: []
  };

  componentDidMount () {
    const { component } = this.context;

    this.mounted = true;
    this.fetchComponents(component.key);
  }

  componentDidUpdate (nextProps, nextState, nextContext) {
    if ((nextProps.metric !== this.props.metric) ||
        (nextContext.component !== this.context.component)) {
      this.fetchComponents(nextContext.component.key);
    }
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  fetchComponents (componentKey) {
    const { metric } = this.props;
    const metrics = ['ncloc', metric.key];
    const options = {
      s: 'metric',
      metricSort: 'ncloc',
      asc: false
    };

    return getChildren(componentKey, metrics, options).then(r => {
      const components = r.map(component => {
        const measures = {};
        const key = component.refKey || component.key;

        component.measures.forEach(measure => {
          measures[measure.metric] = measure.value;
        });
        return { ...component, measures, key };
      });

      this.setState({
        components,
        fetching: false
      });
    });
  }

  getTooltip (component) {
    const { metric } = this.props;

    let inner = [
      component.name,
      `${translate('metric.ncloc.name')}: ${formatMeasure(component.measures['ncloc'], 'INT')}`
    ];

    const colorMeasure = component.measures[metric.key];
    const formatted = colorMeasure != null ? formatMeasure(colorMeasure, metric.type) : '—';

    inner.push(`${metric.name}: ${formatted}`);
    inner = inner.join('<br>');

    return `<div class="text-left">${inner}</div>`;
  }

  getPercentColorScale (metric) {
    const color = d3.scale.linear().domain([0, 25, 50, 75, 100]);
    color.range(metric.direction === 1 ?
        ['#ee0000', '#f77700', '#ffee00', '#80cc00', '#00aa00'] :
        ['#00aa00', '#80cc00', '#ffee00', '#f77700', '#ee0000']);
    return color;
  }

  getRatingColorScale () {
    return d3.scale.linear()
        .domain([1, 2, 3, 4, 5])
        .range(['#00aa00', '#80cc00', '#ffee00', '#f77700', '#ee0000']);
  }

  getLevelColorScale () {
    return d3.scale.ordinal()
        .domain(['ERROR', 'WARN', 'OK', 'NONE'])
        .range(['#d4333f', '#ff9900', '#85bb43', '##b4b4b4']);
  }

  getScale () {
    const { metric } = this.props;

    if (metric.type === 'LEVEL') {
      return this.getLevelColorScale();
    }
    if (metric.type === 'RATING') {
      return this.getRatingColorScale();
    }
    return this.getPercentColorScale(metric);
  }

  handleRectangleClick (node) {
    const isFile = node.qualifier === 'FIL' || node.qualifier === 'UTS';

    if (isFile) {
      return Workspace.openComponent({ uuid: node.id });
    }

    this.fetchComponents(node.key).then(() => {
      let nextBreadcrumbs = [...this.state.breadcrumbs];
      const index = this.state.breadcrumbs.findIndex(b => b.key === node.key);

      if (index !== -1) {
        nextBreadcrumbs = nextBreadcrumbs.slice(0, index);
      }

      nextBreadcrumbs = [...nextBreadcrumbs, {
        key: node.key,
        name: node.name,
        qualifier: node.qualifier
      }];

      this.setState({ breadcrumbs: nextBreadcrumbs });
    });
  }

  handleReset () {
    const { component } = this.context;
    this.fetchComponents(component.key).then(() => {
      this.setState({ breadcrumbs: [] });
    });
  }

  renderTreemap () {
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

    // FIXME remove this magic number
    const height = HEIGHT - 35;

    return (
        <Treemap
            items={items}
            breadcrumbs={this.state.breadcrumbs}
            height={height}
            canBeClicked={() => true}
            onRectangleClick={this.handleRectangleClick.bind(this)}
            onReset={this.handleReset.bind(this)}/>
    );
  }

  render () {
    const { metric } = this.props;
    const { fetching } = this.state;

    if (fetching) {
      return (
          <div className="measure-details-treemap">
            <div className="note text-center" style={{ lineHeight: `${HEIGHT}px` }}>
              <Spinner/>
            </div>
          </div>
      );
    }

    return (
        <div className="measure-details-treemap">
          <ul className="list-inline note measure-details-treemap-legend">
            <li>
              {translateWithParameters('component_measures.legend.color_x', metric.name)}
            </li>
            <li>
              {translateWithParameters('component_measures.legend.size_x', translate('metric.ncloc.name'))}
            </li>
          </ul>
          {this.renderTreemap()}
        </div>
    );
  }
}

MeasureTreemap.contextTypes = {
  component: React.PropTypes.object,
  metrics: React.PropTypes.array
};
