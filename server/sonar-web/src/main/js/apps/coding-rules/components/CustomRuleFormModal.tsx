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
import FormattingTips from '../../../components/common/FormattingTips';
import TypeHelper from '../../../components/shared/TypeHelper';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { RULE_STATUSES, RULE_TYPES } from '../../../helpers/constants';
import { csvEscape } from '../../../helpers/csv';
import { translate } from '../../../helpers/l10n';
import { sanitizeString } from '../../../helpers/sanitize';
import { latinize } from '../../../helpers/strings';
import { useCreateRuleMutation, useUpdateRuleMutation } from '../../../queries/rules';
import { Dict, RuleDetails, RuleParameter, RuleType, Status } from '../../../types/types';
import { SeveritySelect } from './SeveritySelect';

interface Props {
  customRule?: RuleDetails;
  onClose: () => void;
  templateRule: RuleDetails;
}

const FORM_ID = 'custom-rule-form';

export default function CustomRuleFormModal(props: Readonly<Props>) {
  const { customRule, templateRule } = props;
  const [description, setDescription] = React.useState(customRule?.mdDesc ?? '');
  const [key, setKey] = React.useState(customRule?.key ?? '');
  const [keyModifiedByUser, setKeyModifiedByUser] = React.useState(false);
  const [name, setName] = React.useState(customRule?.name ?? '');
  const [params, setParams] = React.useState(getParams(customRule));
  const [reactivating, setReactivating] = React.useState(false);
  const [severity, setSeverity] = React.useState(customRule?.severity ?? templateRule.severity);
  const [status, setStatus] = React.useState(customRule?.status ?? templateRule.status);
  const [type, setType] = React.useState(customRule?.type ?? templateRule.type);
  const { mutate: updateRule, isLoading: updatingRule } = useUpdateRuleMutation(props.onClose);
  const { mutate: createRule, isLoading: creatingRule } = useCreateRuleMutation(
    {
      f: 'name,severity,params',
      template_key: templateRule.key,
    },
    props.onClose,
    (response: Response) => {
      setReactivating(response.status === HttpStatusCode.Conflict);
    },
  );

  const submitting = updatingRule || creatingRule;

  const submit = () => {
    const stringifiedParams = Object.keys(params)
      .map((key) => `${key}=${csvEscape(params[key])}`)
      .join(';');
    const ruleData = {
      markdownDescription: description,
      name,
      params: stringifiedParams,
      severity,
      status,
    };
    return customRule
      ? updateRule({ ...ruleData, key: customRule.key })
      : createRule({
          ...ruleData,
          customKey: key,
          preventReactivation: !reactivating,
          templateKey: templateRule.key,
          type,
        });
  };

  const NameField = React.useMemo(
    () => (
      <FormField
        ariaLabel={translate('name')}
        label={translate('name')}
        htmlFor="coding-rules-custom-rule-creation-name"
        required
      >
        <InputField
          autoFocus
          disabled={submitting}
          id="coding-rules-custom-rule-creation-name"
          onChange={({
            currentTarget: { value: name },
          }: React.SyntheticEvent<HTMLInputElement>) => {
            setName(name);
            setKey(keyModifiedByUser ? key : latinize(name).replace(/[^A-Za-z0-9]/g, '_'));
          }}
          required
          size="full"
          type="text"
          value={name}
        />
      </FormField>
    ),
    [key, keyModifiedByUser, name, submitting],
  );

  const KeyField = React.useMemo(
    () => (
      <FormField
        ariaLabel={translate('key')}
        label={translate('key')}
        htmlFor="coding-rules-custom-rule-creation-key"
        required
      >
        {customRule ? (
          <span title={customRule.key}>{customRule.key}</span>
        ) : (
          <InputField
            disabled={submitting}
            id="coding-rules-custom-rule-creation-key"
            onChange={(event: React.SyntheticEvent<HTMLInputElement>) => {
              setKey(event.currentTarget.value);
              setKeyModifiedByUser(true);
            }}
            required
            size="full"
            type="text"
            value={key}
          />
        )}
      </FormField>
    ),
    [customRule, key, submitting],
  );

  const DescriptionField = React.useMemo(
    () => (
      <FormField
        ariaLabel={translate('description')}
        label={translate('description')}
        htmlFor="coding-rules-custom-rule-creation-html-description"
        required
      >
        <InputTextArea
          disabled={submitting}
          id="coding-rules-custom-rule-creation-html-description"
          onChange={(event: React.SyntheticEvent<HTMLTextAreaElement>) =>
            setDescription(event.currentTarget.value)
          }
          required
          rows={5}
          size="full"
          value={description}
        />
        <FormattingTips />
      </FormField>
    ),
    [description, submitting],
  );

  const TypeField = React.useMemo(() => {
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
          isDisabled={submitting}
          isSearchable={false}
          onChange={({ value }: LabelValueSelectOption<RuleType>) => setType(value)}
          components={{
            Option: TypeSelectOption,
            SingleValue: TypeSelectValue,
          }}
          options={ruleTypeOption}
          value={ruleTypeOption.find((t) => t.value === type)}
        />
      </FormField>
    );
  }, [type, submitting]);

  const SeverityField = React.useMemo(
    () => (
      <FormField
        ariaLabel={translate('severity')}
        label={translate('severity')}
        htmlFor="coding-rules-severity-select"
      >
        <SeveritySelect
          isDisabled={submitting}
          onChange={({ value }: { value: string }) => setSeverity(value)}
          severity={severity}
        />
      </FormField>
    ),
    [severity, submitting],
  );

  const StatusField = React.useMemo(() => {
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
          isDisabled={submitting}
          aria-labelledby="coding-rules-custom-rule-status"
          onChange={({ value }: LabelValueSelectOption<Status>) => setStatus(value)}
          options={statusesOptions}
          isSearchable={false}
          value={statusesOptions.find((s) => s.value === status)}
        />
      </FormField>
    );
  }, [status, submitting]);

  const handleParameterChange = React.useCallback(
    (event: React.SyntheticEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      const { name, value } = event.currentTarget;
      setParams({ ...params, [name]: value });
    },
    [params],
  );

  const renderParameterField = React.useCallback(
    (param: RuleParameter) => {
      // Gets the actual value from params from the state.
      // Without it, we have a issue with string 'constructor' as key
      const actualValue = new Map(Object.entries(params)).get(param.key) ?? '';

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
              disabled={submitting}
              id={`coding-rule-custom-rule-${param.key}`}
              name={param.key}
              onChange={handleParameterChange}
              placeholder={param.defaultValue}
              size="full"
              rows={3}
              value={actualValue}
            />
          ) : (
            <InputField
              disabled={submitting}
              id={`coding-rule-custom-rule-${param.key}`}
              name={param.key}
              onChange={handleParameterChange}
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
    },
    [params, submitting, handleParameterChange],
  );

  const { params: templateParams = [] } = templateRule;
  const header = customRule
    ? translate('coding_rules.update_custom_rule')
    : translate('coding_rules.create_custom_rule');
  let buttonText = translate(customRule ? 'save' : 'create');
  if (reactivating) {
    buttonText = translate('coding_rules.reactivate');
  }
  return (
    <Modal
      headerTitle={header}
      onClose={props.onClose}
      body={
        <form
          className="sw-flex sw-flex-col sw-justify-stretch sw-pb-4"
          id={FORM_ID}
          onSubmit={(event: React.SyntheticEvent<HTMLFormElement>) => {
            event.preventDefault();
            submit();
          }}
        >
          {reactivating && (
            <FlagMessage variant="warning" className="sw-mb-6">
              {translate('coding_rules.reactivate.help')}
            </FlagMessage>
          )}

          <MandatoryFieldsExplanation className="sw-mb-4" />

          {NameField}
          {KeyField}
          {/* do not allow to change the type of existing rule */}
          {!customRule && TypeField}
          {SeverityField}
          {StatusField}
          {DescriptionField}
          {templateParams.map(renderParameterField)}
        </form>
      }
      primaryButton={
        <ButtonPrimary disabled={submitting} type="submit" form={FORM_ID}>
          {buttonText}
        </ButtonPrimary>
      }
      loading={submitting}
      secondaryButtonLabel={translate('cancel')}
    />
  );
}

function TypeSelectOption(
  optionProps: Readonly<OptionProps<LabelValueSelectOption<RuleType>, false>>,
) {
  return (
    <components.Option {...optionProps}>
      <TypeHelper type={optionProps.data.value} />
    </components.Option>
  );
}

function TypeSelectValue(
  valueProps: Readonly<SingleValueProps<LabelValueSelectOption<RuleType>, false>>,
) {
  return (
    <components.SingleValue {...valueProps}>
      <TypeHelper className="display-flex-center" type={valueProps.data.value} />
    </components.SingleValue>
  );
}

function getParams(customRule?: RuleDetails) {
  const params: Dict<string> = {};

  if (customRule?.params) {
    for (const param of customRule.params) {
      params[param.key] = param.defaultValue ?? '';
    }
  }

  return params;
}
