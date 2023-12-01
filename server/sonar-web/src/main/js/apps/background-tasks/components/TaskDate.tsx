/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { differenceInDays } from 'date-fns';
import { Note, NumericalCell, themeColor } from 'design-system';
import * as React from 'react';
import TimeFormatter from '../../../components/intl/TimeFormatter';
import { isValidDate, parseDate } from '../../../helpers/dates';

interface Props {
  date?: string;
  baseDate?: string;
}

export default function TaskDate({ date, baseDate }: Readonly<Props>) {
  const parsedDate = date !== undefined && parseDate(date);
  const parsedBaseDate = baseDate !== undefined && parseDate(baseDate);
  const diff =
    parsedDate && parsedBaseDate && isValidDate(parsedDate) && isValidDate(parsedBaseDate)
      ? differenceInDays(parsedDate, parsedBaseDate)
      : 0;

  return (
    <NumericalCell className="sw-px-2">
      {diff > 0 && <StyledWarningText className="sw-mr-1">{`(+${diff}d)`}</StyledWarningText>}

      {parsedDate && isValidDate(parsedDate) ? (
        <span className="sw-whitespace-nowrap">
          <TimeFormatter date={parsedDate} long />
        </span>
      ) : (
        ''
      )}
    </NumericalCell>
  );
}

const StyledWarningText = styled(Note)`
  color: ${themeColor('warningText')};
`;
