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
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { formatMeasure } from 'sonar-ui-common/helpers/measures';
import { bulkActivateRules, bulkDeactivateRules, Profile } from '../../../api/quality-profiles';
import { Query, serializeQuery } from '../query';

interface Props {
  action: string;
  languages: T.Languages;
  onClose: () => void;
  organization: string | undefined;
  profile?: Profile;
  query: Query;
  referencedProfiles: T.Dict<Profile>;
  total: number;
}

interface ActivationResult {
  failed: number;
  profile: string;
  succeeded: number;
}

interface State {
  finished: boolean;
  results: ActivationResult[];
  selectedProfiles: any[];
  submitting: boolean;
}

export default class BulkChangeModal extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    // if there is only one possible option for profile, select it immediately
    const selectedProfiles = [];
    const availableProfiles = this.getAvailableQualityProfiles(props);
    if (availableProfiles.length === 1) {
      selectedProfiles.push(availableProfiles[0].key);
    }

    this.state = { finished: false, results: [], selectedProfiles, submitting: false };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleProfileSelect = (options: { value: string }[]) => {
    const selectedProfiles = options.map(option => option.value);
    this.setState({ selectedProfiles });
  };

  getAvailableQualityProfiles = ({ query, referencedProfiles } = this.props) => {
    let profiles = Object.values(referencedProfiles);
    if (query.languages.length > 0) {
      profiles = profiles.filter(profile => query.languages.includes(profile.language));
    }
    return profiles
      .filter(profile => profile.actions && profile.actions.edit)
      .filter(profile => !profile.isBuiltIn);
  };

  processResponse = (profile: string, response: any) => {
    if (this.mounted) {
      const result: ActivationResult = {
        failed: response.failed || 0,
        profile,
        succeeded: response.succeeded || 0
      };
      this.setState(state => ({ results: [...state.results, result] }));
    }
  };

  sendRequests = () => {
    let looper = Promise.resolve();

    // serialize the query, but delete the `profile`
    const data = serializeQuery(this.props.query);
    delete data.profile;

    const method = this.props.action === 'activate' ? bulkActivateRules : bulkDeactivateRules;

    // if a profile is selected in the facet, pick it
    // otherwise take all profiles selected in the dropdown
    const profiles: string[] = this.props.profile
      ? [this.props.profile.key]
      : this.state.selectedProfiles;

    for (const profile of profiles) {
      looper = looper.then(() =>
        method({ ...data, organization: this.props.organization, targetKey: profile }).then(
          response => this.processResponse(profile, response)
        )
      );
    }
    return looper;
  };

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ submitting: true });
    this.sendRequests().then(
      () => {
        if (this.mounted) {
          this.setState({ finished: true, submitting: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ submitting: false });
        }
      }
    );
  };

  renderResult = (result: ActivationResult) => {
    const { profile: profileKey } = result;
    const profile = this.props.referencedProfiles[profileKey];
    if (!profile) {
      return null;
    }
    const { languages } = this.props;
    const language = languages[profile.language]
      ? languages[profile.language].name
      : profile.language;
    return (
      <Alert key={result.profile} variant={result.failed === 0 ? 'success' : 'warning'}>
        {result.failed
          ? translateWithParameters(
              'coding_rules.bulk_change.warning',
              profile.name,
              language,
              result.succeeded,
              result.failed
            )
          : translateWithParameters(
              'coding_rules.bulk_change.success',
              profile.name,
              language,
              result.succeeded
            )}
      </Alert>
    );
  };

  renderProfileSelect = () => {
    const profiles = this.getAvailableQualityProfiles();
    const options = profiles.map(profile => ({
      label: `${profile.name} - ${profile.languageName}`,
      value: profile.key
    }));
    return (
      <Select
        multi={true}
        onChange={this.handleProfileSelect}
        options={options}
        value={this.state.selectedProfiles}
      />
    );
  };

  render() {
    const { action, profile, total } = this.props;
    const header =
      // prettier-ignore
      action === 'activate'
        ? `${translate('coding_rules.activate_in_quality_profile')} (${formatMeasure(total, 'INT')} ${translate('coding_rules._rules')})`
        : `${translate('coding_rules.deactivate_in_quality_profile')} (${formatMeasure(total, 'INT')} ${translate('coding_rules._rules')})`;

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose} size="small">
        <form onSubmit={this.handleFormSubmit}>
          <header className="modal-head">
            <h2>{header}</h2>
          </header>

          <div className="modal-body">
            {this.state.results.map(this.renderResult)}

            {!this.state.finished && !this.state.submitting && (
              <div className="modal-field">
                <h3>
                  <label htmlFor="coding-rules-bulk-change-profile">
                    {action === 'activate'
                      ? translate('coding_rules.activate_in')
                      : translate('coding_rules.deactivate_in')}
                  </label>
                </h3>
                {profile ? (
                  <span>
                    {profile.name}
                    {' — '}
                    {translate('are_you_sure')}
                  </span>
                ) : (
                  this.renderProfileSelect()
                )}
              </div>
            )}
          </div>

          <footer className="modal-foot">
            {this.state.submitting && <i className="spinner spacer-right" />}
            {!this.state.finished && (
              <SubmitButton disabled={this.state.submitting} id="coding-rules-submit-bulk-change">
                {translate('apply')}
              </SubmitButton>
            )}
            <ResetButtonLink onClick={this.props.onClose}>
              {this.state.finished ? translate('close') : translate('cancel')}
            </ResetButtonLink>
          </footer>
        </form>
      </Modal>
    );
  }
}
