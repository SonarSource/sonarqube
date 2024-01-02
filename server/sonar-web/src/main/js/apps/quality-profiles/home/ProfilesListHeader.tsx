/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import Select from '../../../components/controls/Select';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import { translate } from '../../../helpers/l10n';
import { PROFILE_PATH } from '../constants';
import { getProfilesForLanguagePath } from '../utils';

interface Props {
  currentFilter?: string;
  languages: Array<{ key: string; name: string }>;
  router: Router;
}

export class ProfilesListHeader extends React.PureComponent<Props> {
  handleChange = (option: { value: string } | null) => {
    const { router } = this.props;

    router.replace(!option ? PROFILE_PATH : getProfilesForLanguagePath(option.value));
  };

  render() {
    const { currentFilter, languages } = this.props;
    if (languages.length < 2) {
      return null;
    }

    const options = languages.map((language) => ({
      label: language.name,
      value: language.key,
    }));

    return (
      <div className="quality-profiles-list-header clearfix">
        <label htmlFor="quality-profiles-filter-input" className="spacer-right">
          {translate('quality_profiles.filter_by')}:
        </label>
        <Select
          className="input-medium"
          autoFocus={true}
          id="quality-profiles-filter"
          inputId="quality-profiles-filter-input"
          isClearable={true}
          onChange={this.handleChange}
          options={options}
          isSearchable={true}
          value={options.filter((o) => o.value === currentFilter)}
        />
      </div>
    );
  }
}

export default withRouter(ProfilesListHeader);
