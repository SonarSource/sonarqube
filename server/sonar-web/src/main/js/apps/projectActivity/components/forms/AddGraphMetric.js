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
import { find } from 'lodash';
import DropdownIcon from '../../../../components/icons-components/DropdownIcon';
import BubblePopup from '../../../../components/common/BubblePopup';
import BubblePopupHelper from '../../../../components/common/BubblePopupHelper';
import MultiSelect from '../../../../components/common/MultiSelect';
import { isDiffMetric } from '../../../../helpers/measures';
import { getLocalizedMetricName, translate } from '../../../../helpers/l10n';
/*:: import type { Metric } from '../../types'; */

/*::
type Props = {
  addMetric: (metric: string) => void,
  removeMetric: (metric: string) => void,
  className?: string,
  metrics: Array<Metric>,
  metricsTypeFilter: ?Array<string>,
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
  card /*: HTMLDivElement | null*/ = null;

  /*:: props: Props; */
  state /*: State */ = {
    open: false,
    metrics: [],
    selectedMetrics: [],
    query: ''
  };

  componentDidMount() {
    if (this.card !== null) {
      window.addEventListener('keydown', this.handleKey, false);
      window.addEventListener('click', this.handleOutsideClick, false);
    }
    this.setState({
      metrics: this.filterMetricsElements(this.props),
      selectedMetrics: this.getSelectedMetricsElements(this.props.metrics, null)
    });
  }

  componentWillReceiveProps(nextProps /*: Props */) {
    if (nextProps.metrics.length > this.props.metrics.length) {
      this.setState({
        metrics: this.filterMetricsElements(nextProps),
        selectedMetrics: this.getSelectedMetricsElements(nextProps.metrics, null)
      });
    }
    if (nextProps.selectedMetrics.length < this.props.selectedMetrics.length) {
      this.setState({
        selectedMetrics: this.getSelectedMetricsElements(
          nextProps.metrics,
          nextProps.selectedMetrics
        )
      });
    }
  }

  componentWillUnmount() {
    window.removeEventListener('keydown', this.handleKey);
    window.removeEventListener('click', this.handleOutsideClick);
  }

  getPopupPos = (containerPos /*: ClientRect*/) => ({
    top: containerPos.height,
    right: containerPos.width - 240
  });

  handleKey = (evt /*: KeyboardEvent*/) => {
    // Escape key
    if (evt.keyCode === 27) {
      this.setState({ open: false });
    }
  };

  handleOutsideClick = (evt /*: Event*/) => {
    // $FlowFixMe
    if (!this.card || !this.card.contains(evt.target)) {
      this.setState({ open: false });
    }
  };

  filterMetricsElements = (nextProps /*: Props */) => {
    const { metricsTypeFilter, metrics } = nextProps;
    return metrics
      .filter(metric => {
        if (
          metric.hidden ||
          isDiffMetric(metric.key) ||
          ['DATA', 'DISTRIB'].includes(metric.type) ||
          this.props.selectedMetrics.includes(metric.key)
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
    return getLocalizedMetricName(metric);
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
        selectedMetrics: [...state.selectedMetrics, metric].sort(),
        metrics: state.metrics.filter(selected => selected !== metric)
      };
    });
  };

  onUnselect = (metric /*: string */) => {
    this.props.removeMetric(metric);
    this.setState(state => {
      return {
        metrics: [...state.metrics, metric].sort(),
        selectedMetrics: state.selectedMetrics.filter(selected => selected !== metric)
      };
    });
  };

  renderSelector() {
    const { metrics, selectedMetrics, query } = this.state;
    const filteredMetrics = metrics.filter(
      (metric /*: string */) => metric.toLowerCase().indexOf(query.toLowerCase()) > -1
    );

    return (
      <BubblePopupHelper
        isOpen={this.state.open}
        popup={
          <BubblePopup customClass="bubble-popup-bottom-right bubble-popup-menu abs-width-300">
            <MultiSelect
              alertMessage={translate('project_activity.graphs.custom.add_metric_info')}
              allowNewElements={false}
              allowSelection={selectedMetrics.length < 6}
              displayAlertMessage={selectedMetrics.length >= 6}
              elements={filteredMetrics}
              onSearch={this.onSearch}
              onSelect={this.onSelect}
              onUnselect={this.onUnselect}
              placeholder={translate('search.search_for_tags')}
              renderLabel={element => this.getLocalizedMetricNameFromKey(element)}
              selectedElements={selectedMetrics}
            />
          </BubblePopup>
        }
        position="bottomright"
        togglePopup={() => {}}
      />
    );
  }

  render() {
    return (
      <div ref={card => (this.card = card)}>
        <button className="spacer-left" onClick={this.toggleForm} type="button">
          <span>
            <span className="text-ellipsis spacer-right">
              {translate('project_activity.graphs.custom.add')}
            </span>
            <DropdownIcon className="vertical-text-top" />
          </span>
        </button>
        {this.renderSelector()}
      </div>
    );
  }
}
