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
import tw from 'twin.macro';
import { themeBorder, themeColor, themeContrast } from '../../helpers/theme';

export const SCMHighlight = styled.h6`
  color: ${themeColor('tooltipHighlight')};

  ${tw`sw-body-sm-highlight`};
  ${tw`sw-text-right`};
  ${tw`sw-min-w-[6rem]`};
  ${tw`sw-mr-4`};
  ${tw`sw-my-1`};
`;

export const LineSCMStyledDiv = styled.div`
  outline: none;

  ${tw`sw-pr-2`}
  ${tw`sw-truncate`}
${tw`sw-whitespace-nowrap`}
${tw`sw-cursor-pointer`}
${tw`sw-w-full sw-h-full`}

&:hover {
    color: ${themeColor('codeLineMetaHover')};
  }
`;

export const DuplicationHighlight = styled.h6`
  color: ${themeColor('tooltipHighlight')};

  ${tw`sw-mb-2 sw-font-semibold`};
`;

export const LineStyled = styled.tr`
  display: grid;
  grid-template-rows: auto;
  grid-template-columns: var(--columns);
  align-items: center;

  ${tw`sw-code`}
`;
LineStyled.displayName = 'LineStyled';

export const LineMeta = styled.td`
  color: ${themeColor('codeLineMeta')};
  background-color: var(--line-background);
  outline: none;

  ${tw`sw-w-full sw-h-full`}
  ${tw`sw-box-border`}
  ${tw`sw-select-none`}

  ${LineStyled}:hover & {
    background-color: ${themeColor('codeLineHover')};
  }
`;

export const LineCodePreFormatted = styled.pre`
  position: relative;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  tab-size: 4;
`;

export const LineCodeLayer = styled.div`
  grid-row: 1;
  grid-column: 1;
`;

export const LineCodeLayers = styled.td`
  position: relative;
  display: grid;
  height: 100%;
  background-color: var(--line-background);
  border-left: ${themeBorder('default', 'codeLineBorder')};

  ${LineStyled}:hover & {
    background-color: ${themeColor('codeLineHover')};
  }
`;

export const NewCodeUnderline = styled(LineCodeLayer)`
  background-color: ${themeColor('codeLineNewCodeUnderline')};
`;

export const CoveredUnderline = styled(LineCodeLayer)`
  background-color: ${themeColor('codeLineCoveredUnderline')};
`;

export const UncoveredUnderline = styled(LineCodeLayer)`
  background-color: ${themeColor('codeLineUncoveredUnderline')};
`;

export const UnderlineLabels = styled.div<{ transparentBackground?: boolean }>`
  ${tw`sw-absolute`}
  ${tw`sw-flex sw-gap-1`}
  ${tw`sw-px-1`}
  ${tw`sw-right-0`}


  height: 1.125rem;
  margin-top: -1.125rem;
  background-color: ${({ transparentBackground, theme }) =>
    themeColor(transparentBackground ? 'transparent' : 'codeLine')({ theme })};
`;

export const UnderlineLabel = styled.span`
  ${tw`sw-rounded-t-1`}
  ${tw`sw-px-1`}
`;

export const NewCodeUnderlineLabel = styled(UnderlineLabel)`
  color: ${themeContrast('codeLineNewCodeUnderline')};
  background-color: ${themeColor('codeLineNewCodeUnderline')};
`;

export const CoveredUnderlineLabel = styled(UnderlineLabel)`
  color: ${themeContrast('codeLineCoveredUnderline')};
  background-color: ${themeColor('codeLineCoveredUnderline')};
`;

export const UncoveredUnderlineLabel = styled(UnderlineLabel)`
  color: ${themeContrast('codeLineUncoveredUnderline')};
  background-color: ${themeColor('codeLineUncoveredUnderline')};
`;
