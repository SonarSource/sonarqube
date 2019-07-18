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
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { deleteProfile } from '../../../api/quality-profiles';
import { Profile } from '../types';

interface Props {
  onClose: () => void;
  onDelete: () => void;
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

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ loading: true });
    deleteProfile(this.props.profile.key).then(this.props.onDelete, () => {
      if (this.mounted) {
        this.setState({ loading: false });
      }
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
                <Alert variant="warning">
                  {translate('quality_profiles.this_profile_has_descendants')}
                </Alert>
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
            <SubmitButton
              className="button-red"
              disabled={this.state.loading}
              id="delete-profile-submit">
              {translate('delete')}
            </SubmitButton>
            <ResetButtonLink id="delete-profile-cancel" onClick={this.props.onClose}>
              {translate('cancel')}
            </ResetButtonLink>
          </div>
        </form>
      </Modal>
    );
  }
}
