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
import userEvent from '@testing-library/user-event';
import { render } from '../../../helpers/testUtils';
import { FCProps } from '../../../types/misc';
import { SubnavigationAccordion } from '../SubnavigationAccordion';

it('should have correct style and html structure', () => {
  setupWithProps({ expanded: false });

  expect(screen.getByRole('button', { expanded: false })).toBeVisible();
  expect(screen.queryByText('Foo')).not.toBeInTheDocument();
});

it('should display expanded', () => {
  setupWithProps({ initExpanded: true });

  expect(screen.getByRole('button', { expanded: true })).toBeVisible();
  expect(screen.getByText('Foo')).toBeVisible();
});

it('should display collapsed by default', () => {
  setupWithProps();

  expect(screen.getByRole('button')).toBeVisible();
  expect(screen.queryByText('Foo')).not.toBeInTheDocument();
});

it('should toggle expand', async () => {
  const user = userEvent.setup();
  const onSetExpanded = jest.fn();
  setupWithProps({ onSetExpanded, expanded: false });

  expect(onSetExpanded).not.toHaveBeenCalled();
  await user.click(screen.getByRole('button', { expanded: false }));

  expect(onSetExpanded).toHaveBeenCalledWith(true);
});

it('should toggle collapse', async () => {
  const user = userEvent.setup();
  const onSetExpanded = jest.fn();
  setupWithProps({ onSetExpanded, expanded: true });

  expect(onSetExpanded).not.toHaveBeenCalled();
  await user.click(screen.getByRole('button', { expanded: true }));

  expect(onSetExpanded).toHaveBeenCalledWith(false);
});

function setupWithProps(props: Partial<FCProps<typeof SubnavigationAccordion>> = {}) {
  return render(
    <SubnavigationAccordion header="Header" id="test" {...props}>
      <span>Foo</span>
    </SubnavigationAccordion>,
  );
}
