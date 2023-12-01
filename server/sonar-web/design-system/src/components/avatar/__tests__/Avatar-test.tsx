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

/* eslint-disable import/no-extraneous-dependencies */

import { fireEvent, screen } from '@testing-library/react';
import { render } from '../../../helpers/testUtils';
import { FCProps } from '../../../types/misc';
import { Avatar } from '../Avatar';

const gravatarServerUrl = 'http://example.com/{EMAIL_MD5}.jpg?s={SIZE}';

it('should render avatar with border', () => {
  setupWithProps({ border: true, hash: '7daf6c79d4802916d83f6266e24850af' });
  expect(screen.getByRole('img')).toHaveStyle('border: 1px solid rgb(225,230,243)');
});

it('should be able to render with hash only', () => {
  setupWithProps({ hash: '7daf6c79d4802916d83f6266e24850af' });
  expect(screen.getByRole('img')).toHaveAttribute(
    'src',
    'http://example.com/7daf6c79d4802916d83f6266e24850af.jpg?s=48',
  );
});

it('should fall back to generated on error', () => {
  setupWithProps({ hash: '7daf6c79d4802916d83f6266e24850af' });
  fireEvent(screen.getByRole('img'), new Event('error'));
  expect(screen.getByRole('img')).not.toHaveAttribute('src');
});

it('should fall back to dummy avatar', () => {
  setupWithProps({ enableGravatar: false });
  expect(screen.getByRole('img')).not.toHaveAttribute('src');
});

it('should return null if no name is set', () => {
  setupWithProps({ name: undefined });
  expect(screen.queryByRole('img')).not.toBeInTheDocument();
});

it('should display organization avatar correctly', () => {
  const avatar = 'http://example.com/avatar.png';
  setupWithProps({ organizationAvatar: avatar, organizationName: 'my-org' });
  expect(screen.getByRole('img')).toHaveAttribute('src', avatar);
});

function setupWithProps(props: Partial<FCProps<typeof Avatar>> = {}) {
  return render(
    <Avatar enableGravatar gravatarServerUrl={gravatarServerUrl} name="foo" {...props} />,
  );
}
