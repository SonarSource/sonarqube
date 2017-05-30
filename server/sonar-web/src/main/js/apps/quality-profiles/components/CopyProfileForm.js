/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import Modal from 'react-modal';
import type { Profile } from '../propTypes';
import { copyProfile } from '../../../api/quality-profiles';
import { translate, translateWithParameters } from '../../../helpers/l10n';

type Props = {
  onClose: () => void,
  onCopy: string => void,
  onRequestFail: Object => void,
  profile: Profile
};

type State = {
  loading: boolean,
  name: ?string
};

export default class CopyProfileForm extends React.PureComponent {
  mounted: boolean;
  props: Props;
  state: State = { loading: false, name: null };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleCancelClick = (event: Event) => {
    event.preventDefault();
    this.props.onClose();
  };

  handleNameChange = (event: { currentTarget: HTMLInputElement }) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleFormSubmit = (event: Event) => {
    event.preventDefault();

    const { name } = this.state;

    if (name != null) {
      this.setState({ loading: true });
      copyProfile(this.props.profile.key, name).then(
        profile => this.props.onCopy(profile.name),
        error => {
          if (this.mounted) {
            this.setState({ loading: false });
          }
          this.props.onRequestFail(error);
        }
      );
    }
  };

  render() {
    const { profile } = this.props;
    const header = translateWithParameters(
      'quality_profiles.copy_x_title',
      profile.name,
      profile.languageName
    );
    const submitDisabled =
      this.state.loading || !this.state.name || this.state.name === profile.name;

    return (
      <Modal
        isOpen={true}
        contentLabel={header}
        className="modal"
        overlayClassName="modal-overlay"
        onRequestClose={this.props.onClose}>

        <form id="copy-profile-form" onSubmit={this.handleFormSubmit}>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>
          <div className="modal-body">
            <div className="modal-field">
              <label htmlFor="copy-profile-name">
                {translate('quality_profiles.copy_new_name')}<em className="mandatory">*</em>
              </label>
              <input
                autoFocus={true}
                id="copy-profile-name"
                maxLength="100"
                name="name"
                onChange={this.handleNameChange}
                required={true}
                size="50"
                type="text"
                value={this.state.name != null ? this.state.name : profile.name}
              />
            </div>
          </div>
          <div className="modal-foot">
            {this.state.loading && <i className="spinner spacer-right" />}
            <button disabled={submitDisabled} id="copy-profile-submit">
              {translate('copy')}
            </button>
            <a href="#" id="copy-profile-cancel" onClick={this.handleCancelClick}>
              {translate('cancel')}
            </a>
          </div>
        </form>

      </Modal>
    );
  }
}
