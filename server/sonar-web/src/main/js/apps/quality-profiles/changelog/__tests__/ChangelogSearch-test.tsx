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
import { shallow } from 'enzyme';
import * as React from 'react';
import ChangelogSearch from '../ChangelogSearch';
import { click } from '../../../../helpers/testUtils';
import { parseDate } from '../../../../helpers/dates';

it('should render', () => {
  const output = shallow(
    <ChangelogSearch
      dateRange={{
        from: parseDate('2016-01-01T00:00:00.000Z'),
        to: parseDate('2016-05-05T00:00:00.000Z')
      }}
      onDateRangeChange={jest.fn()}
      onReset={jest.fn()}
    />
  );
  expect(output).toMatchSnapshot();
});

it('should reset', () => {
  const onReset = jest.fn();
  const output = shallow(
    <ChangelogSearch
      dateRange={{
        from: parseDate('2016-01-01T00:00:00.000Z'),
        to: parseDate('2016-05-05T00:00:00.000Z')
      }}
      onDateRangeChange={jest.fn()}
      onReset={onReset}
    />
  );
  expect(onReset).not.toBeCalled();
  click(output.find('Button'));
  expect(onReset).toBeCalled();
});
