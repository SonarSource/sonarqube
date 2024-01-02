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
import classNames from 'classnames';
import * as React from 'react';
import { MessageFormatting, MessageFormattingType } from '../../types/issues';

export interface IssueMessageHighlightingProps {
  message?: string;
  messageFormattings?: MessageFormatting[];
}

export function IssueMessageHighlighting(props: IssueMessageHighlightingProps) {
  const { message, messageFormattings } = props;

  if (!message) {
    return null;
  }

  if (!(messageFormattings && messageFormattings.length > 0)) {
    return <>{message}</>;
  }

  let previousEnd = 0;

  const sanitizedFormattings = [...messageFormattings]
    .sort((a, b) => a.start - b.start)
    .reduce((acc, messageFormatting) => {
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
    }, [] as typeof messageFormattings);

  return (
    <span>
      {sanitizedFormattings.map(({ start, end, type }) => {
        const beginning = previousEnd;
        previousEnd = end;

        return (
          <React.Fragment key={`${message}-${start}-${end}`}>
            {message.slice(beginning, start)}
            <span
              className={classNames({
                'issue-message-highlight-CODE': type === MessageFormattingType.CODE,
              })}
            >
              {message.slice(start, end)}
            </span>
          </React.Fragment>
        );
      })}

      {message.slice(previousEnd)}
    </span>
  );
}
