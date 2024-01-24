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
import * as React from 'react';
import { FormatDateOptions, FormattedDate } from 'react-intl';
import { parseDate } from '../../helpers/dates';
import { ParsableDate } from '../../types/dates';

interface Props {
  children?: (formattedDate: string) => React.ReactNode;
  date: ParsableDate;
  short?: boolean;
}

export const formatterOption: FormatDateOptions = {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
  hour: 'numeric',
  minute: 'numeric',
  hour12: false
};

export const longFormatterOption: FormatDateOptions = {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  hour: 'numeric',
  minute: 'numeric',
};

export default function DateTimeFormatter({ children, date, short }: Props) {
  return (
    <FormattedDate value={parseDate(date)} {...(short ? formatterOption : longFormatterOption)}>
      {children}
    </FormattedDate>
  );
}
