/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import DateInput from '../../../../components/controls/DateInput';
import { click } from '../../../../helpers/testUtils';

it('should render DateInput', () => {
  const onFromDateChange = jest.fn();
  const onToDateChange = jest.fn();
  const output = shallow(
    <ChangelogSearch
      fromDate="2016-01-01"
      toDate="2016-05-05"
      onFromDateChange={onFromDateChange}
      onToDateChange={onToDateChange}
      onReset={jest.fn()}
    />
  );
  const dateInputs = output.find(DateInput);
  expect(dateInputs.length).toBe(2);
  expect(dateInputs.at(0).prop('value')).toBe('2016-01-01');
  expect(dateInputs.at(0).prop('onChange')).toBe(onFromDateChange);
  expect(dateInputs.at(1).prop('value')).toBe('2016-05-05');
  expect(dateInputs.at(1).prop('onChange')).toBe(onToDateChange);
});

it('should reset', () => {
  const onReset = jest.fn();
  const output = shallow(
    <ChangelogSearch
      fromDate="2016-01-01"
      toDate="2016-05-05"
      onFromDateChange={jest.fn()}
      onToDateChange={jest.fn()}
      onReset={onReset}
    />
  );
  expect(onReset).not.toBeCalled();
  click(output.find('button'));
  expect(onReset).toBeCalled();
});
