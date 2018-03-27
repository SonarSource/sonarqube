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
import Measure from '@sqcore/components/measure/Measure';
import SizeRating from '@sqcore/components/ui/SizeRating';
import { Language } from '@sqcore/api/languages';
import { ProjectData } from '../../types';

interface Props extends Pick<ProjectData, 'measures'> {
  languages: Language[];
}

export default function MeasureLanguages({ languages, measures }: Props) {
  const parsedLanguages = measures['ncloc_language_distribution']
    .split(';')
    .map(item => item.split('='));
  const finalLanguages = sortBy(parsedLanguages, ([_key, loc]) => -1 * Number(loc)).map(([key]) => {
    const language = languages.find(language => language.key === key);
    return language ? language.name : key;
  });
  const languagesText =
    finalLanguages.slice(0, 2).join(', ') + (finalLanguages.length > 2 ? ', ...' : '');

  return (
    <div className="project-card-measure-language">
      <div className="project-card-measure-value">
        <Measure
          className="little-spacer-right"
          metricKey="ncloc"
          metricType="SHORT_INT"
          value={measures['ncloc']}
        />
        <SizeRating value={Number(measures['ncloc'])} />
      </div>
      <div
        className="project-card-measure-title"
        title={finalLanguages.length > 2 ? finalLanguages.join(', ') : undefined}>
        {languagesText}
      </div>
    </div>
  );
}
