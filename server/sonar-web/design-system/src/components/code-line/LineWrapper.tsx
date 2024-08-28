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
import { HTMLAttributes } from 'react';
import { themeColor } from '../../helpers/theme';
import { LineStyled } from './LineStyles';

interface Props extends HTMLAttributes<HTMLDivElement> {
  displayCoverage: boolean;
  displaySCM: boolean;
  duplicationsCount: number;
  highlighted: boolean;
}

export function LineWrapper(props: Props) {
  const { displayCoverage, displaySCM, duplicationsCount, highlighted, ...htmlProps } = props;
  const theme = useTheme();
  const SCMCol = displaySCM ? '50px ' : '';
  const nbGutters = duplicationsCount + (displayCoverage ? 1 : 0);
  const gutterCols = nbGutters > 0 ? `repeat(${nbGutters}, 6px) ` : '';
  return (
    <LineStyled
      style={{
        '--columns': `44px ${SCMCol}26px ${gutterCols}1fr`,
        '--line-background': highlighted
          ? themeColor('codeLineHighlighted')({ theme })
          : themeColor('codeLine')({ theme }),
      }}
      {...htmlProps}
    />
  );
}

export function SuggestedLineWrapper(props: Readonly<HTMLAttributes<HTMLDivElement>>) {
  const theme = useTheme();
  return (
    <LineStyled
      as="div"
      style={{
        '--columns': `44px 26px 1rem 1fr`,
        '--line-background': themeColor('codeLine')({ theme }),
      }}
      {...props}
    />
  );
}
