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
import Select from 'react-select';
import { sortBy } from 'lodash';
import { changeProfileParent } from '../../../api/quality-profiles';
import { translate } from '../../../helpers/l10n';
import type { Profile } from '../propTypes';

type Props = {
  onChange: () => void,
  onClose: () => void,
  onRequestFail: Object => void,
  profile: Profile,
  profiles: Array<Profile>
};

type State = {
  loading: boolean,
  selected: ?string
};

export default class ChangeParentForm extends React.PureComponent {
  mounted: boolean;
  props: Props;
  state: State = {
    loading: false,
    selected: null
  };

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

  handleSelectChange = (option: { value: string }) => {
    this.setState({ selected: option.value });
  };

  handleFormSubmit = (event: Event) => {
    event.preventDefault();

    const parent = this.state.selected;

    if (parent != null) {
      this.setState({ loading: true });
      changeProfileParent(this.props.profile.key, parent).then(this.props.onChange).catch(error => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
        this.props.onRequestFail(error);
      });
    }
  };

  render() {
    const { profiles } = this.props;

    const options = [
      { label: translate('none'), value: '' },
      ...sortBy(profiles, 'name').map(profile => ({
        label: profile.isBuiltIn
          ? `${profile.name} (${translate('quality_profiles.built_in')})`
          : profile.name,
        value: profile.key
      }))
    ];

    const submitDisabled =
      this.state.loading ||
      this.state.selected == null ||
      this.state.selected === this.props.profile.parentKey;

    return (
      <Modal
        isOpen={true}
        contentLabel={translate('quality_profiles.change_parent')}
        className="modal"
        overlayClassName="modal-overlay"
        onRequestClose={this.props.onClose}>

        <form id="change-profile-parent-form" onSubmit={this.handleFormSubmit}>
          <div className="modal-head">
            <h2>{translate('quality_profiles.change_parent')}</h2>
          </div>
          <div className="modal-body">
            <div className="modal-field">
              <label htmlFor="change-profile-parent">
                {translate('quality_profiles.parent')}: <em className="mandatory">*</em>
              </label>
              <Select
                clearable={false}
                id="change-profile-parent"
                name="parentKey"
                onChange={this.handleSelectChange}
                options={options}
                value={
                  this.state.selected != null
                    ? this.state.selected
                    : this.props.profile.parentKey || ''
                }
              />
            </div>
          </div>
          <div className="modal-foot">
            {this.state.loading && <i className="spinner spacer-right" />}
            <button disabled={submitDisabled} id="change-profile-parent-submit">
              {translate('change_verb')}
            </button>
            <a href="#" id="change-profile-parent-cancel" onClick={this.handleCancelClick}>
              {translate('cancel')}
            </a>
          </div>
        </form>

      </Modal>
    );
  }
}
