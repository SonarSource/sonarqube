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
import { groupBy, pick, sortBy } from 'lodash';
import ProfilesListRow from './ProfilesListRow';
import ProfilesListHeader from './ProfilesListHeader';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { TooltipsContainer } from '../../../components/mixins/tooltips-mixin';
import type { Profile } from '../propTypes';

type Props = {
  canAdmin: boolean,
  languages: Array<{ key: string, name: string }>,
  location: { query: { [string]: string } },
  onRequestFail: Object => void,
  organization: ?string,
  profiles: Array<Profile>,
  updateProfiles: () => Promise<*>
};

export default class ProfilesList extends React.PureComponent {
  props: Props;

  renderProfiles(profiles: Array<Profile>) {
    return profiles.map(profile => (
      <ProfilesListRow
        canAdmin={this.props.canAdmin}
        key={profile.key}
        onRequestFail={this.props.onRequestFail}
        organization={this.props.organization}
        profile={profile}
        updateProfiles={this.props.updateProfiles}
      />
    ));
  }

  renderHeader(languageKey: string, profilesCount: number) {
    const language = this.props.languages.find(l => l.key === languageKey);

    if (!language) {
      return null;
    }

    return (
      <thead>
        <tr>
          <th>
            {language.name}
            {', '}
            {translateWithParameters('quality_profiles.x_profiles', profilesCount)}
          </th>
          <th className="text-right nowrap">
            {translate('quality_profiles.list.projects')}
          </th>
          <th className="text-right nowrap">
            {translate('quality_profiles.list.rules')}
          </th>
          <th className="text-right nowrap">
            {translate('quality_profiles.list.updated')}
          </th>
          <th className="text-right nowrap">
            {translate('quality_profiles.list.used')}
          </th>
          {this.props.canAdmin && <th>&nbsp;</th>}
        </tr>
      </thead>
    );
  }

  render() {
    const { profiles, languages } = this.props;
    const { language } = this.props.location.query;

    const profilesIndex = groupBy(profiles, profile => profile.language);
    const profilesToShow = language ? pick(profilesIndex, language) : profilesIndex;

    const languagesToShow = sortBy(Object.keys(profilesToShow));

    return (
      <div>
        <ProfilesListHeader
          currentFilter={language}
          languages={languages}
          organization={this.props.organization}
        />

        {Object.keys(profilesToShow).length === 0 &&
          <div className="alert alert-warning spacer-top">
            {translate('no_results')}
          </div>}

        {languagesToShow.map(languageKey => (
          <div key={languageKey} className="quality-profile-box quality-profiles-table">
            <table data-language={languageKey} className="data zebra zebra-hover">

              {profilesToShow[languageKey] != null &&
                this.renderHeader(languageKey, profilesToShow[languageKey].length)}

              <TooltipsContainer>
                <tbody>
                  {profilesToShow[languageKey] != null &&
                    this.renderProfiles(profilesToShow[languageKey])}
                </tbody>
              </TooltipsContainer>

            </table>
          </div>
        ))}

      </div>
    );
  }
}
