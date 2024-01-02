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
import { render } from '../../../helpers/testUtils';
import { CustomIcon, IconProps } from '../../icons/Icon';
import { GenericAvatar } from '../GenericAvatar';

function TestIcon(props: IconProps) {
  return (
    <CustomIcon {...props}>
      <path d="l10 10" />
    </CustomIcon>
  );
}

it('should render single word and size', () => {
  render(<GenericAvatar name="foo" size="xs" />);
  const image = screen.getByRole('img');
  expect(image).toHaveAttribute('size', '16');
  expect(screen.getByText('F')).toBeInTheDocument();
});

it('should render multiple word with default size', () => {
  render(<GenericAvatar name="foo bar" />);
  const image = screen.getByRole('img');
  expect(image).toHaveAttribute('size', '24');
  expect(screen.getByText('F')).toBeInTheDocument();
});

it('should render without name', () => {
  render(<GenericAvatar Icon={TestIcon} name="" size="md" />);
  const image = screen.getByRole('img');
  expect(image).toHaveAttribute('size', '40');
});
