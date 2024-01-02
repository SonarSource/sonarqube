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
import { getValue, resetSettingValue, setSettingValue } from '../../../../api/settings';
import { mockDefinition, mockSettingValue } from '../../../../helpers/mocks/settings';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { SettingType } from '../../../../types/settings';
import Definition from '../Definition';

jest.mock('../../../../api/settings', () => ({
  getValue: jest.fn().mockResolvedValue({}),
  resetSettingValue: jest.fn().mockResolvedValue(undefined),
  setSettingValue: jest.fn().mockResolvedValue(undefined),
}));

beforeAll(() => {
  jest.useFakeTimers();
});

afterAll(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
});

beforeEach(() => {
  jest.clearAllMocks();
});

describe('Handle change (and check)', () => {
  it.each([
    ['empty, no default', mockDefinition(), '', 'settings.state.value_cant_be_empty_no_default'],
    [
      'empty, default',
      mockDefinition({ defaultValue: 'dflt' }),
      '',
      'settings.state.value_cant_be_empty',
    ],
    [
      'invalid url',
      mockDefinition({ key: 'sonar.core.serverBaseURL' }),
      '%invalid',
      'settings.state.url_not_valid.%invalid',
    ],
    [
      'valid url',
      mockDefinition({ key: 'sonar.core.serverBaseURL' }),
      'http://www.sonarqube.org',
      undefined,
    ],
    [
      'invalid JSON',
      mockDefinition({ type: SettingType.JSON }),
      '{{broken: "json}',
      'Unexpected token { in JSON at position 1',
    ],
    ['valid JSON', mockDefinition({ type: SettingType.JSON }), '{"validJson": true}', undefined],
  ])(
    'should handle change (and check value): %s',
    (_caseName, definition, changedValue, expectedValidationMessage) => {
      const wrapper = shallowRender({ definition });

      wrapper.instance().handleChange(changedValue);

      expect(wrapper.state().changedValue).toBe(changedValue);
      expect(wrapper.state().success).toBe(false);
      expect(wrapper.state().validationMessage).toBe(expectedValidationMessage);
    }
  );
});

it('should handle cancel', () => {
  const wrapper = shallowRender();
  wrapper.setState({ changedValue: 'whatever', validationMessage: 'something wrong' });

  wrapper.instance().handleCancel();

  expect(wrapper.state().changedValue).toBeUndefined();
  expect(wrapper.state().validationMessage).toBeUndefined();
});

describe('handleSave', () => {
  it('should ignore when value unchanged', () => {
    const wrapper = shallowRender();

    wrapper.instance().handleSave();

    expect(wrapper.state().loading).toBe(false);
    expect(setSettingValue).not.toHaveBeenCalled();
  });

  it('should handle an empty value', () => {
    const wrapper = shallowRender();

    wrapper.setState({ changedValue: '' });

    wrapper.instance().handleSave();

    expect(wrapper.state().loading).toBe(false);
    expect(wrapper.state().validationMessage).toBe('settings.state.value_cant_be_empty');
    expect(setSettingValue).not.toHaveBeenCalled();
  });

  it('should save and update setting value', async () => {
    const settingValue = mockSettingValue();
    (getValue as jest.Mock).mockResolvedValueOnce(settingValue);
    const definition = mockDefinition();
    const wrapper = shallowRender({ definition });

    wrapper.setState({ changedValue: 'new value' });

    wrapper.instance().handleSave();

    expect(wrapper.state().loading).toBe(true);

    await waitAndUpdate(wrapper);

    expect(setSettingValue).toHaveBeenCalledWith(definition, 'new value', undefined);
    expect(getValue).toHaveBeenCalledWith({ key: definition.key, component: undefined });
    expect(wrapper.state().changedValue).toBeUndefined();
    expect(wrapper.state().loading).toBe(false);
    expect(wrapper.state().success).toBe(true);
    expect(wrapper.state().settingValue).toBe(settingValue);

    jest.runAllTimers();
    expect(wrapper.state().success).toBe(false);
  });
});

it('should reset and update setting value', async () => {
  const settingValue = mockSettingValue();
  (getValue as jest.Mock).mockResolvedValueOnce(settingValue);
  const definition = mockDefinition();
  const wrapper = shallowRender({ definition });

  wrapper.instance().handleReset();

  expect(wrapper.state().loading).toBe(true);

  await waitAndUpdate(wrapper);

  expect(resetSettingValue).toHaveBeenCalledWith({ keys: definition.key, component: undefined });
  expect(getValue).toHaveBeenCalledWith({ key: definition.key, component: undefined });
  expect(wrapper.state().changedValue).toBeUndefined();
  expect(wrapper.state().loading).toBe(false);
  expect(wrapper.state().success).toBe(true);
  expect(wrapper.state().settingValue).toBe(settingValue);

  jest.runAllTimers();
  expect(wrapper.state().success).toBe(false);
});

function shallowRender(props: Partial<Definition['props']> = {}) {
  return shallow<Definition>(<Definition definition={mockDefinition()} {...props} />);
}
