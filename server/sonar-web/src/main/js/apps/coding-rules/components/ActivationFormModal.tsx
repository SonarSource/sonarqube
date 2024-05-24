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

import {
  ButtonPrimary,
  FlagMessage,
  FormField,
  InputField,
  InputSelect,
  InputTextArea,
  LabelValueSelectOption,
  Modal,
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
  activation?: RuleActivation;
  modalHeader: string;
  onClose: () => void;
  onDone?: (severity: string, prioritized: boolean) => Promise<void> | void;
  profiles: Profile[];
  rule: Rule | RuleDetails;
}

interface ProfileWithDepth extends Profile {
  depth: number;
}

const MIN_PROFILES_TO_ENABLE_SELECT = 2;
const FORM_ID = 'rule-activation-modal-form';

export default function ActivationFormModal(props: Readonly<Props>) {
  const { activation, rule, profiles, modalHeader } = props;
  const { mutate: activateRule, isPending: submitting } = useActivateRuleMutation((data) => {
    props.onDone?.(data.severity as string, data.prioritized as boolean);
    props.onClose();
  });
  const { hasFeature } = useAvailableFeatures();
  const intl = useIntl();
  const [prioritized, setIsPrioritized] = React.useState(
    activation ? activation.prioritized : false,
  );

  const profilesWithDepth = getQualityProfilesWithDepth(profiles, rule.lang);
  const [profile, setProfile] = React.useState(profilesWithDepth[0]);
  const [params, setParams] = React.useState(getRuleParams({ activation, rule }));
  const [severity, setSeverity] = React.useState(
    (activation ? activation.severity : rule.severity) as IssueSeverity,
  );

  const profileOptions = profilesWithDepth.map((p) => ({ label: p.name, value: p }));
  const isCustomRule = !!(rule as RuleDetails).templateKey;
  const activeInAllProfiles = profilesWithDepth.length <= 0;
  const isUpdateMode = !!activation;

  const handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    const data = {
      key: profile?.key ?? '',
      params,
      rule: rule.key,
      severity,
      prioritized,
    };
    activateRule(data);
  };

  const handleParameterChange = (
    event: React.SyntheticEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    const { name, value } = event.currentTarget;
    setParams({ ...params, [name]: value });
  };

  const makeScrollable = (rule.params?.length ?? 0) > 1;

  return (
    <Modal
      headerTitle={modalHeader}
      onClose={props.onClose}
      loading={submitting}
      isOverflowVisible={!makeScrollable}
      isScrollable={makeScrollable}
      primaryButton={
        <ButtonPrimary disabled={submitting || activeInAllProfiles} form={FORM_ID} type="submit">
          {isUpdateMode
            ? intl.formatMessage({ id: 'save' })
            : intl.formatMessage({ id: 'coding_rules.activate' })}
        </ButtonPrimary>
      }
      secondaryButtonLabel={intl.formatMessage({ id: 'cancel' })}
      body={
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
                setProfile(value);
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
                    to="/instance-administration/quality-profiles/#prioritizing-rules"
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
                  labels={{
                    on: intl.formatMessage(
                      {
                        id: 'coding_rules.prioritized_rule.label',
                      },
                      { state: 'on' },
                    ),
                    off: intl.formatMessage(
                      {
                        id: 'coding_rules.prioritized_rule.label',
                      },
                      { state: 'off' },
                    ),
                  }}
                  onChange={setIsPrioritized}
                  value={prioritized}
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
                setSeverity(value);
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
                {param.type === 'TEXT' ? (
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
                ) : (
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
  const params: Dict<string> = {};
  if (rule?.params) {
    for (const param of rule.params) {
      params[param.key] = param.defaultValue ?? '';
    }
    if (activation?.params) {
      for (const param of activation.params) {
        params[param.key] = param.value;
      }
    }
  }
  return params;
}
