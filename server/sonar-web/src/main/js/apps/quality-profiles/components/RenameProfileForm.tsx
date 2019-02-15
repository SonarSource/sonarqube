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
import { Profile } from '../types';
import { renameProfile } from '../../../api/quality-profiles';
import Modal from '../../../components/controls/Modal';
import { SubmitButton, ResetButtonLink } from '../../../components/ui/buttons';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  onClose: () => void;
  onRename: (name: string) => void;
  profile: Profile;
}

interface State {
  loading: boolean;
  name: string | null;
}

export default class RenameProfileForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false, name: null };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleNameChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    const { name } = this.state;

    if (name != null) {
      this.setState({ loading: true });
      renameProfile(this.props.profile.key, name).then(
        () => this.props.onRename(name),
        () => {
          if (this.mounted) {
            this.setState({ loading: false });
          }
        }
      );
    }
  };

  render() {
    const { profile } = this.props;
    const header = translateWithParameters(
      'quality_profiles.rename_x_title',
      profile.name,
      profile.languageName
    );
    const submitDisabled =
      this.state.loading || !this.state.name || this.state.name === profile.name;

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose} size="small">
        <form id="rename-profile-form" onSubmit={this.handleFormSubmit}>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>
          <div className="modal-body">
            <div className="modal-field">
              <label htmlFor="rename-profile-name">
                {translate('quality_profiles.new_name')}
                <em className="mandatory">*</em>
              </label>
              <input
                autoFocus={true}
                id="rename-profile-name"
                maxLength={100}
                name="name"
                onChange={this.handleNameChange}
                required={true}
                size={50}
                type="text"
                value={this.state.name != null ? this.state.name : profile.name}
              />
            </div>
          </div>
          <div className="modal-foot">
            {this.state.loading && <i className="spinner spacer-right" />}
            <SubmitButton disabled={submitDisabled} id="rename-profile-submit">
              {translate('rename')}
            </SubmitButton>
            <ResetButtonLink id="rename-profile-cancel" onClick={this.props.onClose}>
              {translate('cancel')}
            </ResetButtonLink>
          </div>
        </form>
      </Modal>
    );
  }
}
