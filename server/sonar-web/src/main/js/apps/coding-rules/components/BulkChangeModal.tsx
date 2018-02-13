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
import * as classNames from 'classnames';
import { Query, serializeQuery } from '../query';
import { Profile, bulkActivateRules, bulkDeactivateRules } from '../../../api/quality-profiles';
import Modal from '../../../components/controls/Modal';
import Select from '../../../components/controls/Select';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';

interface Props {
  action: string;
  onClose: () => void;
  organization: string | undefined;
  referencedProfiles: { [profile: string]: Profile };
  profile?: Profile;
  query: Query;
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

  handleCloseClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onClose();
  };

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
    return (
      <div
        className={classNames('alert', {
          'alert-warning': result.failed > 0,
          'alert-success': result.failed === 0
        })}
        key={result.profile}>
        {result.failed
          ? translateWithParameters(
              'coding_rules.bulk_change.warning',
              profile.name,
              profile.language,
              result.succeeded,
              result.failed
            )
          : translateWithParameters(
              'coding_rules.bulk_change.success',
              profile.name,
              profile.language,
              result.succeeded
            )}
      </div>
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
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <form onSubmit={this.handleFormSubmit}>
          <header className="modal-head">
            <h2>{header}</h2>
          </header>

          <div className="modal-body">
            {this.state.results.map(this.renderResult)}

            {!this.state.finished &&
              !this.state.submitting && (
                <div className="modal-field">
                  <h3>
                    <label htmlFor="coding-rules-bulk-change-profile">
                      {action === 'activate'
                        ? translate('coding_rules.activate_in')
                        : translate('coding_rules.deactivate_in')}
                    </label>
                  </h3>
                  {profile ? (
                    <h3 className="readonly-field">
                      {profile.name}
                      {' — '}
                      {translate('are_you_sure')}
                    </h3>
                  ) : (
                    this.renderProfileSelect()
                  )}
                </div>
              )}
          </div>

          <footer className="modal-foot">
            {this.state.submitting && <i className="spinner spacer-right" />}
            {!this.state.finished && (
              <button
                disabled={this.state.submitting}
                id="coding-rules-submit-bulk-change"
                type="submit">
                {translate('apply')}
              </button>
            )}
            <button className="button-link" onClick={this.handleCloseClick} type="reset">
              {this.state.finished ? translate('close') : translate('cancel')}
            </button>
          </footer>
        </form>
      </Modal>
    );
  }
}
