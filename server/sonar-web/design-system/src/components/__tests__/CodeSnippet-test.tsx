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
import { screen } from '@testing-library/react';
import { HelmetProvider } from 'react-helmet-async';
import { renderWithContext } from '../../helpers/testUtils';
import { FCProps } from '../../types/misc';
import { CodeSnippet } from '../CodeSnippet';

it('should show full size when multiline with no editing', () => {
  const { container } = setupWithProps();
  const copyButton = screen.getByRole('button', { name: 'Copy' });
  expect(copyButton).toHaveStyle('top: 1.5rem');
  expect(container).toMatchSnapshot();
});

it('should show reduced size when single line with no editing', () => {
  const { container } = setupWithProps({ isOneLine: true, snippet: 'foobar' });
  const copyButton = screen.getByRole('button', { name: 'Copy' });
  expect(copyButton).toHaveStyle('top: 1rem');
  expect(container).toMatchSnapshot();
});

it('should highlight code content correctly', () => {
  const { container } = setupWithProps({ snippet: '<prop>foobar<prop>' });
  expect(container).toMatchSnapshot();
});

function setupWithProps(props: Partial<FCProps<typeof CodeSnippet>> = {}) {
  return renderWithContext(
    <HelmetProvider>
      <CodeSnippet snippet={'foo\nbar'} {...props} />
    </HelmetProvider>,
  );
}
