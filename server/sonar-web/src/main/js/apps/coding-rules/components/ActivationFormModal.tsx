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

import { Button, ButtonVariety, Modal, Select } from '@sonarsource/echoes-react';
import {
  FlagMessage,
  FormField,
  InputField,
  InputSelect,
  InputTextArea,
  LabelValueSelectOption,
  Note,
  Switch,
} from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { Profile } from '../../../api/quality-profiles';
import { useAvailableFeatures } from '../../../app/components/available-features/withAvailableFeatures';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { DocLink } from '../../../helpers/doc-links';
import { sanitizeString } from '../../../helpers/sanitize';
import { useActivateRuleMutation } from '../../../queries/quality-profiles';
import { Feature } from '../../../types/features';
import { IssueSeverity } from '../../../types/issues';
import { Dict, Rule, RuleActivation, RuleDetails } from '../../../types/types';
import { sortProfiles } from '../../quality-profiles/utils';
import { SeveritySelect } from './SeveritySelect';

interface Props {
  organization: string | undefined;
  activation?: RuleActivation;
  isOpen: boolean;
  modalHeader: string;
  onClose: () => void;
  onDone?: (severity: string, prioritizedRule: boolean) => Promise<void> | void;
  onOpenChange: (isOpen: boolean) => void;
  profiles: Profile[];
  rule: Rule | RuleDetails;
}

interface ProfileWithDepth extends Profile {
  depth: number;
}

const MIN_PROFILES_TO_ENABLE_SELECT = 2;
const FORM_ID = 'rule-activation-modal-form';
const PARAMS_DELIMITER = ";";
const KEY_VALUE_DELIMITER = "=";

export default function ActivationFormModal(props: Readonly<Props>) {
  const { activation, rule, profiles, modalHeader, isOpen, onOpenChange } = props;
  const { mutate: activateRule, isPending: submitting } = useActivateRuleMutation((data) => {
    props.onDone?.(data.severity as string, data.prioritizedRule as boolean);
    props.onClose();
  });
  const { hasFeature } = useAvailableFeatures();
  const intl = useIntl();
  const [changedPrioritizedRule, setChangedPrioritizedRule] = React.useState<boolean | undefined>(
    undefined,
  );
  const [changedProfile, setChangedProfile] = React.useState<ProfileWithDepth | undefined>(
    undefined,
  );
  const [changedParams, setChangedParams] = React.useState<Record<string, string> | undefined>(
    undefined,
  );
  const [changedSeverity, setChangedSeverity] = React.useState<IssueSeverity | undefined>(
    undefined,
  );

  const profilesWithDepth = React.useMemo(() => {
    return getQualityProfilesWithDepth(profiles, rule.lang);
  }, [profiles, rule.lang]);

  const prioritizedRule =
    changedPrioritizedRule ?? (activation ? activation.prioritizedRule : false);
  const profile = changedProfile ?? profilesWithDepth[0];
  const params = changedParams ?? getRuleParams({ activation, rule });
  const allParams = getAllParams({ rule });
  const severity =
    changedSeverity ?? ((activation ? activation.severity : rule.severity) as IssueSeverity);
  const profileOptions = profilesWithDepth.map((p) => ({ label: p.name, value: p }));
  const isCustomRule = !!(rule as RuleDetails).templateKey;
  const activeInAllProfiles = profilesWithDepth.length <= 0;
  const isUpdateMode = !!activation;

  React.useEffect(() => {
    if (!isOpen) {
      setChangedPrioritizedRule(undefined);
      setChangedProfile(undefined);
      setChangedParams(undefined);
      setChangedSeverity(undefined);
    }
  }, [isOpen]);

  const handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    const data = {
      organization: props.organization,
      key: profile?.key ?? '',
      params: getParamsFromMap(params),
      rule: rule.key,
      severity,
      prioritizedRule,
    };
    activateRule(data);
  };

  const handleParameterChange = (
    event: React.SyntheticEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    const { name, value } = event.currentTarget;
    setChangedParams({ ...params, [name]: value });
  };

  const handleSingleSelectListChange = ({ value, labelKey }: { label: string, value: string, labelKey: string }) => {
    params[labelKey] = value;
    setChangedParams({ ...params });
  };

  const handleKeyChange = (index: any, value: any, paramKey: any) => {
    params[paramKey][index][0] = value;
    if (index === params[paramKey].length - 1) {
      params[paramKey].splice(index + 1, 0, ['', '']);
    }
    setChangedParams({ ...params });
  };

  const handleValueChange = (index: any, value: any, paramKey: any) => {
    params[paramKey][index][1] = value;
    if (index === params[paramKey].length - 1) {
      params[paramKey].splice(index + 1, 0, ['', '']);
    }
    setChangedParams({ ...params });
  };

  const handleDeleteValue = (index: number, paramKey: any) => {
    params[paramKey].splice(index, 1);
    setChangedParams({ ...params });
  };

  function getParamsFromMap(ruleParams) {
    const { params = [] } = props.rule;
    let saveParams = {...ruleParams}
    params.map(param => {
      if (param.type == 'KEY_VALUE_MAP') {
        saveParams[param.key].pop();
        let res = '';
        let row = saveParams[param.key].length;
        saveParams[param.key].map((param: string[], index: number) => {
          res = res + param[0] + KEY_VALUE_DELIMITER + param[1];
          if (index + 1 != row) {
            res = res + PARAMS_DELIMITER;
          }
        })
        saveParams[param.key] = res;
      }
    })

    return saveParams;
  }

  return (
    <Modal
      title={modalHeader}
      isOpen={isOpen}
      onOpenChange={onOpenChange}
      primaryButton={
        <Button
          variety={ButtonVariety.Primary}
          isDisabled={submitting || activeInAllProfiles}
          isLoading={submitting}
          form={FORM_ID}
          type="submit"
        >
          {isUpdateMode
            ? intl.formatMessage({ id: 'save' })
            : intl.formatMessage({ id: 'coding_rules.activate' })}
        </Button>
      }
      secondaryButton={
        <Button variety={ButtonVariety.Default} isDisabled={submitting} onClick={props.onClose}>
          {intl.formatMessage({ id: 'cancel' })}
        </Button>
      }
      content={
        <form className="sw-pb-10" id={FORM_ID} onSubmit={handleFormSubmit}>
          {!isUpdateMode && activeInAllProfiles && (
            <FlagMessage className="sw-mb-2" variant="info">
              {intl.formatMessage({ id: 'coding_rules.active_in_all_profiles' })}
            </FlagMessage>
          )}

          <FormField
            ariaLabel={intl.formatMessage({ id: 'coding_rules.quality_profile' })}
            label={intl.formatMessage({ id: 'coding_rules.quality_profile' })}
            htmlFor="coding-rules-quality-profile-select-input"
          >
            <InputSelect
              id="coding-rules-quality-profile-select"
              inputId="coding-rules-quality-profile-select-input"
              isClearable={false}
              isDisabled={submitting || profilesWithDepth.length < MIN_PROFILES_TO_ENABLE_SELECT}
              onChange={({ value }: LabelValueSelectOption<ProfileWithDepth>) => {
                setChangedProfile(value);
              }}
              getOptionLabel={({ value }: LabelValueSelectOption<ProfileWithDepth>) =>
                '   '.repeat(value.depth) + value.name
              }
              options={profileOptions}
              value={profileOptions.find(({ value }) => value.key === profile?.key)}
            />
          </FormField>

          {hasFeature(Feature.PrioritizedRules) && (
            <FormField
              ariaLabel={intl.formatMessage({ id: 'coding_rules.prioritized_rule.title' })}
              label={intl.formatMessage({ id: 'coding_rules.prioritized_rule.title' })}
              description={
                <div className="sw-text-xs">
                  {intl.formatMessage({ id: 'coding_rules.prioritized_rule.note' })}
                  <DocumentationLink
                    className="sw-ml-2 sw-whitespace-nowrap"
                    to={DocLink.InstanceAdminQualityProfilesPrioritizingRules}
                  >
                    {intl.formatMessage({ id: 'learn_more' })}
                  </DocumentationLink>
                </div>
              }
            >
              <label
                id="coding-rules-prioritized-rule"
                className="sw-flex sw-items-center sw-gap-2"
              >
                <Switch
                  onChange={setChangedPrioritizedRule}
                  name={intl.formatMessage({ id: 'coding_rules.prioritized_rule.title' })}
                  value={prioritizedRule}
                />
                <span className="sw-text-xs">
                  {intl.formatMessage({ id: 'coding_rules.prioritized_rule.switch_label' })}
                </span>
              </label>
            </FormField>
          )}

          <FormField
            ariaLabel={intl.formatMessage({ id: 'severity_deprecated' })}
            label={intl.formatMessage({ id: 'severity_deprecated' })}
            htmlFor="coding-rules-severity-select"
          >
            <SeveritySelect
              isDisabled={submitting}
              onChange={({ value }: LabelValueSelectOption<IssueSeverity>) => {
                setChangedSeverity(value);
              }}
              severity={severity}
            />
            <FlagMessage className="sw-mb-4 sw-mt-2" variant="info">
              <div>
                {intl.formatMessage({ id: 'coding_rules.severity_deprecated' })}
                <DocumentationLink
                  className="sw-ml-2 sw-whitespace-nowrap"
                  to={DocLink.CleanCodeIntroduction}
                >
                  {intl.formatMessage({ id: 'learn_more' })}
                </DocumentationLink>
              </div>
            </FlagMessage>
          </FormField>

          {isCustomRule ? (
            <Note as="p" className="sw-my-4">
              {intl.formatMessage({ id: 'coding_rules.custom_rule.activation_notice' })}
            </Note>
          ) : (
            rule.params?.map((param) => (
              <FormField label={param.key} key={param.key} htmlFor={param.key}>
                {param.type === 'TEXT' && (
                  <InputTextArea
                    id={param.key}
                    disabled={submitting}
                    name={param.key}
                    onChange={handleParameterChange}
                    placeholder={param.defaultValue}
                    rows={3}
                    size="full"
                    value={params[param.key] ?? ''}
                  />
                )}
                {param.type.startsWith('SINGLE_SELECT_LIST') && (
                  <Select
                    clearable={false}
                    onChange={handleSingleSelectListChange}
                    options={allParams[param.key].map((item: string) => ({
                      labelKey: param.key,
                      label: item,
                      value: item
                    }))}
                    value={{labelKey: param.key, label: params[param.key], value: params[param.key]}}
                  />
                )}
                {param.type === 'KEY_VALUE_MAP' && (
                  <ul>
                    {params[param.key].map((value: any, index: number) =>
                      <li className="spacer-bottom display-flex-row " key={index}>
                        <input
                          disabled={submitting}
                          className="coding-rule-map-input"
                          onChange={e => handleKeyChange(index, e.target.value, param.key)}
                          name={"key" + index}
                          type="text"
                          value={value[0]} />

                        <input
                          disabled={submitting}
                          className="coding-rule-map-input"
                          onChange={e => handleValueChange(index, e.target.value, param.key)}
                          name={"value" + index}
                          type="text"
                          value={value[1]} />

                        {!(index === params[param.key].length - 1) && (
                          <div className="display-inline-block spacer-left">
                            <Button
                              variety={ButtonVariety.Danger}
                              isDisabled={submitting}
                              onClick={() => handleDeleteValue(index, param.key)}
                            />
                          </div>
                        )}
                      </li>
                    )}
                  </ul>
                )}
                {param.type !== 'TEXT' && param.type !== 'KEY_VALUE_MAP' && !param.type.startsWith('SINGLE_SELECT_LIST') && (
                  <InputField
                    id={param.key}
                    disabled={submitting}
                    name={param.key}
                    onChange={handleParameterChange}
                    placeholder={param.defaultValue}
                    size="full"
                    type="text"
                    value={params[param.key] ?? ''}
                  />
                )}
                {param.htmlDesc !== undefined && (
                  <Note
                    as="div"
                    // eslint-disable-next-line react/no-danger
                    dangerouslySetInnerHTML={{ __html: sanitizeString(param.htmlDesc) }}
                  />
                )}
              </FormField>
            ))
          )}
        </form>
      }
    />
  );
}

function getQualityProfilesWithDepth(
  profiles: Profile[] = [],
  ruleLang?: string,
): ProfileWithDepth[] {
  return sortProfiles(
    profiles.filter(
      (profile) =>
        !profile.isBuiltIn &&
        profile.actions &&
        profile.actions.edit &&
        profile.language === ruleLang,
    ),
  ).map((profile) => ({
    ...profile,
    // Decrease depth by 1, so the top level starts at 0
    depth: profile.depth - 1,
  }));
}

function getRuleParams({
  activation,
  rule,
}: {
  activation?: RuleActivation;
  rule: RuleDetails | Rule;
}) {
  const params: Dict<any> = {};
  if (rule?.params) {
    for (const param of rule.params) {
      if (param.type == "KEY_VALUE_MAP") {
        let paramsArray: String[] = [];
        if (param.defaultValue) {
          paramsArray = param.defaultValue.split(PARAMS_DELIMITER);
        }
        paramsArray.push(KEY_VALUE_DELIMITER);
        params[param.key] = paramsArray.map((object: any) => object.split(KEY_VALUE_DELIMITER));
      } else {
        params[param.key] = param.defaultValue ?? '';
      }
    }
    if (activation?.params) {
      for (const param of activation.params) {
        if (typeof (params[param.key]) !== 'string') {
          let paramsArray: String[] = [];
          paramsArray = param.value.split(PARAMS_DELIMITER);
          paramsArray.push(KEY_VALUE_DELIMITER);
          params[param.key] = paramsArray.map((object: any) => object.split(KEY_VALUE_DELIMITER));
        } else {
          params[param.key] = param.value;
        }
      }
    }
  }
  return params;
}

// Unlike other param types, SINGLE_SELECT_LIST has predefined list of values from which user can choose from.
function getAllParams({ rule }) {
  const params: Dict<any> = {};
  if (rule && rule.params) {
    for (const param of rule.params) {
      if (param.type.startsWith("SINGLE_SELECT_LIST")) {
        let list: String;
        list = param.type.substring(param.type.indexOf('\"') + 1);
        list = list.substring(0, list.indexOf(',\"'));
        params[param.key] = list.split(',');
      }
    }
  }
  return params;
}
