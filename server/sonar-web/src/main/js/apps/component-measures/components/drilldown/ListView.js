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
import ComponentsList from './ComponentsList';
import ListFooter from '../../../../components/controls/ListFooter';
import { getComponentTree } from '../../../../api/components';
import { complementary } from '../../config/complementary';
import { enhanceComponent } from '../../utils';
import { isDiffMetric } from '../../../../helpers/measures';
import type { Component, ComponentEnhanced, Paging } from '../../types';
import type { Metric } from '../../../../store/metrics/actions';

type Props = {
  component: Component,
  handleSelect: Component => void,
  metric: Metric,
  metrics: { [string]: Metric },
  updateLoading: ({ [string]: boolean }) => void
};

type State = {
  components: Array<ComponentEnhanced>,
  metric: ?Metric,
  paging?: Paging
};

export default class ListView extends React.PureComponent {
  mounted: boolean;
  props: Props;
  state: State = {
    components: [],
    metric: null,
    paging: null
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchComponents(this.props);
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.component !== this.props.component || nextProps.metric !== this.props.metric) {
      this.fetchComponents(nextProps);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getComponentRequestParams = (metric: Metric, options: Object = {}) => {
    const metricKeys = [metric.key, ...(complementary[metric.key] || [])];
    let opts: Object = {
      asc: metric.direction === 1,
      ps: 100,
      metricSortFilter: 'withMeasuresOnly',
      metricSort: metric.key
    };
    if (isDiffMetric(metric.key)) {
      opts = {
        ...opts,
        s: 'metricPeriod,name',
        metricPeriodSort: 1
      };
    } else {
      opts = {
        ...opts,
        s: 'metric,name'
      };
    }
    return { metricKeys, opts: { ...opts, ...options } };
  };

  fetchComponents = ({ component, metric }: Props) => {
    const { metricKeys, opts } = this.getComponentRequestParams(metric);
    this.props.updateLoading({ components: true });
    getComponentTree('leaves', component.key, metricKeys, opts).then(
      r => {
        if (this.mounted) {
          this.setState({
            components: r.components.map(component => enhanceComponent(component, metric)),
            metric,
            paging: r.paging
          });
        }
        this.props.updateLoading({ components: false });
      },
      () => this.props.updateLoading({ components: false })
    );
  };

  fetchMoreComponents = () => {
    const { component, metric } = this.props;
    const { paging } = this.state;
    if (!paging) {
      return;
    }
    const { metricKeys, opts } = this.getComponentRequestParams(metric, {
      p: paging.pageIndex + 1
    });
    this.props.updateLoading({ components: true });
    getComponentTree('leaves', component.key, metricKeys, opts).then(
      r => {
        if (this.mounted) {
          this.setState(state => ({
            components: [
              ...state.components,
              ...r.components.map(component => enhanceComponent(component, metric))
            ],
            metric,
            paging: r.paging
          }));
        }
        this.props.updateLoading({ components: false });
      },
      () => this.props.updateLoading({ components: false })
    );
  };

  render() {
    const { components, metric, paging } = this.state;
    if (metric == null) {
      return null;
    }

    return (
      <div>
        <ComponentsList
          components={components}
          metrics={this.props.metrics}
          metric={metric}
          onClick={this.props.handleSelect}
        />
        {paging &&
          <ListFooter
            count={components.length}
            total={paging.total}
            loadMore={this.fetchMoreComponents}
          />}
      </div>
    );
  }
}
