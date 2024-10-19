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
import { createRef, useRef } from 'react';
import { render } from '../../helpers/testUtils';
import { useResizeObserver } from '../useResizeObserver';

it('should return the correct width and height of the element', () => {
  render(<ExampleWithRefComponent />);
  expect(screen.getByText('width: 100')).toBeInTheDocument();
  expect(screen.getByText('height: 200')).toBeInTheDocument();
});

it('should return no values if no ref element is passed', () => {
  render(<ExampleNoRefComponent />);
  expect(screen.getByText('width: NONE')).toBeInTheDocument();
  expect(screen.getByText('height: NONE')).toBeInTheDocument();
});

function ExampleWithRefComponent() {
  const containerRef = useRef(null);
  const [width, height] = useResizeObserver(containerRef);
  return (
    <div ref={containerRef}>
      some content<span>width: {width}</span>
      <span>height: {height}</span>
    </div>
  );
}

function ExampleNoRefComponent() {
  const ref = createRef<HTMLDivElement>();
  const [width, height] = useResizeObserver(ref);
  return (
    <div>
      some content<span>width: {width ?? 'NONE'}</span>
      <span>height: {height ?? 'NONE'}</span>
    </div>
  );
}
