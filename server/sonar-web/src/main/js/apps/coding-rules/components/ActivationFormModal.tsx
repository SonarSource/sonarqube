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
import * as classNames from 'classnames';
import * as React from 'react';
import { ResetButtonLink, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import Modal from 'sonar-ui-common/components/controls/Modal';
import Select from 'sonar-ui-common/components/controls/Select';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { activateRule, Profile } from '../../../api/quality-profiles';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import { SEVERITIES } from '../../../helpers/constants';
import { sortProfiles } from '../../quality-profiles/utils';

interface Props {
  activation?: T.RuleActivation;
  modalHeader: string;
  onClose: () => void;
  onDone: (severity: string) => Promise<void>;
  organization: string | undefined;
  profiles: Profile[];
  rule: T.Rule | T.RuleDetails;
}

interface State {
  params: T.Dict<string>;
  profile: string;
  severity: string;
  submitting: boolean;
}

export default class ActivationFormModal extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    const profilesWithDepth = this.getQualityProfilesWithDepth(props);
    this.state = {
      params: this.getParams(props),
      profile: profilesWithDepth.length > 0 ? profilesWithDepth[0].key : '',
      severity: props.activation ? props.activation.severity : props.rule.severity,
      submitting: false
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getParams = ({ activation, rule } = this.props) => {
    const params: T.Dict<string> = {};
    if (rule && rule.params) {
      for (const param of rule.params) {
        params[param.key] = param.defaultValue || '';
      }
      if (activation && activation.params) {
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
        profile =>
          !profile.isBuiltIn &&
          profile.actions &&
          profile.actions.edit &&
          profile.language === this.props.rule.lang
      )
    ).map(profile => ({
      ...profile,
      // Decrease depth by 1, so the top level starts at 0
      depth: profile.depth - 1
    }));
  };

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ submitting: true });
    const data = {
      key: this.state.profile,
      organization: this.props.organization,
      params: this.state.params,
      rule: this.props.rule.key,
      severity: this.state.severity
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
        }
      );
  };

  handleParameterChange = (event: React.SyntheticEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = event.currentTarget;
    this.setState((state: State) => ({ params: { ...state.params, [name]: value } }));
  };

  handleProfileChange = ({ value }: { value: string }) => {
    this.setState({ profile: value });
  };

  handleSeverityChange = ({ value }: { value: string }) => {
    this.setState({ severity: value });
  };

  renderSeverityOption = ({ value }: { value: string }) => {
    return <SeverityHelper severity={value} />;
  };

  render() {
    const { activation, rule } = this.props;
    const { profile, severity, submitting } = this.state;
    const { params = [] } = rule;
    const profilesWithDepth = this.getQualityProfilesWithDepth();
    const isCustomRule = !!(rule as T.RuleDetails).templateKey;
    const activeInAllProfiles = profilesWithDepth.length <= 0;
    const isUpdateMode = !!activation;

    return (
      <Modal contentLabel={this.props.modalHeader} onRequestClose={this.props.onClose} size="small">
        <form onSubmit={this.handleFormSubmit}>
          <div className="modal-head">
            <h2>{this.props.modalHeader}</h2>
          </div>

          <div className={classNames('modal-body', { 'modal-container': params.length > 0 })}>
            {!isUpdateMode && activeInAllProfiles && (
              <Alert variant="info">{translate('coding_rules.active_in_all_profiles')}</Alert>
            )}

            <div className="modal-field">
              <label>{translate('coding_rules.quality_profile')}</label>
              <Select
                className="js-profile"
                clearable={false}
                disabled={submitting || profilesWithDepth.length === 1}
                onChange={this.handleProfileChange}
                options={profilesWithDepth.map(profile => ({
                  label: '   '.repeat(profile.depth) + profile.name,
                  value: profile.key
                }))}
                value={profile}
              />
            </div>
            <div className="modal-field">
              <label>{translate('severity')}</label>
              <Select
                className="js-severity"
                clearable={false}
                disabled={submitting}
                onChange={this.handleSeverityChange}
                optionRenderer={this.renderSeverityOption}
                options={SEVERITIES.map(severity => ({
                  label: translate('severity', severity),
                  value: severity
                }))}
                searchable={false}
                value={severity}
                valueRenderer={this.renderSeverityOption}
              />
            </div>
            {isCustomRule ? (
              <div className="modal-field">
                <p className="note">{translate('coding_rules.custom_rule.activation_notice')}</p>
              </div>
            ) : (
              params.map(param => (
                <div className="modal-field" key={param.key}>
                  <label title={param.key}>{param.key}</label>
                  {param.type === 'TEXT' ? (
                    <textarea
                      disabled={submitting}
                      name={param.key}
                      onChange={this.handleParameterChange}
                      placeholder={param.defaultValue}
                      rows={3}
                      value={this.state.params[param.key] || ''}
                    />
                  ) : (
                    <input
                      disabled={submitting}
                      name={param.key}
                      onChange={this.handleParameterChange}
                      placeholder={param.defaultValue}
                      type="text"
                      value={this.state.params[param.key] || ''}
                    />
                  )}
                  <div
                    className="note"
                    // Safe: defined by rule creator (instance admin?)
                    dangerouslySetInnerHTML={{ __html: param.htmlDesc || '' }}
                  />
                </div>
              ))
            )}
          </div>

          <footer className="modal-foot">
            {submitting && <i className="spinner spacer-right" />}
            <SubmitButton disabled={submitting || activeInAllProfiles}>
              {isUpdateMode ? translate('save') : translate('coding_rules.activate')}
            </SubmitButton>
            <ResetButtonLink disabled={submitting} onClick={this.props.onClose}>
              {translate('cancel')}
            </ResetButtonLink>
          </footer>
        </form>
      </Modal>
    );
  }
}
