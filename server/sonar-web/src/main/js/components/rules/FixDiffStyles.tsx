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
import { themeColor } from '~design-system';

// Styled components for header
export const FixDiffHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border: 1px solid ${({ theme }) => themeColor('codeLineBorder')({ theme })};
  border-bottom: none;
  background-color: ${({ theme }) => themeColor('backgroundSecondary')({ theme })};
`;

export const BranchSelect = styled.div`
  position: relative;
  margin-left: 8px;
  padding: 0 12px 0 0;
`;

export const BranchSelectButton = styled.button`
  display: flex;
  height: 36px;
  max-width: var(--width-800, 800px);
  min-height: var(--height-36, 36px);
  padding: 1px 13px;
  align-items: center;
  gap: 8px;
  border-radius: 8px;
  border: var(--stroke-weight-1, 1px) solid var(--color-azure-82, #C5CDDF);
  background: var(--color-white-solid, #FFF);
  cursor: pointer;
  
  &:hover {
    opacity: 0.9;
  }
`;

export const BranchIcon = styled.div`
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  
  svg {
    width: 18px;
    height: 18px;
  }
`;

export const BranchText = styled.span`
  color: var(--CodeScan-Lynch, var(--color-azure-48, #637192));
  text-align: center;
  font-family: var(--font-family-Font-1, Inter);
  font-size: var(--font-size-14, 14px);
  font-style: normal;
  font-weight: var(--font-weight-400, 400);
  line-height: var(--font-size-20, 20px);
`;

// Styled components for diff line renderer
export const DiffLineRow = styled.tr<{ $bgColor: string }>`
  display: grid;
  grid-template-columns: 44px 44px 26px 1fr; /* Two line number columns, marker, code */
  align-items: start;
  background-color: ${(props) => props.$bgColor};
  border: none;
  margin: 0;
  padding: 0;
  
  &:hover {
    background-color: ${(props) => 
      props.$bgColor === 'transparent' 
        ? themeColor('codeLineHover')({ theme: props.theme })
        : props.$bgColor
    };
  }
  
  td {
    background-color: ${(props) => props.$bgColor} !important;
    border: none;
    padding: 0;
    margin: 0;
  }
`;

export const DiffLineNumberCell = styled.td<{ $align: 'left' | 'right'; $bgColor: string }>`
  text-align: ${(props) => props.$align};
  padding: 0 8px;
  user-select: none;
  font-size: 12px;
  line-height: 22px;
  min-width: 44px;
  width: 44px;
  background-color: ${(props) => props.$bgColor} !important;
  border-right: 1px solid ${({ theme }) => themeColor('codeLineBorder')({ theme })};
  color: var(--echoes-color-text-subdued);
  vertical-align: top;
  min-height: 22px;
`;

export const DiffMarkerCell = styled.td<{ $bgColor: string }>`
  padding: 0 4px;
  text-align: center;
  user-select: none;
  min-width: 26px;
  width: 26px;
  background-color: ${(props) => props.$bgColor} !important;
  border-right: 1px solid ${({ theme }) => themeColor('codeLineBorder')({ theme })};
  vertical-align: top;
  line-height: 22px;
  min-height: 22px;
`;

export const DiffMarker = styled.span<{ $type: 'added' | 'removed' }>`
  color: rgb(46, 44, 44);
  font-weight: 400;
  font-size: 13px;
`;

export const DiffCodeCell = styled.td<{ $bgColor: string }>`
  padding: 0 8px;
  background-color: ${(props) => props.$bgColor} !important;
  vertical-align: top;
  line-height: 22px;
  min-height: 22px;
  overflow: visible;
  word-wrap: break-word;
  width: 100%;
`;

export const DiffCodeContent = styled.div`
  font-size: 12px;
  line-height: 22px;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  tab-size: 4;
  
  code {
    background: transparent;
    padding: 0;
    font-family: inherit;
    font-size: inherit;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
  }
`;

// Create Pull Request Button styles
export const CreatePRButton = styled.button<{ $active?: boolean }>`
  display: flex;
  width: 182px;
  height: 36px;
  padding: 0 12px;
  justify-content: center;
  align-items: center;
  gap: 6px;
  border-radius: 8px;
  background: ${({ $active }) => ($active ? 'var(--color-azure-82, #C5CDDF)' : '#5d6cd0')};
  border: none;
  cursor: ${({ disabled }) => (disabled ? 'not-allowed' : 'pointer')};
  opacity: ${({ disabled }) => (disabled ? 0.8 : 1)};
  transition: opacity 0.2s;

  &:hover:not(:disabled) {
    opacity: 0.9;
  }

  &:active:not(:disabled) {
    opacity: 0.8;
  }
`;

export const CreatePRIcon = styled.div`
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;

  svg {
    width: 18px;
    height: 18px;
  }
`;

export const CreatePRText = styled.span`
  color: var(--color-white-solid, #fff);
  text-align: center;
  font-family: var(--font-family-Font-1, Inter);
  font-size: var(--font-size-14, 14px);
  font-style: normal;
  font-weight: var(--font-weight-600, 600);
  line-height: var(--font-size-20, 20px);
`;

