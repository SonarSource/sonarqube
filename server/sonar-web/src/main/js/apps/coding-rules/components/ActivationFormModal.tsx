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
} from 'design-system';
import * as React from 'react';
import { Profile, activateRule } from '../../../api/quality-profiles';
import DocLink from '../../../components/common/DocLink';
import { translate } from '../../../helpers/l10n';
import { sanitizeString } from '../../../helpers/sanitize';
import { IssueSeverity } from '../../../types/issues';
import { Dict, Rule, RuleActivation, RuleDetails } from '../../../types/types';
import { sortProfiles } from '../../quality-profiles/utils';
import { SeveritySelect } from './SeveritySelect';

interface Props {
  activation?: RuleActivation;
  modalHeader: string;
  onClose: () => void;
  onDone: (severity: string) => Promise<void>;
  // eslint-disable-next-line react/no-unused-prop-types
  profiles: Profile[];
  rule: Rule | RuleDetails;
}

interface ProfileWithDepth extends Profile {
  depth: number;
}

interface State {
  params: Dict<string>;
  profile?: ProfileWithDepth;
  submitting: boolean;
  severity: IssueSeverity;
}

const MIN_PROFILES_TO_ENABLE_SELECT = 2;
const FORM_ID = 'rule-activation-modal-form';

export default class ActivationFormModal extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    const profilesWithDepth = this.getQualityProfilesWithDepth(props);
    this.state = {
      params: this.getParams(props),
      profile: profilesWithDepth.length > 0 ? profilesWithDepth[0] : undefined,
      submitting: false,
      severity: (props.activation
        ? props.activation.severity
        : props.rule.severity) as IssueSeverity,
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getParams = ({ activation, rule } = this.props) => {
    const params: Dict<string> = {};
    if (rule?.params) {
      for (const param of rule.params) {
        params[param.key] = param.defaultValue || '';
      }
      if (activation?.params) {
        for (const param of activation.params) {
          params[param.key] = param.value;
        }
      }
    }
    return params;
  };

  // Choose QP which a user can administrate, which are the same language and which are not built-in
  getQualityProfilesWithDepth = ({ profiles } = this.props) => {
    return sortProfiles(
      profiles.filter(
        (profile) =>
          !profile.isBuiltIn &&
          profile.actions &&
          profile.actions.edit &&
          profile.language === this.props.rule.lang,
      ),
    ).map((profile) => ({
      ...profile,
      // Decrease depth by 1, so the top level starts at 0
      depth: profile.depth - 1,
    }));
  };

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ submitting: true });
    const data = {
      key: this.state.profile?.key ?? '',
      params: this.state.params,
      rule: this.props.rule.key,
      severity: this.state.severity,
    };
    activateRule(data)
      .then(() => this.props.onDone(data.severity))
      .then(
        () => {
          if (this.mounted) {
            this.setState({ submitting: false });
            this.props.onClose();
          }
        },
        () => {
          if (this.mounted) {
            this.setState({ submitting: false });
          }
        },
      );
  };

  handleParameterChange = (event: React.SyntheticEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = event.currentTarget;
    this.setState((state: State) => ({ params: { ...state.params, [name]: value } }));
  };

  handleProfileChange = (value: LabelValueSelectOption<ProfileWithDepth>) => {
    this.setState({ profile: value.value });
  };

  handleSeverityChange = ({ value }: LabelValueSelectOption<IssueSeverity>) => {
    this.setState({ severity: value });
  };

  render() {
    const { activation, rule } = this.props;
    const { profile, severity, submitting } = this.state;
    const { params = [] } = rule;
    const profilesWithDepth = this.getQualityProfilesWithDepth();
    const profileOptions = profilesWithDepth.map((p) => ({ label: p.name, value: p }));
    const isCustomRule = !!(rule as RuleDetails).templateKey;
    const activeInAllProfiles = profilesWithDepth.length <= 0;
    const isUpdateMode = !!activation;

    return (
      <Modal
        headerTitle={this.props.modalHeader}
        onClose={this.props.onClose}
        loading={submitting}
        isOverflowVisible
        primaryButton={
          <ButtonPrimary disabled={submitting || activeInAllProfiles} form={FORM_ID} type="submit">
            {isUpdateMode ? translate('save') : translate('coding_rules.activate')}
          </ButtonPrimary>
        }
        secondaryButtonLabel={translate('cancel')}
        body={
          <form id={FORM_ID} onSubmit={this.handleFormSubmit}>
            {!isUpdateMode && activeInAllProfiles && (
              <FlagMessage className="sw-mb-2" variant="info">
                {translate('coding_rules.active_in_all_profiles')}
              </FlagMessage>
            )}

            <FlagMessage className="sw-mb-4" variant="info">
              {translate('coding_rules.severity_deprecated')}
              <DocLink className="sw-ml-2 sw-whitespace-nowrap" to="/user-guide/clean-code/">
                {translate('learn_more')}
              </DocLink>
            </FlagMessage>

            <FormField
              ariaLabel={translate('coding_rules.quality_profile')}
              label={translate('coding_rules.quality_profile')}
              htmlFor="coding-rules-quality-profile-select-input"
            >
              <InputSelect
                id="coding-rules-quality-profile-select"
                inputId="coding-rules-quality-profile-select-input"
                isClearable={false}
                isDisabled={submitting || profilesWithDepth.length < MIN_PROFILES_TO_ENABLE_SELECT}
                onChange={this.handleProfileChange}
                getOptionLabel={({ value }: LabelValueSelectOption<ProfileWithDepth>) =>
                  '   '.repeat(value.depth) + value.name
                }
                options={profileOptions}
                value={profileOptions.find(({ value }) => value.key === profile?.key)}
              />
            </FormField>

            <FormField
              ariaLabel={translate('severity')}
              label={translate('severity')}
              htmlFor="coding-rules-severity-select"
            >
              <SeveritySelect
                isDisabled={submitting}
                onChange={this.handleSeverityChange}
                severity={severity}
              />
            </FormField>

            {isCustomRule ? (
              <Note as="p" className="sw-my-4">
                {translate('coding_rules.custom_rule.activation_notice')}
              </Note>
            ) : (
              params.map((param) => (
                <FormField label={param.key} key={param.key} htmlFor={param.key}>
                  {param.type === 'TEXT' ? (
                    <InputTextArea
                      id={param.key}
                      disabled={submitting}
                      name={param.key}
                      onChange={this.handleParameterChange}
                      placeholder={param.defaultValue}
                      rows={3}
                      size="full"
                      value={this.state.params[param.key] ?? ''}
                    />
                  ) : (
                    <InputField
                      id={param.key}
                      disabled={submitting}
                      name={param.key}
                      onChange={this.handleParameterChange}
                      placeholder={param.defaultValue}
                      size="full"
                      type="text"
                      value={this.state.params[param.key] ?? ''}
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
}
