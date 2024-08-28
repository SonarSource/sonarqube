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
import {
  CodeSyntaxHighlighter,
  LineMeta,
  LineStyled,
  SuggestedLineWrapper,
  themeBorder,
  themeColor,
} from 'design-system';
import React from 'react';
import { LineTypeEnum } from '../../queries/fix-suggestions';

type LineType = 'code' | 'added' | 'removed';

export function IssueSuggestionLine({
  language,
  line,
  lineAfter,
  lineBefore,
  type = 'code',
}: Readonly<{
  language?: string;
  line: string;
  lineAfter: number;
  lineBefore: number;
  type: LineType;
}>) {
  return (
    <SuggestedLineWrapper>
      <LineMeta as="div">
        {type !== LineTypeEnum.ADDED && (
          <LineNumberStyled className="sw-px-1 sw-inline-block">{lineBefore}</LineNumberStyled>
        )}
      </LineMeta>
      <LineMeta as="div">
        {type !== LineTypeEnum.REMOVED && (
          <LineNumberStyled className="sw-px-1 sw-inline-block">{lineAfter}</LineNumberStyled>
        )}
      </LineMeta>
      <LineDirectionMeta as="div">
        {type === LineTypeEnum.REMOVED && (
          <RemovedLineLayer className="sw-px-2">-</RemovedLineLayer>
        )}
        {type === LineTypeEnum.ADDED && <AddedLineLayer className="sw-px-2">+</AddedLineLayer>}
      </LineDirectionMeta>
      <LineCodeLayers>
        {type === LineTypeEnum.CODE && (
          <LineCodeLayer className="sw-px-3">
            <CodeSyntaxHighlighter
              htmlAsString={`<pre style="white-space: pre-wrap;">${line}</pre>`}
              language={language}
              escapeDom={false}
            />
          </LineCodeLayer>
        )}
        {type === LineTypeEnum.REMOVED && (
          <RemovedLineLayer className="sw-px-3">
            <LineCodePreFormatted>
              <CodeSyntaxHighlighter
                htmlAsString={`<pre style="white-space: pre-wrap;">${line}</pre>`}
                language={language}
                escapeDom={false}
              />
            </LineCodePreFormatted>
          </RemovedLineLayer>
        )}
        {type === LineTypeEnum.ADDED && (
          <AddedLineLayer className="sw-px-3">
            <LineCodePreFormatted>
              <CodeSyntaxHighlighter
                htmlAsString={`<pre style="white-space: pre-wrap;">${line}</pre>`}
                language={language}
                escapeDom={false}
              />
            </LineCodePreFormatted>
          </AddedLineLayer>
        )}
      </LineCodeLayers>
    </SuggestedLineWrapper>
  );
}

const LineNumberStyled = styled.div`
  &:hover {
    color: ${themeColor('codeLineMetaHover')};
  }

  &:focus-visible {
    outline-offset: -1px;
  }
`;

const LineCodeLayers = styled.div`
  position: relative;
  display: grid;
  height: 100%;
  background-color: var(--line-background);

  ${LineStyled}:hover & {
    background-color: ${themeColor('codeLineHover')};
  }
`;

const LineDirectionMeta = styled(LineMeta)`
  border-left: ${themeBorder('default', 'codeLineBorder')};
`;

const LineCodeLayer = styled.div`
  grid-row: 1;
  grid-column: 1;
`;

const LineCodePreFormatted = styled.pre`
  position: relative;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  tab-size: 4;
`;

const AddedLineLayer = styled.div`
  background-color: ${themeColor('codeLineCoveredUnderline')};
`;

const RemovedLineLayer = styled.div`
  background-color: ${themeColor('codeLineUncoveredUnderline')};
`;
