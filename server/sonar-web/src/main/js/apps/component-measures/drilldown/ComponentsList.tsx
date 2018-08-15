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
import * as React from 'react';
import ComponentsListRow from './ComponentsListRow';
import EmptyResult from './EmptyResult';
import { complementary } from '../config/complementary';
import { getLocalizedMetricName, translate, translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure, isDiffMetric, isPeriodBestValue } from '../../../helpers/measures';
import { ComponentMeasure, ComponentMeasureEnhanced, Metric, BranchLike } from '../../../app/types';
import { Button } from '../../../components/ui/buttons';

interface Props {
  bestValue?: string;
  branchLike?: BranchLike;
  components: ComponentMeasureEnhanced[];
  onClick: (component: string) => void;
  metric: Metric;
  metrics: { [metric: string]: Metric };
  rootComponent: ComponentMeasure;
  selectedComponent?: string;
}

interface State {
  hideBest: boolean;
}

export default class ComponentsList extends React.PureComponent<Props, State> {
  state: State = { hideBest: true };

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.metric !== this.props.metric) {
      this.setState({ hideBest: true });
    }
  }

  displayAll = () => {
    this.setState({ hideBest: false });
  };

  hasBestValue = (component: ComponentMeasureEnhanced) => {
    const { metric } = this.props;
    const focusedMeasure = component.measures.find(measure => measure.metric.key === metric.key);
    if (focusedMeasure && isDiffMetric(focusedMeasure.metric.key)) {
      return isPeriodBestValue(focusedMeasure, 1);
    }
    return Boolean(focusedMeasure && focusedMeasure.bestValue);
  };

  renderHiddenLink = (hiddenCount: number) => {
    return (
      <div className="alert alert-info spacer-top">
        {translateWithParameters(
          'component_measures.hidden_best_score_metrics',
          hiddenCount,
          formatMeasure(this.props.bestValue, this.props.metric.type)
        )}
        <Button className="button-link spacer-left" onClick={this.displayAll}>
          {translate('show_all')}
        </Button>
      </div>
    );
  };

  render() {
    const { components, metric, metrics } = this.props;
    if (!components.length) {
      return <EmptyResult />;
    }

    const otherMetrics = (complementary[metric.key] || []).map(key => metrics[key]);
    const notBestComponents = components.filter(component => !this.hasBestValue(component));
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
            {(shouldHideBest ? notBestComponents : components).map(component => (
              <ComponentsListRow
                branchLike={this.props.branchLike}
                component={component}
                isSelected={component.key === this.props.selectedComponent}
                key={component.key}
                metric={metric}
                onClick={this.props.onClick}
                otherMetrics={otherMetrics}
                rootComponent={this.props.rootComponent}
              />
            ))}
          </tbody>
        </table>
        {shouldHideBest && hiddenCount > 0 && this.renderHiddenLink(hiddenCount)}
      </React.Fragment>
    );
  }
}
