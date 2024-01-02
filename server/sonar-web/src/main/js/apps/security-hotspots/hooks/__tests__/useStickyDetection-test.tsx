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
import { act } from '@testing-library/react';
import React from 'react';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { byRole } from '../../../../helpers/testSelector';
import { mockIntersectionObserver } from '../../../../helpers/testUtils';
import useStickyDetection from '../useStickyDetection';

it('should render correctly based on intersection callback', () => {
  const intersect = mockIntersectionObserver();
  renderComponent(<StickyComponent />);

  expect(byRole('heading', { name: 'static' }).get()).toBeInTheDocument();

  act(() => {
    intersect({
      isIntersecting: false,
      intersectionRatio: 0.99,
      boundingClientRect: { top: 1 },
      intersectionRect: { top: 0 },
    });
  });

  expect(byRole('heading', { name: 'sticky' }).get()).toBeInTheDocument();
});

function StickyComponent() {
  const isSticky = useStickyDetection('.target', { offset: 0 });

  return (
    <div className="target">
      <h1>{isSticky ? 'sticky' : 'static'}</h1>
    </div>
  );
}
