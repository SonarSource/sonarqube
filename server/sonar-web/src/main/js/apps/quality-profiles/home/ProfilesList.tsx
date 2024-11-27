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

import { Button, ButtonVariety, IconQuestionMark, Tooltip } from '@sonarsource/echoes-react';
import { groupBy, pick, sortBy } from 'lodash';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { ContentCell, FlagMessage, Table, TableRow } from '~design-system';
import { Language } from '../../../types/languages';
import { Dict } from '../../../types/types';
import { Profile } from '../types';
import ProfilesListRow from './ProfilesListRow';

interface Props {
  language?: string;
  languages: Language[];
  profiles: Profile[];
  updateProfiles: () => Promise<void>;
}

export default function ProfilesList(props: Readonly<Props>) {
  const { profiles, languages, language } = props;
  const intl = useIntl();

  const profilesIndex: Dict<Profile[]> = groupBy<Profile>(profiles, (profile) => profile.language);
  const profilesToShow = language ? pick(profilesIndex, language) : profilesIndex;

  let languagesToShow = sortBy(languages, ({ name }) => name).map(({ key }) => key);

  if (language) {
    languagesToShow = languagesToShow.find((key) => key === language) ? [language] : [];
  }

  const renderHeader = React.useCallback(
    (languageKey: string, count: number) => {
      const language = languages.find((l) => l.key === languageKey);

      return (
        <TableRow>
          <ContentCell>
            {intl.formatMessage(
              { id: 'quality_profiles.x_profiles' },
              { count, name: language?.name },
            )}
          </ContentCell>
          <ContentCell>
            {intl.formatMessage({ id: 'quality_profiles.list.projects' })}
            <Tooltip content={intl.formatMessage({ id: 'quality_profiles.list.projects.help' })}>
              <Button
                className="sw-p-0 sw-h-fit sw-min-h-fit"
                aria-label={intl.formatMessage({ id: 'help' })}
                variety={ButtonVariety.DefaultGhost}
              >
                <IconQuestionMark color="echoes-color-icon-subdued" />
              </Button>
            </Tooltip>
          </ContentCell>
          <ContentCell>{intl.formatMessage({ id: 'quality_profiles.list.rules' })}</ContentCell>
          <ContentCell>{intl.formatMessage({ id: 'quality_profiles.list.updated' })}</ContentCell>
          <ContentCell>{intl.formatMessage({ id: 'quality_profiles.list.used' })}</ContentCell>
          <ContentCell>&nbsp;</ContentCell>
        </TableRow>
      );
    },
    [languages, intl],
  );

  return (
    <div>
      {Object.keys(profilesToShow).length === 0 && (
        <FlagMessage className="sw-mt-4 sw-w-full" variant="warning">
          {intl.formatMessage({ id: 'no_results' })}
        </FlagMessage>
      )}

      {languagesToShow.map((languageKey) => (
        <Table
          className="sw-mb-12"
          noSidePadding
          noHeaderTopBorder
          key={languageKey}
          columnCount={6}
          columnWidths={['43%', '14%', '14%', '14%', '14%', '1%']}
          header={renderHeader(languageKey, profilesToShow[languageKey].length)}
          data-language={languageKey}
        >
          {(profilesToShow[languageKey] ?? []).map((profile) => (
            <ProfilesListRow
              key={profile.key}
              profile={profile}
              updateProfiles={props.updateProfiles}
              isComparable={profilesToShow[languageKey].length > 1}
            />
          ))}
        </Table>
      ))}
    </div>
  );
}
