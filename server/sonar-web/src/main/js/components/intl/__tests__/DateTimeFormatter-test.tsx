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

import { screen } from '@testing-library/react';
import * as React from 'react';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import DateTimeFormatter from '../DateTimeFormatter';

it('should render correctly', () => {
  renderDateTimeFormatter();
  expect(screen.getByText(/February 20, 2020(\sat|,) 8:20 PM/)).toBeInTheDocument();

  renderDateTimeFormatter((formatted: string) => <span>Nice date: {formatted}</span>);
  expect(screen.getByText(/Nice date: February 20, 2020(\sat|,) 8:20 PM/)).toBeInTheDocument();
});

function renderDateTimeFormatter(children?: (d: string) => React.ReactNode) {
  return renderComponent(
    <DateTimeFormatter date={new Date('2020-02-20T20:20:20Z')} timeZone="UTC">
      {children}
    </DateTimeFormatter>,
  );
}
