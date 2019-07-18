/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getDuplications } from '../../../../api/components';
import { getIssueFlowSnippets } from '../../../../api/issues';
import {
  mockFlowLocation,
  mockIssue,
  mockSnippetsByComponent,
  mockSourceLine,
  mockSourceViewerFile
} from '../../../../helpers/testMocks';
import CrossComponentSourceViewerWrapper from '../CrossComponentSourceViewerWrapper';

jest.mock('../../../../api/issues', () => {
  const { mockSnippetsByComponent } = require.requireActual('../../../../helpers/testMocks');
  return {
    getIssueFlowSnippets: jest.fn().mockResolvedValue({ 'main.js': mockSnippetsByComponent() })
  };
});

jest.mock('../../../../api/components', () => ({
  getDuplications: jest.fn().mockResolvedValue({})
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('Should fetch data', async () => {
  const wrapper = shallowRender();
  wrapper.instance().fetchIssueFlowSnippets('124');
  await waitAndUpdate(wrapper);
  expect(getIssueFlowSnippets).toHaveBeenCalledWith('1');
  expect(wrapper.state('components')).toEqual({ 'main.js': mockSnippetsByComponent() });

  (getIssueFlowSnippets as jest.Mock).mockClear();
  wrapper.setProps({ issue: mockIssue(true, { key: 'foo' }) });
  expect(getIssueFlowSnippets).toBeCalledWith('foo');
});

it('should handle issue popup', () => {
  const wrapper = shallowRender();
  // open
  wrapper.instance().handleIssuePopupToggle('1', 'popup1');
  expect(wrapper.state('issuePopup')).toEqual({ issue: '1', name: 'popup1' });

  // close
  wrapper.instance().handleIssuePopupToggle('1', 'popup1');
  expect(wrapper.state('issuePopup')).toBeUndefined();
});

it('should handle line popup', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  const linePopup = { component: 'foo', index: 0, line: 16, name: 'b.tsx' };
  wrapper.find('ComponentSourceSnippetViewer').prop<Function>('onLinePopupToggle')(linePopup);
  expect(wrapper.state('linePopup')).toEqual(linePopup);

  wrapper.find('ComponentSourceSnippetViewer').prop<Function>('onLinePopupToggle')(linePopup);
  expect(wrapper.state('linePopup')).toEqual(undefined);

  const openLinePopup = { ...linePopup, open: true };
  wrapper.find('ComponentSourceSnippetViewer').prop<Function>('onLinePopupToggle')(openLinePopup);
  wrapper.find('ComponentSourceSnippetViewer').prop<Function>('onLinePopupToggle')(openLinePopup);
  expect(wrapper.state('linePopup')).toEqual(linePopup);
});

it('should handle duplication popup', async () => {
  const files = { b: { key: 'b', name: 'B.tsx', project: 'foo', projectName: 'Foo' } };
  const duplications = [{ blocks: [{ _ref: '1', from: 1, size: 2 }] }];
  (getDuplications as jest.Mock).mockResolvedValueOnce({ duplications, files });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.find('ComponentSourceSnippetViewer').prop<Function>('loadDuplications')(
    'foo',
    mockSourceLine()
  );

  await waitAndUpdate(wrapper);
  expect(getDuplications).toHaveBeenCalledWith({ key: 'foo' });
  expect(wrapper.state('duplicatedFiles')).toEqual(files);
  expect(wrapper.state('duplications')).toEqual(duplications);
  expect(wrapper.state('duplicationsByLine')).toEqual({ '1': [0], '2': [0] });
  expect(wrapper.state('linePopup')).toEqual({
    component: 'foo',
    index: 0,
    line: 16,
    name: 'duplications'
  });

  expect(
    wrapper.find('ComponentSourceSnippetViewer').prop<Function>('renderDuplicationPopup')(
      mockSourceViewerFile(),
      0,
      16
    )
  ).toMatchSnapshot();
});

function shallowRender(props: Partial<CrossComponentSourceViewerWrapper['props']> = {}) {
  return shallow<CrossComponentSourceViewerWrapper>(
    <CrossComponentSourceViewerWrapper
      branchLike={undefined}
      highlightedLocationMessage={undefined}
      issue={mockIssue(true, { key: '1' })}
      issues={[]}
      locations={[mockFlowLocation()]}
      onIssueChange={jest.fn()}
      onLoaded={jest.fn()}
      onLocationSelect={jest.fn()}
      scroll={jest.fn()}
      selectedFlowIndex={0}
      {...props}
    />
  );
}
