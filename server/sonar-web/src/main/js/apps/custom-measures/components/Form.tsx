/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { ResetButtonLink, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import Select from 'sonar-ui-common/components/controls/Select';
import SimpleModal from 'sonar-ui-common/components/controls/SimpleModal';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getAllMetrics } from '../../../api/metrics';

interface Props {
  confirmButtonText: string;
  header: string;
  measure?: T.CustomMeasure;
  onClose: () => void;
  onSubmit: (data: { description: string; metricKey: string; value: string }) => Promise<void>;
  skipMetrics?: string[];
}

interface State {
  description: string;
  loading: boolean;
  metricKey?: string;
  metrics?: T.Metric[];
  value: string;
}

export default class Form extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      description: (props.measure && props.measure.description) || '',
      loading: false,
      metricKey: props.measure && props.measure.metric.key,
      value: (props.measure && props.measure.value) || ''
    };
  }

  componentDidMount() {
    this.mounted = true;
    if (!this.props.measure) {
      this.fetchCustomMetrics();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleSubmit = () => {
    return this.state.metricKey
      ? this.props
          .onSubmit({
            description: this.state.description,
            metricKey: this.state.metricKey,
            value: this.state.value
          })
          .then(this.props.onClose)
      : Promise.reject(undefined);
  };

  fetchCustomMetrics = () => {
    this.setState({ loading: true });
    getAllMetrics({ isCustom: true }).then(
      metrics => {
        if (this.mounted) {
          this.setState({ loading: false, metrics });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  handleMetricSelect = ({ value }: { value: string }) => {
    this.setState({ metricKey: value });
  };

  handleDescriptionChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.setState({ description: event.currentTarget.value });
  };

  handleValueChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ value: event.currentTarget.value });
  };

  renderMetricSelect = (options: { label: string; value: string }[]) => {
    if (!options.length && !this.state.loading) {
      return <Alert variant="warning">{translate('custom_measures.all_metrics_taken')}</Alert>;
    }
    return (
      <div className="modal-field">
        <label htmlFor="create-custom-measure-metric">
          {translate('custom_measures.metric')}
          <em className="mandatory">*</em>
        </label>
        {this.state.loading ? (
          <i className="spinner" />
        ) : (
          <Select
            autoFocus={true}
            clearable={false}
            id="create-custom-measure-metric"
            onChange={this.handleMetricSelect}
            options={options}
            value={this.state.metricKey}
          />
        )}
      </div>
    );
  };

  render() {
    const { skipMetrics = [] } = this.props;
    const { metrics = [] } = this.state;
    const options = metrics
      .filter(metric => !skipMetrics.includes(metric.key))
      .map(metric => ({ label: metric.name, value: metric.key }));
    const forbidSubmitting = !this.props.measure && !options.length;

    return (
      <SimpleModal
        header={this.props.header}
        onClose={this.props.onClose}
        onSubmit={this.handleSubmit}
        size="small">
        {({ onCloseClick, onFormSubmit, submitting }) => (
          <form onSubmit={onFormSubmit}>
            <header className="modal-head">
              <h2>{this.props.header}</h2>
            </header>

            <div className="modal-body">
              {!this.props.measure && this.renderMetricSelect(options)}

              <div className="modal-field">
                <label htmlFor="create-custom-measure-value">
                  {translate('value')}
                  <em className="mandatory">*</em>
                </label>
                <input
                  autoFocus={this.props.measure !== undefined}
                  id="create-custom-measure-value"
                  maxLength={200}
                  name="value"
                  onChange={this.handleValueChange}
                  required={true}
                  type="text"
                  value={this.state.value}
                />
              </div>
              <div className="modal-field">
                <label htmlFor="create-custom-measure-description">
                  {translate('description')}
                </label>
                <textarea
                  id="create-custom-measure-description"
                  name="description"
                  onChange={this.handleDescriptionChange}
                  value={this.state.description}
                />
              </div>
            </div>

            <footer className="modal-foot">
              <DeferredSpinner className="spacer-right" loading={submitting} />
              <SubmitButton
                disabled={forbidSubmitting || submitting}
                id="create-custom-measure-submit">
                {this.props.confirmButtonText}
              </SubmitButton>
              <ResetButtonLink
                disabled={submitting}
                id="create-custom-measure-cancel"
                onClick={onCloseClick}>
                {translate('cancel')}
              </ResetButtonLink>
            </footer>
          </form>
        )}
      </SimpleModal>
    );
  }
}
