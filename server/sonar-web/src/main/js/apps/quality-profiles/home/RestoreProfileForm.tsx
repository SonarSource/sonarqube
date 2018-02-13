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
import { restoreQualityProfile } from '../../../api/quality-profiles';
import Modal from '../../../components/controls/Modal';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  onClose: () => void;
  onRequestFail: (reason: any) => void;
  onRestore: () => void;
  organization: string | null;
}

interface State {
  loading: boolean;
  profile?: { name: string };
  ruleFailures?: number;
  ruleSuccesses?: number;
}

export default class RestoreProfileForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleCancelClick = (event: React.SyntheticEvent<HTMLElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    this.setState({ loading: true });

    const data = new FormData(event.currentTarget);
    if (this.props.organization) {
      data.append('organization', this.props.organization);
    }

    restoreQualityProfile(data).then(
      (response: any) => {
        if (this.mounted) {
          this.setState({
            loading: false,
            profile: response.profile,
            ruleFailures: response.ruleFailures,
            ruleSuccesses: response.ruleSuccesses
          });
        }
        this.props.onRestore();
      },
      (error: any) => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
        this.props.onRequestFail(error);
      }
    );
  };

  render() {
    const header = translate('quality_profiles.restore_profile');

    const { loading, profile, ruleFailures, ruleSuccesses } = this.state;

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <form id="restore-profile-form" onSubmit={this.handleFormSubmit}>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>

          <div className="modal-body">
            {profile != null && ruleSuccesses != null ? (
              ruleFailures ? (
                <div className="alert alert-warning">
                  {translateWithParameters(
                    'quality_profiles.restore_profile.warning',
                    profile.name,
                    ruleSuccesses,
                    ruleFailures
                  )}
                </div>
              ) : (
                <div className="alert alert-success">
                  {translateWithParameters(
                    'quality_profiles.restore_profile.success',
                    profile.name,
                    ruleSuccesses
                  )}
                </div>
              )
            ) : (
              <div className="modal-field">
                <label htmlFor="restore-profile-backup">
                  {translate('backup')}
                  <em className="mandatory">*</em>
                </label>
                <input id="restore-profile-backup" name="backup" required={true} type="file" />
              </div>
            )}
          </div>

          {ruleSuccesses == null ? (
            <div className="modal-foot">
              {loading && <i className="spinner spacer-right" />}
              <button disabled={loading} id="restore-profile-submit">
                {translate('restore')}
              </button>
              <a href="#" id="restore-profile-cancel" onClick={this.handleCancelClick}>
                {translate('cancel')}
              </a>
            </div>
          ) : (
            <div className="modal-foot">
              <a href="#" onClick={this.handleCancelClick}>
                {translate('close')}
              </a>
            </div>
          )}
        </form>
      </Modal>
    );
  }
}
