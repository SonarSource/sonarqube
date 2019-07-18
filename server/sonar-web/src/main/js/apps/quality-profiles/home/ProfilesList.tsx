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
import { Location } from 'history';
import { groupBy, pick, sortBy } from 'lodash';
import * as React from 'react';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import DocTooltip from '../../../components/docs/DocTooltip';
import { Profile } from '../types';
import ProfilesListHeader from './ProfilesListHeader';
import ProfilesListRow from './ProfilesListRow';

interface Props {
  languages: T.Language[];
  location: Pick<Location, 'query'>;
  organization: string | null;
  profiles: Profile[];
  updateProfiles: () => Promise<void>;
}

export default class ProfilesList extends React.PureComponent<Props> {
  renderProfiles(profiles: Profile[]) {
    return profiles.map(profile => (
      <ProfilesListRow
        key={profile.key}
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
            <DocTooltip
              className="table-cell-doc"
              doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/quality-profiles/quality-profile-projects.md')}
            />
          </th>
          <th className="text-right nowrap">{translate('quality_profiles.list.rules')}</th>
          <th className="text-right nowrap">{translate('quality_profiles.list.updated')}</th>
          <th className="text-right nowrap">{translate('quality_profiles.list.used')}</th>
          <th>&nbsp;</th>
        </tr>
      </thead>
    );
  }

  renderLanguage = (languageKey: string, profiles: Profile[] | undefined) => {
    return (
      <div className="boxed-group boxed-group-inner quality-profiles-table" key={languageKey}>
        <table className="data zebra zebra-hover" data-language={languageKey}>
          {profiles !== undefined && this.renderHeader(languageKey, profiles.length)}
          <tbody>{profiles !== undefined && this.renderProfiles(profiles)}</tbody>
        </table>
      </div>
    );
  };

  render() {
    const { profiles, languages } = this.props;
    const { language } = this.props.location.query;

    const profilesIndex: T.Dict<Profile[]> = groupBy<Profile>(
      profiles,
      profile => profile.language
    );

    const profilesToShow = language ? pick(profilesIndex, language) : profilesIndex;

    const languagesToShow = sortBy(Object.keys(profilesToShow));

    return (
      <div>
        <ProfilesListHeader
          currentFilter={language}
          languages={languages}
          organization={this.props.organization}
        />

        {Object.keys(profilesToShow).length === 0 && (
          <Alert className="spacer-top" variant="warning">
            {translate('no_results')}
          </Alert>
        )}

        {languagesToShow.map(languageKey =>
          this.renderLanguage(languageKey, profilesToShow[languageKey])
        )}
      </div>
    );
  }
}
