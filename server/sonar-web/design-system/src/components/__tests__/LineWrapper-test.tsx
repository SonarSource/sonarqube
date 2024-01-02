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
import { render } from '../../helpers/testUtils';
import { FCProps } from '../../types/misc';
import { LineWrapper } from '../code-line/LineWrapper';

it('should render with correct styling', () => {
  expect(setupWithProps().container).toMatchSnapshot();
});

it('should properly setup css grid columns', () => {
  expect(setupWithProps().container.firstChild).toHaveStyle({
    '--columns': '44px 50px 26px repeat(3, 6px) 1fr',
  });
  expect(setupWithProps({ duplicationsCount: 0 }).container.firstChild).toHaveStyle({
    '--columns': '44px 50px 26px repeat(1, 6px) 1fr',
  });
  expect(
    setupWithProps({ displayCoverage: false, displaySCM: false, duplicationsCount: 0 }).container
      .firstChild,
  ).toHaveStyle({ '--columns': '44px 26px 1fr' });
});

it('should set a highlighted background color in css props', () => {
  const { container } = setupWithProps({ highlighted: true });
  expect(container.firstChild).toHaveStyle({ '--line-background': 'rgb(225,230,243)' });
});

function setupWithProps(props: Partial<FCProps<typeof LineWrapper>> = {}) {
  return render(
    <LineWrapper displayCoverage displaySCM duplicationsCount={2} highlighted={false} {...props} />,
  );
}
