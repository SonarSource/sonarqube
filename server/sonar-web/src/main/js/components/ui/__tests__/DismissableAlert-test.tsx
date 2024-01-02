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
import React from 'react';
import { ButtonIcon } from '../../../components/controls/buttons';
import { save } from '../../../helpers/storage';
import { click } from '../../../helpers/testUtils';
import DismissableAlert, {
  DismissableAlertProps,
  DISMISSED_ALERT_STORAGE_KEY,
} from '../DismissableAlert';

jest.mock('../../../helpers/storage', () => ({
  get: jest.fn((_: string, suffix: string) => (suffix === 'bar' ? 'true' : undefined)),
  save: jest.fn(),
}));

jest.mock('react', () => {
  return {
    ...jest.requireActual('react'),
    useEffect: jest.fn(),
  };
});

beforeEach(() => {
  jest.clearAllMocks();
  (React.useEffect as jest.Mock).mockImplementationOnce((f) => f());
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render correctly with a non-default display', () => {
  expect(shallowRender({ display: 'block' })).toMatchSnapshot();
});

it('should not render if it was dismissed', () => {
  expect(shallowRender({ alertKey: 'bar' }).type()).toBeNull();
});

it('should correctly allow dismissing', () => {
  const wrapper = shallowRender();
  click(wrapper.find(ButtonIcon));
  expect(save).toHaveBeenCalledWith(DISMISSED_ALERT_STORAGE_KEY, 'true', 'foo');
});

function shallowRender(props: Partial<DismissableAlertProps> = {}) {
  return shallow<DismissableAlertProps>(
    <DismissableAlert alertKey="foo" variant="info" {...props}>
      <div>My content</div>
    </DismissableAlert>
  );
}
