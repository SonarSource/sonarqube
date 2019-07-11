/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { DateSource, FormattedDate } from 'react-intl';
import { parseDate } from 'sonar-ui-common/helpers/dates';

interface Props {
  children?: (formattedDate: string) => React.ReactNode;
  date: DateSource;
  long?: boolean;
}

export const formatterOption = { year: 'numeric', month: 'short', day: '2-digit' };

export const longFormatterOption = { year: 'numeric', month: 'long', day: 'numeric' };

export default function DateFormatter({ children, date, long }: Props) {
  return (
    <FormattedDate value={parseDate(date)} {...(long ? longFormatterOption : formatterOption)}>
      {children}
    </FormattedDate>
  );
}
