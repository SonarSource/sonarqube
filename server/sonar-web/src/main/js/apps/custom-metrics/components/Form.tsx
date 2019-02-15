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
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import SimpleModal from '../../../components/controls/SimpleModal';
import Select, { Creatable } from '../../../components/controls/Select';
import { SubmitButton, ResetButtonLink } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';

export interface MetricProps {
  description: string;
  domain?: string;
  key: string;
  name: string;
  type: string;
}

interface Props {
  confirmButtonText: string;
  domains: string[];
  metric?: T.Metric;
  header: string;
  onClose: () => void;
  onSubmit: (data: MetricProps) => Promise<void>;
  types: string[];
}

interface State extends MetricProps {}

export default class Form extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      description: (props.metric && props.metric.description) || '',
      domain: props.metric && props.metric.domain,
      key: (props.metric && props.metric.key) || '',
      name: (props.metric && props.metric.name) || '',
      type: (props.metric && props.metric.type) || 'INT'
    };
  }

  handleSubmit = () => {
    return this.props
      .onSubmit({
        description: this.state.description,
        domain: this.state.domain,
        key: this.state.key,
        name: this.state.name,
        type: this.state.type
      })
      .then(this.props.onClose);
  };

  handleKeyChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ key: event.currentTarget.value });
  };

  handleDescriptionChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.setState({ description: event.currentTarget.value });
  };

  handleNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleDomainChange = (option: { value: string } | null) => {
    this.setState({ domain: option ? option.value : undefined });
  };

  handleTypeChange = ({ value }: { value: string }) => {
    this.setState({ type: value });
  };

  render() {
    const domains = [...this.props.domains];
    if (this.state.domain) {
      domains.push(this.state.domain);
    }

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
              <div className="modal-field">
                <label htmlFor="create-metric-key">
                  {translate('key')}
                  <em className="mandatory">*</em>
                </label>
                <input
                  autoFocus={true}
                  id="create-metric-key"
                  maxLength={64}
                  name="key"
                  onChange={this.handleKeyChange}
                  required={true}
                  type="text"
                  value={this.state.key}
                />
              </div>
              <div className="modal-field">
                <label htmlFor="create-metric-name">
                  {translate('name')}
                  <em className="mandatory">*</em>
                </label>
                <input
                  id="create-metric-name"
                  maxLength={64}
                  name="name"
                  onChange={this.handleNameChange}
                  required={true}
                  type="text"
                  value={this.state.name}
                />
              </div>
              <div className="modal-field">
                <label htmlFor="create-metric-description">{translate('description')}</label>
                <textarea
                  id="create-metric-description"
                  name="description"
                  onChange={this.handleDescriptionChange}
                  value={this.state.description}
                />
              </div>
              <div className="modal-field">
                <label htmlFor="create-metric-domain">{translate('custom_metrics.domain')}</label>
                <Creatable
                  id="create-metric-domain"
                  onChange={this.handleDomainChange}
                  options={domains.map(domain => ({ label: domain, value: domain }))}
                  value={this.state.domain}
                />
              </div>
              <div className="modal-field">
                <label htmlFor="create-metric-type">
                  {translate('type')}
                  <em className="mandatory">*</em>
                </label>
                <Select
                  clearable={false}
                  id="create-metric-type"
                  onChange={this.handleTypeChange}
                  options={this.props.types.map(type => ({
                    label: translate('metric.type', type),
                    value: type
                  }))}
                  value={this.state.type}
                />
              </div>
            </div>

            <footer className="modal-foot">
              <DeferredSpinner className="spacer-right" loading={submitting} />
              <SubmitButton disabled={submitting} id="create-metric-submit">
                {this.props.confirmButtonText}
              </SubmitButton>
              <ResetButtonLink
                disabled={submitting}
                id="create-metric-cancel"
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
