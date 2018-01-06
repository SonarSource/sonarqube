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
import classNames from 'classnames';
import Modal from '../../../../components/controls/Modal';
import Select from '../../../../components/controls/Select';
import Tooltip from '../../../../components/controls/Tooltip';
import { isDiffMetric } from '../../../../helpers/measures';
import {
  getLocalizedMetricName,
  translate,
  translateWithParameters
} from '../../../../helpers/l10n';
/*:: import type { Metric } from '../../types'; */

/*::
type Props = {
  addMetric: (metric: string) => void,
  className?: string,
  metrics: Array<Metric>,
  metricsTypeFilter: ?Array<string>,
  selectedMetrics: Array<string>
};
*/

/*::
type State = {
  open: boolean,
  selectedMetric?: string
};
*/

export default class AddGraphMetric extends React.PureComponent {
  /*:: props: Props; */
  state /*: State */ = {
    open: false
  };

  getMetricsOptions = (metricsTypeFilter /*: ?Array<string> */) => {
    return this.props.metrics
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
      .map((metric /*: Metric */) => ({
        value: metric.key,
        label: getLocalizedMetricName(metric)
      }));
  };

  openForm = () => {
    this.setState({
      open: true
    });
  };

  closeForm = () => {
    this.setState({
      open: false,
      selectedMetric: undefined
    });
  };

  handleChange = (option /*: { value: string, label: string } */) =>
    this.setState({ selectedMetric: option.value });

  handleSubmit = (e /*: Object */) => {
    e.preventDefault();
    if (this.state.selectedMetric) {
      this.props.addMetric(this.state.selectedMetric);
      this.closeForm();
    }
  };

  renderModal() {
    const { metricsTypeFilter } = this.props;
    const header = translate('project_activity.graphs.custom.add_metric');
    return (
      <Modal key="add-metric-modal" contentLabel={header} onRequestClose={this.closeForm}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <form onSubmit={this.handleSubmit}>
          <div className="modal-body">
            <div className="modal-large-field">
              <label>{translate('project_activity.graphs.custom.search')}</label>
              <Select
                autofocus={true}
                className="Select-big"
                clearable={false}
                noResultsText={translate('no_results')}
                onChange={this.handleChange}
                options={this.getMetricsOptions(metricsTypeFilter)}
                placeholder=""
                searchable={true}
                value={this.state.selectedMetric}
              />
              <span className="alert alert-info">
                {metricsTypeFilter != null && metricsTypeFilter.length > 0
                  ? translateWithParameters(
                      'project_activity.graphs.custom.type_x_message',
                      metricsTypeFilter
                        .map(type => translate('metric.type', type))
                        .sort()
                        .join(', ')
                    )
                  : translate('project_activity.graphs.custom.add_metric_info')}
              </span>
            </div>
          </div>
          <footer className="modal-foot">
            <div>
              <button type="submit" disabled={!this.state.selectedMetric}>
                {translate('project_activity.graphs.custom.add')}
              </button>
              <button type="reset" className="button-link" onClick={this.closeForm}>
                {translate('cancel')}
              </button>
            </div>
          </footer>
        </form>
      </Modal>
    );
  }

  render() {
    if (this.props.selectedMetrics.length >= 6) {
      // Use the class .disabled instead of the property to prevent a bug from
      // rc-tooltip : https://github.com/react-component/tooltip/issues/18
      return (
        <Tooltip
          placement="right"
          overlay={translate('project_activity.graphs.custom.add_metric_info')}>
          <button className={classNames('disabled', this.props.className)}>
            {translate('project_activity.graphs.custom.add')}
          </button>
        </Tooltip>
      );
    }

    const buttonComponent = (
      <button key="add-metric-button" className={this.props.className} onClick={this.openForm}>
        {translate('project_activity.graphs.custom.add')}
      </button>
    );

    if (this.state.open) {
      return [buttonComponent, this.renderModal()];
    }

    return buttonComponent;
  }
}
