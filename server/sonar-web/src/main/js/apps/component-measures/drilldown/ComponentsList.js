/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import ComponentsListRow from './ComponentsListRow';
import EmptyResult from './EmptyResult';
import { complementary } from '../config/complementary';
import { getLocalizedMetricName, translate, translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure, isDiffMetric, isPeriodBestValue } from '../../../helpers/measures';
/*:: import type { Component, ComponentEnhanced } from '../types'; */
/*:: import type { Metric } from '../../../store/metrics/actions'; */

/*:: type Props = {|
  bestValue?: string,
  branchLike?: { id?: string; name: string },
  components: Array<ComponentEnhanced>,
  onClick: string => void,
  metric: Metric,
  metrics: { [string]: Metric },
  rootComponent: Component,
  selectedComponent?: ?string
|}; */

/*:: type State = {
  hideBest: boolean
}; */

export default class ComponentsList extends React.PureComponent {
  /*:: props: Props; */
  state /*: State */ = {
    hideBest: true
  };

  componentWillReceiveProps(nextProps /*: Props */) {
    if (nextProps.metric !== this.props.metric) {
      this.setState({ hideBest: true });
    }
  }

  displayAll = (event /*: Event */) => {
    event.preventDefault();
    this.setState({ hideBest: false });
  };

  hasBestValue(component /*: Component*/, otherMetrics /*: Array<Metric> */) {
    const { metric } = this.props;
    const focusedMeasure = component.measures.find(measure => measure.metric.key === metric.key);
    if (isDiffMetric(focusedMeasure.metric.key)) {
      return isPeriodBestValue(focusedMeasure, 1);
    }
    return focusedMeasure.bestValue;
  }

  renderComponent(component /*: Component*/, otherMetrics /*: Array<Metric> */) {
    const { branchLike, metric, selectedComponent, onClick, rootComponent } = this.props;
    return (
      <ComponentsListRow
        branchLike={branchLike}
        component={component}
        isSelected={component.key === selectedComponent}
        key={component.id}
        metric={metric}
        onClick={onClick}
        otherMetrics={otherMetrics}
        rootComponent={rootComponent}
      />
    );
  }

  renderHiddenLink(hiddenCount /*: number*/, colCount /*: number*/) {
    return (
      <div className="alert alert-info spacer-top">
        {translateWithParameters(
          'component_measures.hidden_best_score_metrics',
          hiddenCount,
          formatMeasure(this.props.bestValue, this.props.metric.type)
        )}
        <a className="spacer-left" href="#" onClick={this.displayAll}>
          {translate('show_all')}
        </a>
      </div>
    );
  }

  render() {
    const { components, metric, metrics } = this.props;
    if (!components.length) {
      return <EmptyResult />;
    }

    const otherMetrics = (complementary[metric.key] || []).map(key => metrics[key]);
    const notBestComponents = components.filter(
      component => !this.hasBestValue(component, otherMetrics)
    );
    const hiddenCount = components.length - notBestComponents.length;
    const shouldHideBest = this.state.hideBest && hiddenCount !== components.length;
    return (
      <React.Fragment>
        <table className="data zebra zebra-hover">
          {otherMetrics.length > 0 && (
            <thead>
              <tr>
                <th>&nbsp;</th>
                <th className="text-right">
                  <span className="small">{getLocalizedMetricName(metric)}</span>
                </th>
                {otherMetrics.map(metric => (
                  <th className="text-right" key={metric.key}>
                    <span className="small">{getLocalizedMetricName(metric)}</span>
                  </th>
                ))}
              </tr>
            </thead>
          )}

          <tbody>
            {(shouldHideBest ? notBestComponents : components).map(component =>
              this.renderComponent(component, otherMetrics)
            )}
          </tbody>
        </table>
        {shouldHideBest &&
          hiddenCount > 0 &&
          this.renderHiddenLink(hiddenCount, otherMetrics.length + 3)}
      </React.Fragment>
    );
  }
}
