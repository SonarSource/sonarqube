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
import { RuleDetails, RuleParameter } from '../../../app/types';
import Modal from '../../../components/controls/Modal';
import { translate } from '../../../helpers/l10n';
import MarkdownTips from '../../../components/common/MarkdownTips';
import { SEVERITIES, TYPES, RULE_STATUSES } from '../../../helpers/constants';
import latinize from '../../../helpers/latinize';
import Select from '../../../components/controls/Select';
import TypeHelper from '../../../components/shared/TypeHelper';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import { createRule, updateRule } from '../../../api/rules';
import { csvEscape } from '../../../helpers/csv';

interface Props {
  customRule?: RuleDetails;
  onClose: () => void;
  onDone: (newRuleDetails: RuleDetails) => void;
  organization: string | undefined;
  templateRule: RuleDetails;
}

interface State {
  description: string;
  key: string;
  keyModifiedByUser: boolean;
  name: string;
  params: { [p: string]: string };
  reactivating: boolean;
  severity: string;
  status: string;
  submitting: boolean;
  type: string;
}

export default class CustomRuleFormModal extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    const params: { [p: string]: string } = {};
    if (props.customRule && props.customRule.params) {
      for (const param of props.customRule.params) {
        params[param.key] = param.defaultValue || '';
      }
    }
    this.state = {
      description: (props.customRule && props.customRule.mdDesc) || '',
      key: '',
      keyModifiedByUser: false,
      name: (props.customRule && props.customRule.name) || '',
      params,
      reactivating: false,
      severity: (props.customRule && props.customRule.severity) || props.templateRule.severity,
      status: (props.customRule && props.customRule.status) || props.templateRule.status,
      submitting: false,
      type: (props.customRule && props.customRule.type) || props.templateRule.type
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleCancelClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onClose();
  };

  prepareRequest = () => {
    /* eslint-disable camelcase */
    const { customRule, organization, templateRule } = this.props;
    const params = Object.keys(this.state.params)
      .map(key => `${key}=${csvEscape(this.state.params[key])}`)
      .join(';');
    const ruleData = {
      markdown_description: this.state.description,
      name: this.state.name,
      organization,
      params,
      severity: this.state.severity,
      status: this.state.status
    };
    return customRule
      ? updateRule({ ...ruleData, key: customRule.key })
      : createRule({
          ...ruleData,
          custom_key: this.state.key,
          prevent_reactivation: !this.state.reactivating,
          template_key: templateRule.key,
          type: this.state.type
        });
    /* eslint-enable camelcase */
  };

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ submitting: true });
    this.prepareRequest().then(
      newRuleDetails => {
        if (this.mounted) {
          this.setState({ submitting: false });
          this.props.onDone(newRuleDetails);
        }
      },
      (response: Response) => {
        if (this.mounted) {
          this.setState({ reactivating: response.status === 409, submitting: false });
        }
      }
    );
  };

  handleNameChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    const { value: name } = event.currentTarget;
    this.setState((state: State) => {
      const change: Partial<State> = { name };
      if (!state.keyModifiedByUser) {
        change.key = latinize(name).replace(/[^A-Za-z0-9]/g, '_');
      }
      return change;
    });
  };

  handleKeyChange = (event: React.SyntheticEvent<HTMLInputElement>) =>
    this.setState({ key: event.currentTarget.value, keyModifiedByUser: true });

  handleDescriptionChange = (event: React.SyntheticEvent<HTMLTextAreaElement>) =>
    this.setState({ description: event.currentTarget.value });

  handleTypeChange = ({ value }: { value: string }) => this.setState({ type: value });

  handleSeverityChange = ({ value }: { value: string }) => this.setState({ severity: value });

  handleStatusChange = ({ value }: { value: string }) => this.setState({ status: value });

  handleParameterChange = (event: React.SyntheticEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = event.currentTarget;
    this.setState((state: State) => ({ params: { ...state.params, [name]: value } }));
  };

  renderNameField = () => (
    <tr className="property">
      <th className="nowrap">
        <h3>
          {translate('name')} <em className="mandatory">*</em>
        </h3>
      </th>
      <td>
        <input
          autoFocus={true}
          className="coding-rules-name-key"
          disabled={this.state.submitting}
          id="coding-rules-custom-rule-creation-name"
          onChange={this.handleNameChange}
          required={true}
          type="text"
          value={this.state.name}
        />
      </td>
    </tr>
  );

  renderKeyField = () => (
    <tr className="property">
      <th className="nowrap">
        <h3>
          {translate('key')} {!this.props.customRule && <em className="mandatory">*</em>}
        </h3>
      </th>
      <td>
        {this.props.customRule ? (
          <span className="coding-rules-detail-custom-rule-key" title={this.props.customRule.key}>
            {this.props.customRule.key}
          </span>
        ) : (
          <input
            className="coding-rules-name-key"
            disabled={this.state.submitting}
            id="coding-rules-custom-rule-creation-key"
            onChange={this.handleKeyChange}
            required={true}
            type="text"
            value={this.state.key}
          />
        )}
      </td>
    </tr>
  );

  renderDescriptionField = () => (
    <tr className="property">
      <th className="nowrap">
        <h3>
          {translate('description')} <em className="mandatory">*</em>
        </h3>
      </th>
      <td>
        <textarea
          className="coding-rules-markdown-description"
          disabled={this.state.submitting}
          id="coding-rules-custom-rule-creation-html-description"
          onChange={this.handleDescriptionChange}
          required={true}
          rows={5}
          value={this.state.description}
        />
        <span className="text-right">
          <MarkdownTips />
        </span>
      </td>
    </tr>
  );

  renderTypeOption = ({ value }: { value: string }) => <TypeHelper type={value} />;

  renderTypeField = () => (
    <tr className="property">
      <th className="nowrap">
        <h3>{translate('type')}</h3>
      </th>
      <td>
        <Select
          className="input-medium"
          clearable={false}
          disabled={this.state.submitting}
          onChange={this.handleTypeChange}
          options={TYPES.map(type => ({
            label: translate('issue.type', type),
            value: type
          }))}
          optionRenderer={this.renderTypeOption}
          searchable={false}
          value={this.state.type}
          valueRenderer={this.renderTypeOption}
        />
      </td>
    </tr>
  );

  renderSeverityOption = ({ value }: { value: string }) => <SeverityHelper severity={value} />;

  renderSeverityField = () => (
    <tr className="property">
      <th className="nowrap">
        <h3>{translate('severity')}</h3>
      </th>
      <td>
        <Select
          className="input-medium"
          clearable={false}
          disabled={this.state.submitting}
          onChange={this.handleSeverityChange}
          options={SEVERITIES.map(severity => ({
            label: translate('severity', severity),
            value: severity
          }))}
          optionRenderer={this.renderSeverityOption}
          searchable={false}
          value={this.state.severity}
          valueRenderer={this.renderSeverityOption}
        />
      </td>
    </tr>
  );

  renderStatusField = () => (
    <tr className="property">
      <th className="nowrap">
        <h3>{translate('coding_rules.filters.status')}</h3>
      </th>
      <td>
        <Select
          className="input-medium"
          clearable={false}
          disabled={this.state.submitting}
          onChange={this.handleStatusChange}
          options={RULE_STATUSES.map(status => ({
            label: translate('rules.status', status),
            value: status
          }))}
          searchable={false}
          value={this.state.status}
        />
      </td>
    </tr>
  );

  renderParameterField = (param: RuleParameter) => (
    <tr className="property" key={param.key}>
      <th className="nowrap">
        <h3>{param.key}</h3>
      </th>
      <td>
        {param.type === 'TEXT' ? (
          <textarea
            className="width100"
            disabled={this.state.submitting}
            name={param.key}
            onChange={this.handleParameterChange}
            placeholder={param.defaultValue}
            rows={3}
            value={this.state.params[param.key] || ''}
          />
        ) : (
          <input
            className="input-super-large"
            disabled={this.state.submitting}
            name={param.key}
            onChange={this.handleParameterChange}
            placeholder={param.defaultValue}
            type="text"
            value={this.state.params[param.key] || ''}
          />
        )}
        <div className="note" dangerouslySetInnerHTML={{ __html: param.htmlDesc || '' }} />
        {param.extra && <div className="note">{param.extra}</div>}
      </td>
    </tr>
  );

  renderSubmitButton = () => {
    if (this.state.reactivating) {
      return (
        <button
          disabled={this.state.submitting}
          id="coding-rules-custom-rule-creation-reactivate"
          type="submit">
          {translate('coding_rules.reactivate')}
        </button>
      );
    } else {
      return (
        <button
          disabled={this.state.submitting}
          id="coding-rules-custom-rule-creation-create"
          type="submit">
          {translate(this.props.customRule ? 'save' : 'create')}
        </button>
      );
    }
  };

  render() {
    const { customRule, templateRule } = this.props;
    const { reactivating, submitting } = this.state;
    const { params = [] } = templateRule;
    const header = translate(
      customRule ? 'coding_rules.update_custom_rule' : 'coding_rules.create_custom_rule'
    );
    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <form onSubmit={this.handleFormSubmit}>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>

          <div className="modal-body modal-container">
            {reactivating && (
              <div className="alert alert-warning">{translate('coding_rules.reactivate.help')}</div>
            )}
            <table>
              <tbody>
                {this.renderNameField()}
                {this.renderKeyField()}
                {this.renderDescriptionField()}
                {/* do not allow to change the type of existing rule */}
                {!customRule && this.renderTypeField()}
                {this.renderSeverityField()}
                {this.renderStatusField()}
                {params.map(this.renderParameterField)}
              </tbody>
            </table>
          </div>

          <div className="modal-foot">
            {submitting && <i className="spinner spacer-right" />}
            {this.renderSubmitButton()}
            <button
              className="button-link"
              disabled={submitting}
              id="coding-rules-custom-rule-creation-cancel"
              onClick={this.handleCancelClick}
              type="reset">
              {translate('cancel')}
            </button>
          </div>
        </form>
      </Modal>
    );
  }
}
