/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { HttpStatusCode } from 'axios';
import {
  ButtonPrimary,
  FlagMessage,
  FormField,
  InputField,
  InputSelect,
  InputTextArea,
  LabelValueSelectOption,
  LightLabel,
  Modal,
} from 'design-system';
import * as React from 'react';
import { OptionProps, SingleValueProps, components } from 'react-select';
import { createRule, updateRule } from '../../../api/rules';
import FormattingTips from '../../../components/common/FormattingTips';
import TypeHelper from '../../../components/shared/TypeHelper';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { RULE_STATUSES, RULE_TYPES } from '../../../helpers/constants';
import { csvEscape } from '../../../helpers/csv';
import { translate } from '../../../helpers/l10n';
import { sanitizeString } from '../../../helpers/sanitize';
import { latinize } from '../../../helpers/strings';
import { Dict, RuleDetails, RuleParameter, RuleType, Status } from '../../../types/types';
import { SeveritySelect } from './SeveritySelect';

interface Props {
  customRule?: RuleDetails;
  onClose: () => void;
  onDone: (newRuleDetails: RuleDetails) => void;
  templateRule: RuleDetails;
}

interface State {
  description: string;
  key: string;
  keyModifiedByUser: boolean;
  name: string;
  params: Dict<string>;
  reactivating: boolean;
  severity: string;
  status: string;
  submitting: boolean;
  type: RuleType;
}

const FORM_ID = 'custom-rule-form';

export default class CustomRuleFormModal extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    const params: Dict<string> = {};
    if (props.customRule?.params) {
      for (const param of props.customRule.params) {
        params[param.key] = param.defaultValue ?? '';
      }
    }
    this.state = {
      description: props.customRule?.mdDesc ?? '',
      key: '',
      keyModifiedByUser: false,
      name: props.customRule?.name ?? '',
      params,
      reactivating: false,
      severity: props.customRule?.severity ?? props.templateRule.severity,
      status: props.customRule?.status ?? props.templateRule.status,
      submitting: false,
      type: props.customRule?.type ?? props.templateRule.type,
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  prepareRequest = () => {
    const { customRule, templateRule } = this.props;
    const params = Object.keys(this.state.params)
      .map((key) => `${key}=${csvEscape(this.state.params[key])}`)
      .join(';');
    const ruleData = {
      markdownDescription: this.state.description,
      name: this.state.name,
      params,
      severity: this.state.severity,
      status: this.state.status,
    };
    return customRule
      ? updateRule({ ...ruleData, key: customRule.key })
      : createRule({
          ...ruleData,
          customKey: this.state.key,
          preventReactivation: !this.state.reactivating,
          templateKey: templateRule.key,
          type: this.state.type,
        });
  };

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ submitting: true });
    this.prepareRequest().then(
      (newRuleDetails) => {
        if (this.mounted) {
          this.setState({ submitting: false });
          this.props.onDone(newRuleDetails);
        }
      },
      (response: Response) => {
        if (this.mounted) {
          this.setState({
            reactivating: response.status === HttpStatusCode.Conflict,
            submitting: false,
          });
        }
      },
    );
  };

  handleNameChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    const { value: name } = event.currentTarget;
    this.setState((state) => ({
      name,
      key: state.keyModifiedByUser ? state.key : latinize(name).replace(/[^A-Za-z0-9]/g, '_'),
    }));
  };

  handleKeyChange = (event: React.SyntheticEvent<HTMLInputElement>) =>
    this.setState({ key: event.currentTarget.value, keyModifiedByUser: true });

  handleDescriptionChange = (event: React.SyntheticEvent<HTMLTextAreaElement>) =>
    this.setState({ description: event.currentTarget.value });

  handleTypeChange = ({ value }: LabelValueSelectOption<RuleType>) =>
    this.setState({ type: value });

  handleSeverityChange = ({ value }: { value: string }) => this.setState({ severity: value });

  handleStatusChange = ({ value }: LabelValueSelectOption<Status>) =>
    this.setState({ status: value });

  handleParameterChange = (event: React.SyntheticEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = event.currentTarget;
    this.setState((state: State) => ({ params: { ...state.params, [name]: value } }));
  };

  renderNameField = () => (
    <FormField
      ariaLabel={translate('name')}
      label={translate('name')}
      htmlFor="coding-rules-custom-rule-creation-name"
      required
    >
      <InputField
        autoFocus
        disabled={this.state.submitting}
        id="coding-rules-custom-rule-creation-name"
        onChange={this.handleNameChange}
        required
        size="full"
        type="text"
        value={this.state.name}
      />
    </FormField>
  );

  renderKeyField = () => (
    <FormField
      ariaLabel={translate('key')}
      label={translate('key')}
      htmlFor="coding-rules-custom-rule-creation-key"
      required
    >
      {this.props.customRule ? (
        <span title={this.props.customRule.key}>{this.props.customRule.key}</span>
      ) : (
        <InputField
          disabled={this.state.submitting}
          id="coding-rules-custom-rule-creation-key"
          onChange={this.handleKeyChange}
          required
          size="full"
          type="text"
          value={this.state.key}
        />
      )}
    </FormField>
  );

  renderDescriptionField = () => (
    <FormField
      ariaLabel={translate('description')}
      label={translate('description')}
      htmlFor="coding-rules-custom-rule-creation-html-description"
      required
    >
      <InputTextArea
        disabled={this.state.submitting}
        id="coding-rules-custom-rule-creation-html-description"
        onChange={this.handleDescriptionChange}
        required
        rows={5}
        size="full"
        value={this.state.description}
      />
      <FormattingTips />
    </FormField>
  );

  renderTypeOption = (props: OptionProps<LabelValueSelectOption<RuleType>, false>) => {
    return (
      <components.Option {...props}>
        <TypeHelper type={props.data.value} />
      </components.Option>
    );
  };

  renderTypeSingleValue = (props: SingleValueProps<LabelValueSelectOption<RuleType>, false>) => {
    return (
      <components.SingleValue {...props}>
        <TypeHelper className="display-flex-center" type={props.data.value} />
      </components.SingleValue>
    );
  };

  renderTypeField = () => {
    const ruleTypeOption: LabelValueSelectOption<RuleType>[] = RULE_TYPES.map((type) => ({
      label: translate('issue.type', type),
      value: type,
    }));
    return (
      <FormField
        ariaLabel={translate('type')}
        label={translate('type')}
        htmlFor="coding-rules-custom-rule-type"
      >
        <InputSelect
          inputId="coding-rules-custom-rule-type"
          isClearable={false}
          isDisabled={this.state.submitting}
          isSearchable={false}
          onChange={this.handleTypeChange}
          components={{
            Option: this.renderTypeOption,
            SingleValue: this.renderTypeSingleValue,
          }}
          options={ruleTypeOption}
          value={ruleTypeOption.find((t) => t.value === this.state.type)}
        />
      </FormField>
    );
  };

  renderSeverityField = () => (
    <FormField
      ariaLabel={translate('severity')}
      label={translate('severity')}
      htmlFor="coding-rules-severity-select"
    >
      <SeveritySelect
        isDisabled={this.state.submitting}
        onChange={this.handleSeverityChange}
        severity={this.state.severity}
      />
    </FormField>
  );

  renderStatusField = () => {
    const statusesOptions = RULE_STATUSES.map((status) => ({
      label: translate('rules.status', status),
      value: status,
    }));
    return (
      <FormField
        ariaLabel={translate('coding_rules.filters.status')}
        label={translate('coding_rules.filters.status')}
        htmlFor="coding-rules-custom-rule-status"
      >
        <InputSelect
          inputId="coding-rules-custom-rule-status"
          isClearable={false}
          isDisabled={this.state.submitting}
          aria-labelledby="coding-rules-custom-rule-status"
          onChange={this.handleStatusChange}
          options={statusesOptions}
          isSearchable={false}
          value={statusesOptions.find((s) => s.value === this.state.status)}
        />
      </FormField>
    );
  };

  renderParameterField = (param: RuleParameter) => {
    // Gets the actual value from params from the state.
    // Without it, we have a issue with string 'constructor' as key
    const actualValue = new Map(Object.entries(this.state.params)).get(param.key) ?? '';

    return (
      <FormField
        ariaLabel={param.key}
        className="sw-capitalize"
        label={param.key}
        htmlFor={`coding-rule-custom-rule-${param.key}`}
        key={param.key}
      >
        {param.type === 'TEXT' ? (
          <InputTextArea
            disabled={this.state.submitting}
            id={`coding-rule-custom-rule-${param.key}`}
            name={param.key}
            onChange={this.handleParameterChange}
            placeholder={param.defaultValue}
            size="full"
            rows={3}
            value={actualValue}
          />
        ) : (
          <InputField
            disabled={this.state.submitting}
            id={`coding-rule-custom-rule-${param.key}`}
            name={param.key}
            onChange={this.handleParameterChange}
            placeholder={param.defaultValue}
            size="full"
            type="text"
            value={actualValue}
          />
        )}
        {param.htmlDesc !== undefined && (
          <LightLabel
            // eslint-disable-next-line react/no-danger
            dangerouslySetInnerHTML={{ __html: sanitizeString(param.htmlDesc) }}
          />
        )}
      </FormField>
    );
  };

  render() {
    const { customRule, templateRule } = this.props;
    const { reactivating, submitting } = this.state;
    const { params = [] } = templateRule;
    const header = customRule
      ? translate('coding_rules.update_custom_rule')
      : translate('coding_rules.create_custom_rule');
    let submit = translate(customRule ? 'save' : 'create');
    if (this.state.reactivating) {
      submit = translate('coding_rules.reactivate');
    }
    return (
      <Modal
        headerTitle={header}
        onClose={this.props.onClose}
        body={
          <form
            className="sw-flex sw-flex-col sw-justify-stretch sw-pb-4"
            id={FORM_ID}
            onSubmit={this.handleFormSubmit}
          >
            {reactivating && (
              <FlagMessage variant="warning" className="sw-mb-6">
                {translate('coding_rules.reactivate.help')}
              </FlagMessage>
            )}

            <MandatoryFieldsExplanation className="sw-mb-4" />

            {this.renderNameField()}
            {this.renderKeyField()}
            {/* do not allow to change the type of existing rule */}
            {!customRule && this.renderTypeField()}
            {this.renderSeverityField()}
            {this.renderStatusField()}
            {this.renderDescriptionField()}
            {params.map(this.renderParameterField)}
          </form>
        }
        primaryButton={
          <ButtonPrimary disabled={submitting} type="submit" form={FORM_ID}>
            {submit}
          </ButtonPrimary>
        }
        loading={submitting}
        secondaryButtonLabel={translate('cancel')}
      />
    );
  }
}
