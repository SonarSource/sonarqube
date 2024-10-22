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
import ComponentsServiceMock from '../../../../api/mocks/ComponentsServiceMock';
import IssuesServiceMock from '../../../../api/mocks/IssuesServiceMock';
import { mockLocation } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import SourceViewerWrapper, { SourceViewerWrapperProps } from '../SourceViewerWrapper';

const issuesHandler = new IssuesServiceMock();
const componentsHandler = new ComponentsServiceMock();
// eslint-disable-next-line testing-library/no-node-access
const originalQuerySelector = document.querySelector;
const scrollIntoView = jest.fn();

beforeAll(() => {
  Object.defineProperty(document, 'querySelector', {
    writable: true,
    value: () => ({ scrollIntoView }),
  });
});

afterAll(() => {
  Object.defineProperty(document, 'querySelector', {
    writable: true,
    value: originalQuerySelector,
  });
});

beforeEach(() => {
  issuesHandler.reset();
  componentsHandler.reset();
});

it('should scroll to a line directly', async () => {
  renderSourceViewerWrapper();
  await screen.findAllByText('function Test() {}');
  expect(scrollIntoView).toHaveBeenCalled();
});

function renderSourceViewerWrapper(props: Partial<SourceViewerWrapperProps> = {}) {
  return renderComponent(
    <SourceViewerWrapper
      component="foo:index.tsx"
      componentMeasures={[]}
      location={mockLocation({ query: { line: '2' } })}
      {...props}
    />,
  );
}
