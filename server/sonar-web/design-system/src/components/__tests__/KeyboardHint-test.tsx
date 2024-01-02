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
import { Key } from '../../helpers/keyboard';
import { render } from '../../helpers/testUtils';
import { FCProps } from '../../types/misc';
import { KeyboardHint } from '../KeyboardHint';

afterEach(() => {
  jest.clearAllMocks();
});

it('renders without title', () => {
  const { container } = setupWithProps();
  expect(container).toMatchSnapshot();
});

it('renders with title', () => {
  const { container } = setupWithProps({ title: 'title' });
  expect(container).toMatchSnapshot();
});

it('renders with command', () => {
  const { container } = setupWithProps({ command: 'command' });
  expect(container).toMatchSnapshot();
});

it('renders on mac', () => {
  Object.defineProperty(navigator, 'userAgent', {
    configurable: true,
    value: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_4)',
  });
  const { container } = setupWithProps({ command: `${Key.Control} ${Key.Alt}` });
  expect(container).toMatchSnapshot();
});

it('renders on windows', () => {
  Object.defineProperty(navigator, 'userAgent', {
    configurable: true,
    value: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
  });
  const { container } = setupWithProps({ command: `${Key.Control} ${Key.Alt}` });
  expect(container).toMatchSnapshot();
});

function setupWithProps(props: Partial<FCProps<typeof KeyboardHint>> = {}) {
  return render(<KeyboardHint command="click" {...props} />);
}
