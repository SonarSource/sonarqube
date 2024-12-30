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
import { Histogram } from '~design-system';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { MetricType } from '~sonar-aligned/types/metrics';
import withLanguagesContext from '../../app/components/languages/withLanguagesContext';
import { translate } from '../../helpers/l10n';
import { Languages } from '../../types/languages';

export interface LanguageDistributionProps {
  distribution: string;
  languages: Languages;
}

const NUMBER_FORMAT_THRESHOLD = 1000;

export function LanguageDistribution(props: LanguageDistributionProps) {
  const { distribution, languages } = props;
  let parsedDistribution = distribution.split(';').map((point) => {
    const tokens = point.split('=');
    return { language: tokens[0], lines: parseInt(tokens[1], 10) };
  });

  parsedDistribution = sortBy(parsedDistribution, (d) => -d.lines);

  const data = parsedDistribution.map((d) => d.lines);
  const yTicks = parsedDistribution
    .map((d) => getLanguageName(languages, d.language))
    .map(cutLanguageName);
  const yTooltips = parsedDistribution.map((d) =>
    d.lines > NUMBER_FORMAT_THRESHOLD ? formatMeasure(d.lines, MetricType.Integer) : '',
  );
  const yValues = parsedDistribution.map((d) => formatMeasure(d.lines, MetricType.ShortInteger));

  return (
    <Histogram
      bars={data}
      height={parsedDistribution.length * 25}
      leftAlignTicks
      padding={[0, 60, 0, 80]}
      width={260}
      yTicks={yTicks}
      yTooltips={yTooltips}
      yValues={yValues}
    />
  );
}

function getLanguageName(languages: Languages, langKey: string) {
  if (langKey === '<null>') {
    return translate('unknown');
  }
  const lang = languages[langKey];
  return lang ? lang.name : langKey;
}

function cutLanguageName(name: string) {
  return name.length > 10 ? `${name.substr(0, 7)}...` : name;
}

export default withLanguagesContext(LanguageDistribution);
