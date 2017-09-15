/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import * as React from 'react';
import { find, sortBy } from 'lodash';
import { Histogram } from './histogram';
import { formatMeasure } from '../../helpers/measures';
import { Language } from '../../api/languages';
import { translate } from '../../helpers/l10n';

interface Props {
  alignTicks?: boolean;
  distribution: string;
  languages?: Language[];
}

export default function LanguageDistribution(props: Props) {
  let data = props.distribution.split(';').map((point, index) => {
    const tokens = point.split('=');
    return { x: parseInt(tokens[1], 10), y: index, value: tokens[0] };
  });

  data = sortBy(data, d => -d.x);

  const yTicks = data.map(point => getLanguageName(point.value)).map(cutLanguageName);
  const yValues = data.map(point => formatMeasure(point.x, 'SHORT_INT'));

  return (
    <Histogram
      alignTicks={props.alignTicks}
      data={data}
      yTicks={yTicks}
      yValues={yValues}
      barsWidth={10}
      height={data.length * 25}
      padding={[0, 60, 0, 80]}
    />
  );

  function getLanguageName(langKey: string) {
    const lang = find(props.languages, { key: langKey });
    return lang ? lang.name : translate('unknown');
  }

  function cutLanguageName(name: string) {
    return name.length > 10 ? `${name.substr(0, 7)}...` : name;
  }
}
