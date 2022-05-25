/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { getDuplications } from '../../../../api/components';
import { getIssueFlowSnippets } from '../../../../api/issues';
import {
  mockSnippetsByComponent,
  mockSourceLine,
  mockSourceViewerFile
} from '../../../../helpers/mocks/sources';
import { mockFlowLocation, mockIssue } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import CrossComponentSourceViewerWrapper from '../CrossComponentSourceViewerWrapper';

jest.mock('../../../../api/issues', () => {
  const { mockSnippetsByComponent } = jest.requireActual('../../../../helpers/mocks/sources');
  return {
    getIssueFlowSnippets: jest.fn().mockResolvedValue({ 'main.js': mockSnippetsByComponent() })
  };
});

jest.mock('../../../../api/components', () => ({
  getDuplications: jest.fn().mockResolvedValue({}),
  getComponentForSourceViewer: jest.fn().mockResolvedValue({})
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', async () => {
  let wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  wrapper = shallowRender({ issue: mockIssue(true, { component: 'test.js', key: 'unknown' }) });
  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot('no component found');
});

it('Should fetch data', async () => {
  const wrapper = shallowRender();
  wrapper.instance().fetchIssueFlowSnippets();
  await waitAndUpdate(wrapper);
  expect(getIssueFlowSnippets).toHaveBeenCalledWith('1');
  expect(wrapper.state('components')).toEqual(
    expect.objectContaining({ 'main.js': mockSnippetsByComponent() })
  );

  (getIssueFlowSnippets as jest.Mock).mockClear();
  wrapper.setProps({ issue: mockIssue(true, { key: 'foo' }) });
  expect(getIssueFlowSnippets).toBeCalledWith('foo');
});

it('Should handle no access rights', async () => {
  (getIssueFlowSnippets as jest.Mock).mockRejectedValueOnce({ status: 403 });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper.state().notAccessible).toBe(true);
  expect(wrapper).toMatchSnapshot();
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

it('should handle duplication popup', async () => {
  const files = { b: { key: 'b', name: 'B.tsx', project: 'foo', projectName: 'Foo' } };
  const duplications = [{ blocks: [{ _ref: '1', from: 1, size: 2 }] }];
  (getDuplications as jest.Mock).mockResolvedValueOnce({ duplications, files });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.find('ComponentSourceSnippetGroupViewer').prop<Function>('loadDuplications')(
    'foo',
    mockSourceLine()
  );

  await waitAndUpdate(wrapper);
  expect(getDuplications).toHaveBeenCalledWith({ key: 'foo' });
  expect(wrapper.state('duplicatedFiles')).toEqual(files);
  expect(wrapper.state('duplications')).toEqual(duplications);
  expect(wrapper.state('duplicationsByLine')).toEqual({ '1': [0], '2': [0] });

  expect(
    wrapper.find('ComponentSourceSnippetGroupViewer').prop<Function>('renderDuplicationPopup')(
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
      onIssueSelect={jest.fn()}
      onLocationSelect={jest.fn()}
      scroll={jest.fn()}
      selectedFlowIndex={0}
      {...props}
    />
  );
}
