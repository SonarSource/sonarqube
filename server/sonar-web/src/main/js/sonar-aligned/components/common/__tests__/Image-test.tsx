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
import React from 'react';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { Image } from '../Image';

describe('should render correctly', () => {
  it('with a src', () => {
    setupWithProps({
      src: 'foo.png',
    });

    expect(screen.getByRole('img').outerHTML).toEqual('<img src="foo.png">');
  });

  it('with a src and alt', () => {
    setupWithProps({
      src: 'foo.png',
      alt: 'bar',
    });

    expect(screen.getByRole('img').outerHTML).toEqual('<img alt="bar" src="foo.png">');
  });

  it('should strip beginning slashes', () => {
    setupWithProps({
      src: '/foo.png',
    });

    expect(screen.getByRole('img').outerHTML).toEqual('<img src="/foo.png">');
  });
});

function setupWithProps(props: Partial<Readonly<JSX.IntrinsicElements['img']>> = {}) {
  return renderComponent(<Image {...props} />);
}
