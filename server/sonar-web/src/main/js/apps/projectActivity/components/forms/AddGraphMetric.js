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
import Modal from 'react-modal';
import Select from 'react-select';
import { isDiffMetric } from '../../../../helpers/measures';
import { translate } from '../../../../helpers/l10n';
import type { Metric } from '../../types';

type Props = {
  addMetric: (metric: string) => void,
  className?: string,
  metrics: Array<Metric>,
  selectedMetrics: Array<string>
};

type State = {
  open: boolean,
  selectedMetric?: string
};

export default class AddGraphMetric extends React.PureComponent {
  props: Props;
  state: State = {
    open: false
  };

  getMetricsType = () => {
    if (this.props.selectedMetrics.length > 0) {
      const metric = this.props.metrics.find(
        metric => metric.key === this.props.selectedMetrics[0]
      );
      return metric && metric.type;
    }
  };

  getMetricsOptions = () => {
    const selectedType = this.getMetricsType();
    return this.props.metrics
      .filter(metric => {
        if (metric.hidden || isDiffMetric(metric.key)) {
          return false;
        }
        if (selectedType) {
          return selectedType === metric.type && !this.props.selectedMetrics.includes(metric.key);
        }
        return true;
      })
      .map((metric: Metric) => ({
        value: metric.key,
        label: metric.custom ? metric.name : translate('metric', metric.key, 'name')
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

  handleChange = (option: { value: string, label: string }) =>
    this.setState({ selectedMetric: option.value });

  handleSubmit = (e: Object) => {
    e.preventDefault();
    if (this.state.selectedMetric) {
      this.props.addMetric(this.state.selectedMetric);
      this.closeForm();
    }
  };

  renderModal() {
    return (
      <Modal
        isOpen={true}
        contentLabel="graph metric add"
        className="modal"
        overlayClassName="modal-overlay"
        onRequestClose={this.closeForm}>
        <header className="modal-head">
          <h2>{translate('project_activity.graphs.custom.add_metric')}</h2>
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
                options={this.getMetricsOptions()}
                placeholder=""
                searchable={true}
                value={this.state.selectedMetric}
              />
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
    return (
      <button
        className={this.props.className}
        onClick={this.openForm}
        disabled={this.props.selectedMetrics.length >= 3}>
        {translate('project_activity.graphs.custom.add')}
        {this.state.open && this.renderModal()}
      </button>
    );
  }
}
