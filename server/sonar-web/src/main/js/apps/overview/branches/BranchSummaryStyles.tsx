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
import { themeColor } from 'design-system/lib';

export const GridContainer = styled.div`
  --grids-gaps: var(--echoes-dimension-space-500);
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: var(--grids-gaps);
`;

export const StyleMeasuresCard = styled.div`
  box-sizing: border-box;
  position: relative;

  &:not(:last-child):before {
    content: '';
    position: absolute;
    top: 0;
    right: calc(var(--grids-gaps) / -2);
    height: 100%;
    width: 1px;
    background: ${themeColor('pageBlockBorder')};
  }

  &:not(:last-child):after {
    content: '';
    position: absolute;
    bottom: calc(var(--grids-gaps) / -2);
    right: 0;
    left: 0px;
    height: 1px;
    width: 100vw;
    background: ${themeColor('pageBlockBorder')};
  }
`;

export const StyleMeasuresCardRightBorder = styled.div`
  box-sizing: border-box;
  position: relative;

  &:not(:last-child):before {
    content: '';
    position: absolute;
    top: 0;
    right: calc(var(--grids-gaps) / -2);
    height: 100%;
    width: 1px;
    background: ${themeColor('pageBlockBorder')};
  }
`;

export const StyledConditionsCard = styled.div`
  box-sizing: border-box;
  position: relative;
  &:before {
    content: '';
    position: absolute;
    top: 0;
    right: calc(var(--grids-gaps) / -2);
    height: 100%;
    width: 1px;
    background: ${themeColor('pageBlockBorder')};
  }
`;
