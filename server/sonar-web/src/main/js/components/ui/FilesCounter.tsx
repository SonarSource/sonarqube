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

import styled from '@emotion/styled';
import classNames from 'classnames';
import { Note, themeColor } from '~design-system';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { MetricType } from '~sonar-aligned/types/metrics';
import { translate } from '../../helpers/l10n';
import { isDefined } from '../../helpers/types';

interface Props {
  className?: string;
  current?: number;
  total: number;
}

export default function FilesCounter({ className, current, total }: Props) {
  return (
    <Note className={classNames('sw-whitespace-nowrap', className)}>
      <Counter className="sw-typo-semibold">
        {isDefined(current) && formatMeasure(current, MetricType.Integer) + '/'}
        {formatMeasure(total, MetricType.Integer)}
      </Counter>{' '}
      {translate('component_measures.files')}
    </Note>
  );
}

FilesCounter.displayName = 'FilesCounter';

const Counter = styled.strong`
  color: ${themeColor('pageContent')};
`;
