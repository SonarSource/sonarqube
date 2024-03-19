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
import { useTheme } from '@emotion/react';
import classNames from 'classnames';
import { Theme, themeColor } from 'design-system';
import * as React from 'react';
import { LINE_CHART_DASHES } from './utils';

interface Props {
  className?: string;
  index: number;
}

export function ChartLegend({ index, className }: Readonly<Props>) {
  const theme = useTheme() as Theme;

  return (
    <svg
      className={className}
      clipRule="evenodd"
      fillRule="evenodd"
      height={16}
      strokeMiterlimit={1.41421}
      viewBox="0 0 16 16"
      width={16}
      xmlSpace="preserve"
      xmlnsXlink="http://www.w3.org/1999/xlink"
    >
      <path
        className={classNames('line-chart-path', `line-chart-path-${index}`)}
        d="M0 8 L 16 8"
        style={{
          stroke: themeColor(`graphLineColor.${index}` as Parameters<typeof themeColor>[0])({
            theme,
          }),
          strokeDasharray: LINE_CHART_DASHES[index],
        }}
      />
    </svg>
  );
}
