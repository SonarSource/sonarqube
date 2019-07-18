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
import Modal from 'sonar-ui-common/components/controls/Modal';
import Select from 'sonar-ui-common/components/controls/Select';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { csvEscape } from 'sonar-ui-common/helpers/csv';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { latinize } from 'sonar-ui-common/helpers/strings';
import { createRule, updateRule } from '../../../api/rules';
import MarkdownTips from '../../../components/common/MarkdownTips';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import TypeHelper from '../../../components/shared/TypeHelper';
import { RULE_STATUSES, RULE_TYPES, SEVERITIES } from '../../../helpers/constants';

interface Props {
  customRule?: T.RuleDetails;
  onClose: () => void;
  onDone: (newRuleDetails: T.RuleDetails) => void;
  organization: string | undefined;
  templateRule: T.RuleDetails;
}

interface State {
  description: string;
  key: string;
  keyModifiedByUser: boolean;
  name: string;
  params: T.Dict<string>;
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
    const params: T.Dict<string> = {};
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

  prepareRequest = () => {
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
    this.setState(state => ({
      name,
      key: state.keyModifiedByUser ? state.key : latinize(name).replace(/[^A-Za-z0-9]/g, '_')
    }));
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
    <div className="modal-field">
      <label htmlFor="coding-rules-custom-rule-creation-name">
        {translate('name')} <em className="mandatory">*</em>
      </label>
      <input
        autoFocus={true}
        disabled={this.state.submitting}
        id="coding-rules-custom-rule-creation-name"
        onChange={this.handleNameChange}
        required={true}
        type="text"
        value={this.state.name}
      />
    </div>
  );

  renderKeyField = () => (
    <div className="modal-field">
      <label htmlFor="coding-rules-custom-rule-creation-key">
        {translate('key')} {!this.props.customRule && <em className="mandatory">*</em>}
      </label>

      {this.props.customRule ? (
        <span className="coding-rules-detail-custom-rule-key" title={this.props.customRule.key}>
          {this.props.customRule.key}
        </span>
      ) : (
        <input
          disabled={this.state.submitting}
          id="coding-rules-custom-rule-creation-key"
          onChange={this.handleKeyChange}
          required={true}
          type="text"
          value={this.state.key}
        />
      )}
    </div>
  );

  renderDescriptionField = () => (
    <div className="modal-field">
      <label htmlFor="coding-rules-custom-rule-creation-html-description">
        {translate('description')} <em className="mandatory">*</em>
      </label>
      <textarea
        disabled={this.state.submitting}
        id="coding-rules-custom-rule-creation-html-description"
        onChange={this.handleDescriptionChange}
        required={true}
        rows={5}
        value={this.state.description}
      />
      <MarkdownTips className="modal-field-descriptor text-right" />
    </div>
  );

  renderTypeOption = ({ value }: { value: T.RuleType }) => {
    return <TypeHelper type={value} />;
  };

  renderTypeField = () => (
    <div className="modal-field flex-1 spacer-right">
      <label htmlFor="coding-rules-custom-rule-type">{translate('type')}</label>
      <Select
        clearable={false}
        disabled={this.state.submitting}
        id="coding-rules-custom-rule-type"
        onChange={this.handleTypeChange}
        optionRenderer={this.renderTypeOption}
        options={RULE_TYPES.map(type => ({
          label: translate('issue.type', type),
          value: type
        }))}
        searchable={false}
        value={this.state.type}
        valueRenderer={this.renderTypeOption}
      />
    </div>
  );

  renderSeverityOption = ({ value }: { value: string }) => <SeverityHelper severity={value} />;

  renderSeverityField = () => (
    <div className="modal-field flex-1 spacer-right">
      <label htmlFor="coding-rules-custom-rule-severity">{translate('severity')}</label>
      <Select
        clearable={false}
        disabled={this.state.submitting}
        id="coding-rules-custom-rule-severity"
        onChange={this.handleSeverityChange}
        optionRenderer={this.renderSeverityOption}
        options={SEVERITIES.map(severity => ({
          label: translate('severity', severity),
          value: severity
        }))}
        searchable={false}
        value={this.state.severity}
        valueRenderer={this.renderSeverityOption}
      />
    </div>
  );

  renderStatusField = () => (
    <div className="modal-field flex-1">
      <label htmlFor="coding-rules-custom-rule-status">
        {translate('coding_rules.filters.status')}
      </label>
      <Select
        clearable={false}
        disabled={this.state.submitting}
        id="coding-rules-custom-rule-status"
        onChange={this.handleStatusChange}
        options={RULE_STATUSES.map(status => ({
          label: translate('rules.status', status),
          value: status
        }))}
        searchable={false}
        value={this.state.status}
      />
    </div>
  );

  renderParameterField = (param: T.RuleParameter) => (
    <div className="modal-field" key={param.key}>
      <label className="capitalize" htmlFor={param.key}>
        {param.key}
      </label>

      {param.type === 'TEXT' ? (
        <textarea
          disabled={this.state.submitting}
          id={param.key}
          name={param.key}
          onChange={this.handleParameterChange}
          placeholder={param.defaultValue}
          rows={3}
          value={this.state.params[param.key] || ''}
        />
      ) : (
        <input
          disabled={this.state.submitting}
          id={param.key}
          name={param.key}
          onChange={this.handleParameterChange}
          placeholder={param.defaultValue}
          type="text"
          value={this.state.params[param.key] || ''}
        />
      )}
      <div
        className="modal-field-description"
        // Safe: defined by rule creator (instance admin?)
        dangerouslySetInnerHTML={{ __html: param.htmlDesc || '' }}
      />
    </div>
  );

  render() {
    const { customRule, templateRule } = this.props;
    const { reactivating, submitting } = this.state;
    const { params = [] } = templateRule;
    const header = customRule
      ? translate('coding_rules.update_custom_rule')
      : translate('coding_rules.create_custom_rule');
    let submit = this.props.customRule ? translate('save') : translate('create');
    if (this.state.reactivating) {
      submit = translate('coding_rules.reactivate');
    }
    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <form onSubmit={this.handleFormSubmit}>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>

          <div className="modal-body modal-container">
            {reactivating && (
              <Alert variant="warning">{translate('coding_rules.reactivate.help')}</Alert>
            )}

            {this.renderNameField()}
            {this.renderKeyField()}
            <div className="display-flex-space-between">
              {/* do not allow to change the type of existing rule */}
              {!customRule && this.renderTypeField()}
              {this.renderSeverityField()}
              {this.renderStatusField()}
            </div>
            {this.renderDescriptionField()}
            {params.map(this.renderParameterField)}
          </div>

          <div className="modal-foot">
            {submitting && <i className="spinner spacer-right" />}
            <SubmitButton disabled={this.state.submitting}>{submit}</SubmitButton>
            <ResetButtonLink
              disabled={submitting}
              id="coding-rules-custom-rule-creation-cancel"
              onClick={this.props.onClose}>
              {translate('cancel')}
            </ResetButtonLink>
          </div>
        </form>
      </Modal>
    );
  }
}
