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
import { find, sortBy } from 'lodash';
import AddGraphMetricPopup from './AddGraphMetricPopup';
import DropdownIcon from '../../../../components/icons-components/DropdownIcon';
import BubblePopupHelper from '../../../../components/common/BubblePopupHelper';
import { isDiffMetric } from '../../../../helpers/measures';
import { getLocalizedMetricName, translate } from '../../../../helpers/l10n';
/*:: import type { Metric } from '../../types'; */

/*::
type Props = {
  addMetric: (metric: string) => void,
  className?: string,
  metrics: Array<Metric>,
  metricsTypeFilter: ?Array<string>,
  removeMetric: (metric: string) => void,
  selectedMetrics: Array<string>
};
*/

/*::
type State = {
  open: boolean,
  selectedMetric?: string,
  metrics: Array<string>,
  selectedMetrics: Array<string>,
  query: string
};
*/

export default class AddGraphMetric extends React.PureComponent {
  /*:: props: Props; */
  state /*: State */ = {
    open: false,
    metrics: [],
    selectedMetrics: [],
    query: ''
  };

  componentDidMount() {
    this.setState({
      metrics: this.filterMetricsElements(this.props),
      selectedMetrics: this.getSelectedMetricsElements(this.props.metrics, null)
    });
  }

  componentWillReceiveProps(nextProps /*: Props */) {
    this.setState({
      metrics: this.filterMetricsElements(nextProps),
      selectedMetrics: this.getSelectedMetricsElements(nextProps.metrics, nextProps.selectedMetrics)
    });
  }

  getPopupPos = (containerPos /*: ClientRect*/) => ({
    top: containerPos.height,
    right: containerPos.width - 240
  });

  filterMetricsElements = (props /*: Props */) => {
    const { metricsTypeFilter, metrics, selectedMetrics } = props;
    return metrics
      .filter(metric => {
        if (
          metric.hidden ||
          isDiffMetric(metric.key) ||
          ['DATA', 'DISTRIB'].includes(metric.type) ||
          selectedMetrics.includes(metric.key)
        ) {
          return false;
        }
        if (metricsTypeFilter && metricsTypeFilter.length > 0) {
          return metricsTypeFilter.includes(metric.type);
        }
        return true;
      })
      .map(metric => metric.key);
  };

  getSelectedMetricsElements = (
    metrics /*: Array<Metric> */,
    selectedMetrics /*: Array<string> | null */
  ) => {
    const selected /*: Array<string> */ =
      selectedMetrics === null ? this.props.selectedMetrics : selectedMetrics;
    return metrics.filter(metric => selected.includes(metric.key)).map(metric => metric.key);
  };

  getLocalizedMetricNameFromKey = (key /*: string*/) => {
    const metric = find(this.props.metrics, { key });
    return metric === undefined ? key : getLocalizedMetricName(metric);
  };

  toggleForm = () => {
    this.setState(state => {
      return { open: !state.open };
    });
  };

  onSearch = (query /*: string */) => {
    this.setState({ query });
    return Promise.resolve();
  };

  onSelect = (metric /*: string */) => {
    this.props.addMetric(metric);
    this.setState(state => {
      return {
        selectedMetrics: sortBy([...state.selectedMetrics, metric]),
        metrics: this.filterMetricsElements(this.props)
      };
    });
  };

  onUnselect = (metric /*: string */) => {
    this.props.removeMetric(metric);
    this.setState(state => {
      return {
        metrics: sortBy([...state.metrics, metric]),
        selectedMetrics: state.selectedMetrics.filter(selected => selected !== metric)
      };
    });
  };

  togglePopup = (open /*: boolean*/) => {
    this.setState({ open });
  };

  render() {
    const { metrics, selectedMetrics, query } = this.state;
    const filteredMetrics = metrics.filter(
      (metric /*: string */) => metric.toLowerCase().indexOf(query.toLowerCase()) > -1
    );

    return (
      <div className="display-inline-block">
        <BubblePopupHelper
          isOpen={this.state.open}
          offset={{ horizontal: 16, vertical: 0 }}
          popup={
            <AddGraphMetricPopup
              elements={filteredMetrics}
              metricsTypeFilter={this.props.metricsTypeFilter}
              onSearch={this.onSearch}
              onSelect={this.onSelect}
              onUnselect={this.onUnselect}
              renderLabel={element => this.getLocalizedMetricNameFromKey(element)}
              selectedElements={selectedMetrics}
            />
          }
          position="bottomright"
          togglePopup={this.togglePopup}>
          <button className="spacer-left" onClick={this.toggleForm} type="button">
            <span>
              <span className="text-ellipsis spacer-right">
                {translate('project_activity.graphs.custom.add')}
              </span>
              <DropdownIcon className="vertical-text-top" />
            </span>
          </button>
        </BubblePopupHelper>
      </div>
    );
  }
}
