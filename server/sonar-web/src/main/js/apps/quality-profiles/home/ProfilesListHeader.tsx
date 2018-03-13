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
import * as classNames from 'classnames';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getProfilesPath, getProfilesForLanguagePath } from '../utils';
import Dropdown from '../../../components/controls/Dropdown';

interface Props {
  currentFilter?: string;
  languages: Array<{ key: string; name: string }>;
  organization: string | null;
}

export default function ProfilesListHeader({ currentFilter, languages, organization }: Props) {
  if (languages.length < 2) {
    return null;
  }

  const currentLanguage = currentFilter && languages.find(l => l.key === currentFilter);

  // if unknown language, then
  if (currentFilter && !currentLanguage) {
    return null;
  }

  const label = currentLanguage
    ? translateWithParameters('quality_profiles.x_Profiles', currentLanguage.name)
    : translate('quality_profiles.all_profiles');

  return (
    <header className="quality-profiles-list-header clearfix">
      <Dropdown>
        {({ onToggleClick, open }) => (
          <div className={classNames('dropdown', { open })}>
            <a
              className="dropdown-toggle link-no-underline js-language-filter"
              href="#"
              onClick={onToggleClick}>
              {label}
              <i className="icon-dropdown little-spacer-left" />
            </a>

            <ul className="dropdown-menu">
              <li>
                <IndexLink to={getProfilesPath(organization)}>
                  {translate('quality_profiles.all_profiles')}
                </IndexLink>
              </li>
              {languages.map(language => (
                <li key={language.key}>
                  <IndexLink
                    className="js-language-filter-option"
                    data-language={language.key}
                    to={getProfilesForLanguagePath(language.key, organization)}>
                    {language.name}
                  </IndexLink>
                </li>
              ))}
            </ul>
          </div>
        )}
      </Dropdown>
    </header>
  );
}
