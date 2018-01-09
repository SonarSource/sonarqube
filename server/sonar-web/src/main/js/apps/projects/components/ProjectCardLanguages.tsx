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
import { sortBy } from 'lodash';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import { Languages } from '../../../store/languages/reducer';

interface Props {
  distribution?: string;
  languages: Languages;
}

export default function ProjectCardLanguages({ distribution, languages }: Props) {
  if (distribution === undefined) {
    return null;
  }

  const parsedLanguages = distribution.split(';').map(item => item.split('='));
  const finalLanguages = sortBy(parsedLanguages, l => -1 * Number(l[1])).map(l =>
    getLanguageName(languages, l[0])
  );

  const tooltip = (
    <span>
      {finalLanguages.map(language => (
        <span key={language}>
          {language}
          <br />
        </span>
      ))}
    </span>
  );

  const languagesText =
    finalLanguages.slice(0, 2).join(', ') + (finalLanguages.length > 2 ? ', ...' : '');

  return (
    <div className="project-card-languages">
      <Tooltip placement="bottom" overlay={tooltip}>
        <span>{languagesText}</span>
      </Tooltip>
    </div>
  );
}

function getLanguageName(languages: Languages, key: string): string {
  if (key === '<null>') {
    return translate('unknown');
  }
  const language = languages[key];
  return language != null ? language.name : key;
}
