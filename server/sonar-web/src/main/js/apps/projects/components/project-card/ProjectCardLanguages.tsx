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

import { sortBy } from 'lodash';
import withLanguagesContext from '../../../../app/components/languages/withLanguagesContext';
import Tooltip from '../../../../components/controls/Tooltip';
import { translate } from '../../../../helpers/l10n';
import { Languages } from '../../../../types/languages';

interface Props {
  className?: string;
  distribution?: string;
  languages: Languages;
}

const MAX_DISPLAYED_LANGUAGES = 2;

export function ProjectCardLanguages({ className, distribution, languages }: Props) {
  if (distribution === undefined) {
    return null;
  }

  const parsedLanguages = distribution.split(';').map((item) => item.split('='));
  const finalLanguages = sortBy(parsedLanguages, (l) => -1 * Number(l[1])).map((l) =>
    getLanguageName(languages, l[0]),
  );

  const languagesText =
    finalLanguages.slice(0, MAX_DISPLAYED_LANGUAGES).join(', ') +
    (finalLanguages.length > MAX_DISPLAYED_LANGUAGES ? ', ...' : '');

  const tooltip =
    finalLanguages.length > MAX_DISPLAYED_LANGUAGES ? (
      <span>
        {finalLanguages.map((language) => (
          <span key={language}>
            {language}
            <br />
          </span>
        ))}
      </span>
    ) : null;

  return (
    <Tooltip content={tooltip}>
      <span className={className}>{languagesText}</span>
    </Tooltip>
  );
}

function getLanguageName(languages: Languages, key: string): string {
  if (key === '<null>') {
    return translate('unknown');
  }
  const language = languages[key];
  return language != null ? language.name : key;
}

export default withLanguagesContext(ProjectCardLanguages);
