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
import * as React from 'react';
import tw from 'twin.macro';
import { themeColor } from '../helpers';

export interface MessageFormatting {
  end: number;
  start: number;
  type: MessageFormattingType;
}

export enum MessageFormattingType {
  CODE = 'CODE',
}

export interface IssueMessageHighlightingProps {
  message?: string;
  messageFormattings?: MessageFormatting[];
}

export function IssueMessageHighlighting(props: IssueMessageHighlightingProps) {
  const { message, messageFormattings } = props;

  if (message === undefined || message === '') {
    return null;
  }

  if (!(messageFormattings && messageFormattings.length > 0)) {
    return <>{message}</>;
  }

  let previousEnd = 0;

  const sanitizedFormattings = [...messageFormattings]
    .sort((a, b) => a.start - b.start)
    .reduce<typeof messageFormattings>((acc, messageFormatting) => {
      const { type } = messageFormatting;

      if (type !== MessageFormattingType.CODE) {
        return acc;
      }

      const { start } = messageFormatting;
      let { end } = messageFormatting;

      end = Math.min(message.length, end);

      if (start < 0 || end === start || end < start) {
        return acc;
      }

      if (acc.length > 0) {
        const { start: previousStart, end: previousEnd } = acc[acc.length - 1];

        if (start <= previousEnd) {
          acc[acc.length - 1] = {
            start: previousStart,
            end: Math.max(previousEnd, end),
            type,
          };

          return acc;
        }
      }

      acc.push({ start, end, type });

      return acc;
    }, []);

  return (
    <span>
      {sanitizedFormattings.map(({ start, end, type }) => {
        const beginning = previousEnd;
        previousEnd = end;

        return (
          <React.Fragment key={`${message}-${start}-${end}`}>
            {message.slice(beginning, start)}
            {type === MessageFormattingType.CODE ? (
              <SingleLineSnippet className="sw-code sw-rounded-1 sw-px-1 sw-border sw-border-solid">
                {message.slice(start, end)}
              </SingleLineSnippet>
            ) : (
              <span>{message.slice(start, end)}</span>
            )}
          </React.Fragment>
        );
      })}

      {message.slice(previousEnd)}
    </span>
  );
}

const SingleLineSnippet = styled.span`
  background: ${themeColor('codeSnippetBackground')};
  border-color: ${themeColor('codeSnippetBorder')};
  color: ${themeColor('codeSnippetInline')};
  ${tw`sw-py-1/2`}

  a & {
    ${tw`sw-pb-0`}
  }
`;
