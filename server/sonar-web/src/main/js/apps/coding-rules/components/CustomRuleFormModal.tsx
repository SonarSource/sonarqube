/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import { Button, ButtonVariety, Modal, ModalSize, Select, Text } from '@sonarsource/echoes-react';
import { HttpStatusCode } from 'axios';
import { SyntheticEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  FlagMessage,
  FormField,
  InputField,
  InputTextArea,
  LabelValueSelectOption,
  SafeHTMLInjection,
  SanitizeLevel,
} from '~design-system';
import FormattingTips from '../../../components/common/FormattingTips';
import IssueTypeIcon from '../../../components/icon-mappers/IssueTypeIcon';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { RULE_STATUSES, RULE_TYPES } from '../../../helpers/constants';
import { csvEscape } from '../../../helpers/csv';
import { translate } from '../../../helpers/l10n';
import { latinize } from '../../../helpers/strings';
import { useCreateRuleMutation, useUpdateRuleMutation } from '../../../queries/rules';
import { useStandardExperienceMode } from '../../../queries/settings';
import {
  CleanCodeAttribute,
  CleanCodeAttributeCategory,
  SoftwareImpact,
} from '../../../types/clean-code-taxonomy';
import { CustomRuleType, Dict, RuleDetails, RuleParameter, RuleType } from '../../../types/types';
import {
  CleanCodeAttributeField,
  CleanCodeCategoryField,
  SoftwareQualitiesFields,
} from './CustomRuleFormFieldsCCT';
import { SeveritySelect } from './SeveritySelect';

interface Props {
  customRule?: RuleDetails;
  isOpen: boolean;
  onClose: () => void;
  templateRule: RuleDetails;
}

const FORM_ID = 'custom-rule-form';

export default function CustomRuleFormModal(props: Readonly<Props>) {
  const { customRule, templateRule, isOpen } = props;
  const { data: isStandardMode } = useStandardExperienceMode();
  const [description, setDescription] = useState(customRule?.mdDesc ?? '');
  const [key, setKey] = useState(customRule?.key ?? '');
  const [keyModifiedByUser, setKeyModifiedByUser] = useState(false);
  const [name, setName] = useState(customRule?.name ?? '');
  const [params, setParams] = useState(getParams(customRule));
  const [reactivating, setReactivating] = useState(false);
  const [status, setStatus] = useState(customRule?.status ?? templateRule.status);
  const [ccCategory, setCCCategory] = useState<CleanCodeAttributeCategory>(
    templateRule.cleanCodeAttributeCategory ?? CleanCodeAttributeCategory.Consistent,
  );
  const [ccAttribute, setCCAttribute] = useState<CleanCodeAttribute>(
    templateRule.cleanCodeAttribute ?? CleanCodeAttribute.Conventional,
  );
  const [impacts, setImpacts] = useState<SoftwareImpact[]>(templateRule?.impacts ?? []);
  const [standardSeverity, setStandardSeverity] = useState(
    customRule?.severity ?? templateRule.severity,
  );
  const [standardType, setStandardType] = useState(customRule?.type ?? templateRule.type);
  const [cctType, setCCTType] = useState<CustomRuleType>(
    standardType === 'SECURITY_HOTSPOT' ? CustomRuleType.SECURITY_HOTSPOT : CustomRuleType.ISSUE,
  );
  const customRulesSearchParams = {
    f: 'name,severity,params',
    template_key: templateRule.key,
  };
  const { mutate: updateRule, isPending: updatingRule } = useUpdateRuleMutation(
    customRulesSearchParams,
    props.onClose,
  );
  const { mutate: createRule, isPending: creatingRule } = useCreateRuleMutation(
    customRulesSearchParams,
    props.onClose,
    (response: Response) => {
      setReactivating(response.status === HttpStatusCode.Conflict);
    },
  );
  const warningRef = useRef<HTMLDivElement>(null);

  const submitting = updatingRule || creatingRule;
  const hasError =
    !isStandardMode && impacts.length === 0 && cctType !== CustomRuleType.SECURITY_HOTSPOT;
  const isDisabledInUpdate = submitting || customRule !== undefined;

  const submit = () => {
    const isSecurityHotspot =
      standardType === 'SECURITY_HOTSPOT' || cctType === CustomRuleType.SECURITY_HOTSPOT;
    const stringifiedParams = Object.keys(params)
      .map((key) => `${key}=${csvEscape(params[key])}`)
      .join(';');

    const ruleData = {
      name,
      status,
      markdownDescription: description,
    };

    const standardRule = {
      type: standardType,
      ...(isSecurityHotspot ? {} : { severity: standardSeverity }),
    };

    const cctRule = isSecurityHotspot
      ? { type: cctType as RuleType }
      : {
          cleanCodeAttribute: ccAttribute,
          impacts,
        };

    if (customRule) {
      updateRule({
        ...ruleData,
        ...(isStandardMode ? standardRule : cctRule),
        params: stringifiedParams,
        key: customRule.key,
      });
    } else if (reactivating) {
      updateRule({
        ...ruleData,
        ...(isStandardMode ? standardRule : cctRule),
        params: stringifiedParams,
        key: `${templateRule.repo}:${key}`,
      });
    } else {
      createRule({
        ...ruleData,
        impacts: [], // impacts are required in createRule
        ...(isStandardMode ? standardRule : cctRule),
        key: `${templateRule.repo}:${key}`,
        templateKey: templateRule.key,
        parameters: Object.entries(params).map(([key, value]) => ({ key, defaultValue: value })),
      });
    }
  };

  // If key changes, then most likely user did it to create a new rule instead of reactivating one
  useEffect(() => {
    setReactivating(false);
  }, [key]);

  // scroll to warning when it appears
  useEffect(() => {
    if (reactivating) {
      warningRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [reactivating]);

  const NameField = useMemo(
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
          onChange={({ currentTarget: { value: name } }: SyntheticEvent<HTMLInputElement>) => {
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

  const KeyField = useMemo(
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
            onChange={(event: SyntheticEvent<HTMLInputElement>) => {
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

  const DescriptionField = useMemo(
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
          onChange={(event: SyntheticEvent<HTMLTextAreaElement>) =>
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

  const CCTIssueTypeField = useMemo(() => {
    const typeOptions = Object.values(CustomRuleType).map((value) => ({
      label: translate(`coding_rules.custom.type.option.${value}`),
      value,
    }));

    return (
      <FormField
        ariaLabel={translate('coding_rules.custom.type.label')}
        label={translate('coding_rules.custom.type.label')}
        htmlFor="coding-rules-custom-rule-type"
      >
        <Select
          isRequired
          id="coding-rules-custom-rule-type"
          isDisabled={isDisabledInUpdate}
          aria-labelledby="coding-rules-custom-rule-type"
          onChange={(value) => (value ? setCCTType(value as CustomRuleType) : '')}
          data={typeOptions}
          isSearchable={false}
          value={typeOptions.find((s) => s.value === cctType)?.value}
        />
      </FormField>
    );
  }, [cctType, isDisabledInUpdate]);

  const StatusField = useMemo(() => {
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
        <Select
          isRequired
          id="coding-rules-custom-rule-status"
          isDisabled={submitting}
          aria-labelledby="coding-rules-custom-rule-status"
          onChange={(value) => (value ? setStatus(value) : undefined)}
          data={statusesOptions}
          isSearchable={false}
          value={statusesOptions.find((s) => s.value === status)?.value}
        />
      </FormField>
    );
  }, [status, submitting]);

  const StandardTypeField = useMemo(() => {
    const ruleTypeOption: LabelValueSelectOption<RuleType>[] = RULE_TYPES.map((type) => ({
      label: translate('issue.type', type),
      value: type,
      prefix: <IssueTypeIcon type={type} />,
    }));
    return (
      <FormField
        ariaLabel={translate('type')}
        label={translate('type')}
        htmlFor="coding-rules-custom-rule-type"
      >
        <Select
          id="coding-rules-custom-rule-type"
          isNotClearable
          isDisabled={isDisabledInUpdate}
          isSearchable={false}
          onChange={(value) => setStandardType(value as RuleType)}
          data={ruleTypeOption}
          value={ruleTypeOption.find((t) => t.value === standardType)?.value}
          valueIcon={<IssueTypeIcon type={standardType} />}
        />
      </FormField>
    );
  }, [isDisabledInUpdate, standardType]);

  const StandardSeverityField = useMemo(
    () => (
      <FormField
        ariaLabel={translate('severity')}
        label={translate('severity')}
        htmlFor="coding-rules-severity-select"
      >
        <SeveritySelect
          id="coding-rules-severity-select"
          isDisabled={submitting}
          onChange={(value) => setStandardSeverity(value)}
          severity={standardSeverity}
          recommendedSeverity={customRule?.severity ? undefined : templateRule.severity}
        />
      </FormField>
    ),
    [customRule?.severity, standardSeverity, submitting, templateRule.severity],
  );

  const handleParameterChange = useCallback(
    (event: SyntheticEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      const { name, value } = event.currentTarget;
      setParams({ ...params, [name]: value });
    },
    [params],
  );

  const renderParameterField = useCallback(
    (param: RuleParameter) => {
      // Gets the actual value from params from the state.
      // Without it, we have an issue with string 'constructor' as key
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
            <SafeHTMLInjection
              htmlAsString={param.htmlDesc}
              sanitizeLevel={SanitizeLevel.FORBID_SVG_MATHML}
            >
              <Text isSubdued />
            </SafeHTMLInjection>
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
      size={ModalSize.Wide}
      isOpen={isOpen}
      title={header}
      onOpenChange={props.onClose}
      content={
        <form
          className="sw-flex sw-flex-col sw-justify-stretch sw-pb-4"
          id={FORM_ID}
          onSubmit={(event: SyntheticEvent<HTMLFormElement>) => {
            event.preventDefault();
            submit();
          }}
        >
          {reactivating && (
            <div ref={warningRef}>
              <FlagMessage variant="warning" className="sw-mb-6">
                {translate('coding_rules.reactivate.help')}
              </FlagMessage>
            </div>
          )}

          <MandatoryFieldsExplanation className="sw-mb-4" />

          {NameField}
          {KeyField}
          {isStandardMode && (
            <>
              {StandardTypeField}
              {standardType !== 'SECURITY_HOTSPOT' && StandardSeverityField}
            </>
          )}
          {!isStandardMode && (
            <>
              {CCTIssueTypeField}
              {cctType !== 'SECURITY_HOTSPOT' && (
                <>
                  <div className="sw-flex sw-justify-between sw-gap-6">
                    <CleanCodeCategoryField
                      value={ccCategory}
                      disabled={isDisabledInUpdate}
                      onChange={setCCCategory}
                    />
                    <CleanCodeAttributeField
                      value={ccAttribute}
                      category={ccCategory}
                      disabled={isDisabledInUpdate}
                      onChange={setCCAttribute}
                    />
                  </div>
                  <SoftwareQualitiesFields
                    error={hasError}
                    value={impacts}
                    onChange={setImpacts}
                    disabled={submitting}
                    qualityUpdateDisabled={isDisabledInUpdate}
                  />
                </>
              )}
            </>
          )}
          {StatusField}
          {DescriptionField}
          {templateParams.map(renderParameterField)}
        </form>
      }
      primaryButton={
        <Button
          variety={ButtonVariety.Primary}
          isDisabled={submitting || hasError}
          type="submit"
          form={FORM_ID}
        >
          {buttonText}
        </Button>
      }
      secondaryButton={
        <Button onClick={props.onClose} variety={ButtonVariety.Default}>
          {translate('cancel')}
        </Button>
      }
    />
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
