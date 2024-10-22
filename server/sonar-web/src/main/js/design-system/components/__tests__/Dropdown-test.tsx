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
import { renderWithRouter } from '../../helpers/testUtils';
import { ButtonSecondary } from '../../sonar-aligned/components/buttons';
import { Dropdown } from '../Dropdown';

describe('Dropdown', () => {
  it('renders', async () => {
    const { user } = renderDropdown();
    expect(screen.getByRole('button')).toBeInTheDocument();

    await user.click(screen.getByRole('button'));
    expect(screen.getByRole('menu')).toBeInTheDocument();
  });

  it('toggles with render prop', async () => {
    const { user } = renderDropdown({
      children: ({ onToggleClick }) => <ButtonSecondary onClick={onToggleClick} />,
    });

    await user.click(screen.getByRole('button'));
    expect(screen.getByRole('menu')).toBeVisible();
  });

  it('closes when clicking outside of menu', async () => {
    const { user } = renderDropdown();

    await user.click(screen.getByRole('button'));
    expect(screen.getByRole('menu')).toBeInTheDocument();

    await user.click(document.body);
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });

  it('does not close when clicking ouside of menu', async () => {
    const { user } = renderDropdown({ withClickOutHandler: false });

    await user.click(screen.getByRole('button'));
    expect(screen.getByRole('menu')).toBeInTheDocument();

    await user.click(document.body);
    expect(screen.getByRole('menu')).toBeInTheDocument();
  });

  it('closes when other target gets focus', async () => {
    const { user } = renderDropdown();

    await user.click(screen.getByRole('button'));
    expect(screen.getByRole('menu')).toBeInTheDocument();

    await user.tab();

    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });

  it('does not close when other target gets focus', async () => {
    const { user } = renderDropdown({ withFocusOutHandler: false });

    await user.click(screen.getByRole('button'));
    expect(screen.getByRole('menu')).toBeInTheDocument();

    await user.tab();
    expect(screen.getByRole('menu')).toBeInTheDocument();
  });

  function renderDropdown(props: Partial<Dropdown['props']> = {}) {
    const { children, ...rest } = props;
    return renderWithRouter(
      <Dropdown id="test-menu" overlay={<div id="overlay" />} {...rest}>
        {children ?? <ButtonSecondary />}
      </Dropdown>,
    );
  }
});
