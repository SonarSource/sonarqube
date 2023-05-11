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

import { Key } from '../../helpers/keyboard';
import { render } from '../../helpers/testUtils';
import { FCProps } from '../../types/misc';
import { KeyboardHintKeys } from '../KeyboardHintKeys';

it.each([
  Key.Control,
  Key.Command,
  Key.Alt,
  Key.Option,
  Key.ArrowUp,
  Key.ArrowDown,
  Key.ArrowLeft,
  Key.ArrowRight,
])('should render %s', (key) => {
  const { container } = setupWithProps({ command: key });
  expect(container).toMatchSnapshot();
});

it('should render multiple keys', () => {
  const { container } = setupWithProps({ command: `${Key.ArrowUp} ${Key.ArrowDown}` });
  expect(container).toMatchSnapshot();
});

it('should render multiple keys with non-key symbols', () => {
  const { container } = setupWithProps({
    command: `${Key.Control} + ${Key.ArrowDown} ${Key.ArrowUp}`,
  });
  expect(container).toMatchSnapshot();
});

it('should render a default text if no keys match', () => {
  const { container } = setupWithProps({ command: `${Key.Control} + click` });
  expect(container).toMatchSnapshot();
});

function setupWithProps(props: Partial<FCProps<typeof KeyboardHintKeys>> = {}) {
  return render(<KeyboardHintKeys command={`${Key.ArrowUp}`} {...props} />);
}
