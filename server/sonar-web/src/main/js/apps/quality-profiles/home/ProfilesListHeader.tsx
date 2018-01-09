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
import { IndexLink } from 'react-router';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getProfilesPath, getProfilesForLanguagePath } from '../utils';

interface Props {
  currentFilter?: string;
  languages: Array<{ key: string; name: string }>;
  organization: string | null;
}

export default class ProfilesListHeader extends React.PureComponent<Props> {
  renderFilterToggle() {
    const { languages, currentFilter } = this.props;
    const currentLanguage = currentFilter && languages.find(l => l.key === currentFilter);

    const label = currentLanguage
      ? translateWithParameters('quality_profiles.x_Profiles', currentLanguage.name)
      : translate('quality_profiles.all_profiles');

    return (
      <a
        className="dropdown-toggle link-no-underline js-language-filter"
        href="#"
        data-toggle="dropdown">
        {label} <i className="icon-dropdown" />
      </a>
    );
  }

  renderFilterMenu() {
    return (
      <ul className="dropdown-menu">
        <li>
          <IndexLink to={getProfilesPath(this.props.organization)}>
            {translate('quality_profiles.all_profiles')}
          </IndexLink>
        </li>
        {this.props.languages.map(language => (
          <li key={language.key}>
            <IndexLink
              to={getProfilesForLanguagePath(language.key, this.props.organization)}
              className="js-language-filter-option"
              data-language={language.key}>
              {language.name}
            </IndexLink>
          </li>
        ))}
      </ul>
    );
  }

  render() {
    if (this.props.languages.length < 2) {
      return null;
    }

    const { languages, currentFilter } = this.props;
    const currentLanguage = currentFilter && languages.find(l => l.key === currentFilter);

    // if unknown language, then
    if (currentFilter && !currentLanguage) {
      return null;
    }

    return (
      <header className="quality-profiles-list-header clearfix">
        <div className="dropdown">
          {this.renderFilterToggle()}
          {this.renderFilterMenu()}
        </div>
      </header>
    );
  }
}
