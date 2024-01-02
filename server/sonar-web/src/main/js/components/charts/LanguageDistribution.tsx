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
import * as React from 'react';
import withLanguagesContext from '../../app/components/languages/withLanguagesContext';
import Histogram from '../../components/charts/Histogram';
import { translate } from '../../helpers/l10n';
import { formatMeasure } from '../../helpers/measures';
import { Languages } from '../../types/languages';
import { MetricType } from '../../types/metrics';

interface LanguageDistributionProps {
  distribution: string;
  languages: Languages;
}

const NUMBER_FORMAT_THRESHOLD = 1000;

export function LanguageDistribution(props: LanguageDistributionProps) {
  let distribution = props.distribution.split(';').map((point) => {
    const tokens = point.split('=');
    return { language: tokens[0], lines: parseInt(tokens[1], 10) };
  });

  distribution = sortBy(distribution, (d) => -d.lines);

  const data = distribution.map((d) => d.lines);
  const yTicks = distribution.map((d) => getLanguageName(d.language)).map(cutLanguageName);
  const yTooltips = distribution.map((d) =>
    d.lines > NUMBER_FORMAT_THRESHOLD ? formatMeasure(d.lines, MetricType.Integer) : ''
  );
  const yValues = distribution.map((d) => formatMeasure(d.lines, MetricType.ShortInteger));

  return (
    <Histogram
      bars={data}
      height={distribution.length * 25}
      padding={[0, 60, 0, 80]}
      width={260}
      yTicks={yTicks}
      yTooltips={yTooltips}
      yValues={yValues}
    />
  );

  function getLanguageName(langKey: string) {
    if (langKey === '<null>') {
      return translate('unknown');
    }
    const lang = props.languages[langKey];
    return lang ? lang.name : langKey;
  }
}

function cutLanguageName(name: string) {
  return name.length > 10 ? `${name.substr(0, 7)}...` : name;
}

export default withLanguagesContext(LanguageDistribution);
