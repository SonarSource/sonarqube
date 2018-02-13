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
import { deleteProfile } from '../../../api/quality-profiles';
import Modal from '../../../components/controls/Modal';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Profile } from '../types';

interface Props {
  onClose: () => void;
  onDelete: () => void;
  onRequestFail: (reason: any) => void;
  profile: Profile;
}

interface State {
  loading: boolean;
  name: string | null;
}

export default class DeleteProfileForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false, name: null };

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
    deleteProfile(this.props.profile.key).then(this.props.onDelete, (error: any) => {
      if (this.mounted) {
        this.setState({ loading: false });
      }
      this.props.onRequestFail(error);
    });
  };

  render() {
    const { profile } = this.props;
    const header = translate('quality_profiles.delete_confirm_title');

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <form id="delete-profile-form" onSubmit={this.handleFormSubmit}>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>
          <div className="modal-body">
            <div className="js-modal-messages" />
            {profile.childrenCount > 0 ? (
              <div>
                <div className="alert alert-warning">
                  {translate('quality_profiles.this_profile_has_descendants')}
                </div>
                <p>
                  {translateWithParameters(
                    'quality_profiles.are_you_sure_want_delete_profile_x_and_descendants',
                    profile.name,
                    profile.languageName
                  )}
                </p>
              </div>
            ) : (
              <p>
                {translateWithParameters(
                  'quality_profiles.are_you_sure_want_delete_profile_x',
                  profile.name,
                  profile.languageName
                )}
              </p>
            )}
          </div>
          <div className="modal-foot">
            {this.state.loading && <i className="spinner spacer-right" />}
            <button className="button-red" disabled={this.state.loading} id="delete-profile-submit">
              {translate('delete')}
            </button>
            <a href="#" id="delete-profile-cancel" onClick={this.handleCancelClick}>
              {translate('cancel')}
            </a>
          </div>
        </form>
      </Modal>
    );
  }
}
