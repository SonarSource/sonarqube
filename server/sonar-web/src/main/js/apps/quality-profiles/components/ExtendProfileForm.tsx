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
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { changeProfileParent, createQualityProfile } from '../../../api/quality-profiles';
import { Profile } from '../types';

interface Props {
  onClose: () => void;
  onExtend: (name: string) => void;
  organization: string | null;
  profile: Profile;
}

interface State {
  loading: boolean;
  name?: string;
}

type ValidState = State & Required<Pick<State, 'name'>>;

export default class ExtendProfileForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  canSubmit = (state: State): state is ValidState => {
    return Boolean(state.name && state.name.length);
  };

  handleNameChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleFormSubmit = async () => {
    if (this.canSubmit(this.state)) {
      const { organization, profile: parentProfile } = this.props;
      const { name } = this.state;

      const data = new FormData();

      data.append('language', parentProfile.language);
      data.append('name', name);

      if (organization) {
        data.append('organization', organization);
      }

      this.setState({ loading: true });

      try {
        const { profile: newProfile } = await createQualityProfile(data);
        await changeProfileParent(newProfile.key, parentProfile.key);
        this.props.onExtend(newProfile.name);
      } finally {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    }
  };

  render() {
    const { profile } = this.props;
    const header = translateWithParameters(
      'quality_profiles.extend_x_title',
      profile.name,
      profile.languageName
    );

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose} size="small">
        <form>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>
          <div className="modal-body">
            <div className="modal-field">
              <label htmlFor="extend-profile-name">
                {translate('quality_profiles.copy_new_name')}
                <em className="mandatory">*</em>
              </label>
              <input
                autoFocus={true}
                id="extend-profile-name"
                maxLength={100}
                name="name"
                onChange={this.handleNameChange}
                required={true}
                size={50}
                type="text"
                value={this.state.name ? this.state.name : ''}
              />
            </div>
          </div>
          <div className="modal-foot">
            <DeferredSpinner className="spacer-right" loading={this.state.loading} />
            <SubmitButton
              disabled={this.state.loading || !this.canSubmit(this.state)}
              id="extend-profile-submit"
              onClick={this.handleFormSubmit}>
              {translate('extend')}
            </SubmitButton>
            <ResetButtonLink id="extend-profile-cancel" onClick={this.props.onClose}>
              {translate('cancel')}
            </ResetButtonLink>
          </div>
        </form>
      </Modal>
    );
  }
}
