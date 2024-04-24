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
import { render, screen } from '@testing-library/react';
import { ButtonPrimary } from '../ButtonPrimary';

it('renders ButtonPrimary correctly', () => {
  render(<ButtonPrimary>Hello</ButtonPrimary>);
  expect(screen.getByRole('button', { name: 'Hello' })).toBeInTheDocument();
});

it('renders ButtonPrimary correctly when to is defined', () => {
  render(
    <ButtonPrimary download="http://link.com" to="http://link.com">
      Hello
    </ButtonPrimary>,
  );
  expect(screen.queryByRole('button', { name: 'Hello' })).not.toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Hello' })).toBeInTheDocument();
});
