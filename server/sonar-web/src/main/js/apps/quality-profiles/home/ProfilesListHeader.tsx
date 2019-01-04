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
import { Option } from 'react-select';
import { translate } from '../../../helpers/l10n';
import { getProfilesPath, getProfilesForLanguagePath } from '../utils';
import Select from '../../../components/controls/Select';
import { withRouter, Router } from '../../../components/hoc/withRouter';

interface Props {
  currentFilter?: string;
  languages: Array<{ key: string; name: string }>;
  organization: string | null;
  router: Pick<Router, 'replace'>;
}

export class ProfilesListHeader extends React.PureComponent<Props> {
  handleChange = (option: { value: string } | null) => {
    const { organization, router } = this.props;

    router.replace(
      !option
        ? getProfilesPath(organization)
        : getProfilesForLanguagePath(option.value, organization)
    );
  };

  render() {
    const { currentFilter, languages } = this.props;
    if (languages.length < 2) {
      return null;
    }

    const options: Option[] = languages.map(language => ({
      label: language.name,
      value: language.key
    }));

    const currentLanguage = currentFilter && options.find(l => l.value === currentFilter);

    return (
      <header className="quality-profiles-list-header clearfix">
        <span className="spacer-right">{translate('quality_profiles.filter_by')}:</span>
        <Select
          className="input-medium"
          clearable={true}
          onChange={this.handleChange}
          options={options}
          value={currentLanguage}
        />
      </header>
    );
  }
}

export default withRouter(ProfilesListHeader);
