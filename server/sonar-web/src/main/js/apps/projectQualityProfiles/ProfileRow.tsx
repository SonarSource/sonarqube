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
import Select from 'sonar-ui-common/components/controls/Select';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { Profile } from '../../api/quality-profiles';

interface Props {
  onChangeProfile: (oldProfile: string, newProfile: string) => Promise<void>;
  possibleProfiles: Profile[];
  profile: Profile;
}

interface State {
  loading: boolean;
}

export default class ProfileRow extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  handleChange = (option: { value: string }) => {
    if (this.props.profile.key !== option.value) {
      this.setState({ loading: true });
      this.props
        .onChangeProfile(this.props.profile.key, option.value)
        .then(this.stopLoading, this.stopLoading);
    }
  };

  renderProfileName = (profileOption: { isDefault: boolean; label: string }) => {
    if (profileOption.isDefault) {
      return (
        <span>
          <strong>{translate('default')}</strong>
          {': '}
          {profileOption.label}
        </span>
      );
    }

    return <span>{profileOption.label}</span>;
  };

  renderProfileSelect() {
    const { profile, possibleProfiles } = this.props;

    const options = possibleProfiles.map(profile => ({
      value: profile.key,
      label: profile.name,
      isDefault: profile.isDefault
    }));

    return (
      <Select
        clearable={false}
        disabled={this.state.loading}
        onChange={this.handleChange}
        optionRenderer={this.renderProfileName}
        options={options}
        style={{ width: 300 }}
        value={profile.key}
        valueRenderer={this.renderProfileName}
      />
    );
  }

  render() {
    const { profile } = this.props;

    return (
      <tr data-key={profile.language}>
        <td className="thin nowrap">{profile.languageName}</td>
        <td className="thin nowrap">{this.renderProfileSelect()}</td>
        <td>{this.state.loading && <i className="spinner" />}</td>
      </tr>
    );
  }
}
