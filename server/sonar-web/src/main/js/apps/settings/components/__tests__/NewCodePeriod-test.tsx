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
import { shallow } from 'enzyme';
import * as React from 'react';
import { getNewCodePeriod, setNewCodePeriod } from '../../../../api/newCodePeriod';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import NewCodePeriod from '../NewCodePeriod';

jest.mock('../../../../api/newCodePeriod', () => ({
  getNewCodePeriod: jest.fn().mockResolvedValue({}),
  setNewCodePeriod: jest.fn(() => Promise.resolve()),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should load the current new code period on mount', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(getNewCodePeriod).toHaveBeenCalledTimes(1);
  expect(wrapper.state('currentSetting')).toBe('PREVIOUS_VERSION');
});

it('should load the current new code period with value on mount', async () => {
  (getNewCodePeriod as jest.Mock).mockResolvedValue({ type: 'NUMBER_OF_DAYS', value: '42' });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(getNewCodePeriod).toHaveBeenCalledTimes(1);
  expect(wrapper.state('currentSetting')).toBe('NUMBER_OF_DAYS');
  expect(wrapper.state('days')).toBe('42');
});

it('should only show the save button after changing the setting', async () => {
  (getNewCodePeriod as jest.Mock).mockResolvedValue({ type: 'PREVIOUS_VERSION' });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper.state('selected')).toBe('PREVIOUS_VERSION');
  expect(wrapper.find('SubmitButton')).toHaveLength(0);

  wrapper.instance().onSelectSetting('NUMBER_OF_DAYS');
  await waitAndUpdate(wrapper);

  expect(wrapper.find('SubmitButton')).toHaveLength(1);
});

it('should disable the button if the days are invalid', async () => {
  (getNewCodePeriod as jest.Mock).mockResolvedValue({ type: 'NUMBER_OF_DAYS', value: '42' });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().onSelectDays('asd');
  await waitAndUpdate(wrapper);

  expect(wrapper.find('SubmitButton').first().prop('disabled')).toBe(true);

  wrapper.instance().onSelectDays('23');
  await waitAndUpdate(wrapper);

  expect(wrapper.find('SubmitButton').first().prop('disabled')).toBe(false);
});

it('should submit correctly', async () => {
  (getNewCodePeriod as jest.Mock).mockResolvedValue({ type: 'NUMBER_OF_DAYS', value: '42' });

  const preventDefault = jest.fn();

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.instance().onSelectSetting('PREVIOUS_VERSION');
  await waitAndUpdate(wrapper);

  wrapper.find('form').simulate('submit', { preventDefault });

  expect(preventDefault).toHaveBeenCalledTimes(1);
  expect(setNewCodePeriod).toHaveBeenCalledWith({ type: 'PREVIOUS_VERSION', value: undefined });
  await waitAndUpdate(wrapper);
  expect(wrapper.state('currentSetting')).toEqual(wrapper.state('selected'));
});

it('should submit correctly with days', async () => {
  (getNewCodePeriod as jest.Mock).mockResolvedValue({ type: 'NUMBER_OF_DAYS', value: '42' });

  const preventDefault = jest.fn();

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.instance().onSelectDays('66');
  await waitAndUpdate(wrapper);

  wrapper.find('form').simulate('submit', { preventDefault });

  expect(preventDefault).toHaveBeenCalledTimes(1);
  expect(setNewCodePeriod).toHaveBeenCalledWith({ type: 'NUMBER_OF_DAYS', value: '66' });
  await waitAndUpdate(wrapper);
  expect(wrapper.state('currentSetting')).toEqual(wrapper.state('selected'));
});

function shallowRender() {
  return shallow<NewCodePeriod>(<NewCodePeriod />);
}
