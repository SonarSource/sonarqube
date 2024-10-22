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
import { mockAppState } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import Avatar from '../Avatar';

const gravatarServerUrl = 'http://example.com/{EMAIL_MD5}.jpg?s={SIZE}';

it('renders correctly', () => {
  renderComponent(<Avatar name="John Doe" hash="johndoe" />, '', {
    appState: mockAppState({
      settings: {
        'sonar.lf.enableGravatar': 'true',
        'sonar.lf.gravatarServerUrl': gravatarServerUrl,
      },
    }),
  });
  const image = screen.getByAltText('John Doe');
  expect(image).toBeInTheDocument();
  expect(image).toHaveAttribute('src', 'http://example.com/johndoe.jpg?s=48');
});
