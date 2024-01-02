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
import React from 'react';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { MessageFormattingType } from '../../../types/issues';
import {
  IssueMessageHighlighting,
  IssueMessageHighlightingProps,
} from '../IssueMessageHighlighting';

it.each([
  [undefined, undefined],
  ['message', undefined],
  ['message', []],
  ['message', [{ start: 1, end: 4, type: 'something else' as MessageFormattingType }]],
  [
    'message',
    [
      { start: 5, end: 6, type: MessageFormattingType.CODE },
      { start: 1, end: 4, type: MessageFormattingType.CODE },
    ],
  ],
  [
    'a somewhat longer message with overlapping ranges',
    [{ start: -1, end: 1, type: MessageFormattingType.CODE }],
  ],
  [
    'a somewhat longer message with overlapping ranges',
    [{ start: 48, end: 70, type: MessageFormattingType.CODE }],
  ],
  [
    'a somewhat longer message with overlapping ranges',
    [{ start: 0, end: 0, type: MessageFormattingType.CODE }],
  ],
  [
    'a somewhat longer message with overlapping ranges',
    [
      { start: 11, end: 17, type: MessageFormattingType.CODE },
      { start: 2, end: 25, type: MessageFormattingType.CODE },
      { start: 25, end: 2, type: MessageFormattingType.CODE },
    ],
  ],
  [
    'a somewhat longer message with overlapping ranges',
    [
      { start: 18, end: 30, type: MessageFormattingType.CODE },
      { start: 2, end: 25, type: MessageFormattingType.CODE },
    ],
  ],
])('should format the string with highlights', (message, messageFormattings) => {
  const { asFragment } = renderIssueMessageHighlighting({ message, messageFormattings });
  expect(asFragment()).toMatchSnapshot();
});

function renderIssueMessageHighlighting(props: Partial<IssueMessageHighlightingProps> = {}) {
  return renderComponent(<IssueMessageHighlighting {...props} />);
}
