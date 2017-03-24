/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { unconnectedAvatar as Avatar } from '../Avatar';

const gravatarServerUrl = 'http://example.com/{EMAIL_MD5}.jpg?s={SIZE}';

it('should render', () => {
  const avatar = shallow(
    <Avatar
      enableGravatar={true}
      gravatarServerUrl={gravatarServerUrl}
      email="mail@example.com"
      size={20}
    />
  );
  expect(avatar.is('img')).toBe(true);
  expect(avatar.prop('width')).toBe(20);
  expect(avatar.prop('height')).toBe(20);
  expect(avatar.prop('alt')).toBe('mail@example.com');
  expect(avatar.prop('src')).toBe('http://example.com/7daf6c79d4802916d83f6266e24850af.jpg?s=40');
});

it('should not render', () => {
  const avatar = shallow(
    <Avatar
      enableGravatar={false}
      gravatarServerUrl={gravatarServerUrl}
      email="mail@example.com"
      size={20}
    />
  );
  expect(avatar.is('img')).toBe(false);
});

it('should be able to render with hash only', () => {
  const avatar = shallow(
    <Avatar
      enableGravatar={true}
      gravatarServerUrl={gravatarServerUrl}
      hash="7daf6c79d4802916d83f6266e24850af"
      size={30}
    />
  );
  expect(avatar.is('img')).toBe(true);
  expect(avatar.prop('width')).toBe(30);
  expect(avatar.prop('height')).toBe(30);
  expect(avatar.prop('alt')).toBeUndefined();
  expect(avatar.prop('src')).toBe('http://example.com/7daf6c79d4802916d83f6266e24850af.jpg?s=60');
});
