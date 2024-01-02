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
import { shallow } from 'enzyme';
import * as React from 'react';
import { getComponentData, getComponentForSourceViewer, getSources } from '../../../api/components';
import { mockMainBranch } from '../../../helpers/mocks/branch-like';
import { mockSourceLine, mockSourceViewerFile } from '../../../helpers/mocks/sources';
import { mockIssue } from '../../../helpers/testMocks';
import { waitAndUpdate } from '../../../helpers/testUtils';
import defaultLoadIssues from '../helpers/loadIssues';
import SourceViewer from '../SourceViewer';

jest.mock('../helpers/loadIssues', () => jest.fn().mockRejectedValue({}));

jest.mock('../../../api/components', () => ({
  getComponentForSourceViewer: jest.fn().mockRejectedValue(''),
  getComponentData: jest.fn().mockRejectedValue(''),
  getSources: jest.fn().mockRejectedValue(''),
}));

beforeEach(() => {
  jest.resetAllMocks();
});

it('should render nothing from the start', () => {
  expect(shallowRender().type()).toBeNull();
});

it('should render correctly', async () => {
  (defaultLoadIssues as jest.Mock).mockResolvedValueOnce([mockIssue()]);
  (getComponentForSourceViewer as jest.Mock).mockResolvedValueOnce(mockSourceViewerFile());
  (getComponentData as jest.Mock).mockResolvedValueOnce({
    component: { leakPeriodDate: '2018-06-20T17:12:19+0200' },
  });
  (getSources as jest.Mock).mockResolvedValueOnce([]);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot();
});

it('should load sources before', async () => {
  (defaultLoadIssues as jest.Mock).mockResolvedValueOnce([
    mockIssue(false, { key: 'issue1' }),
    mockIssue(false, { key: 'issue2' }),
  ]);
  (getComponentForSourceViewer as jest.Mock).mockResolvedValueOnce(mockSourceViewerFile());
  (getComponentData as jest.Mock).mockResolvedValueOnce({
    component: { leakPeriodDate: '2018-06-20T17:12:19+0200' },
  });
  (getSources as jest.Mock)
    .mockResolvedValueOnce([mockSourceLine()])
    .mockResolvedValueOnce([mockSourceLine()]);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().loadSourcesBefore();
  expect(wrapper.state().loadingSourcesBefore).toBe(true);

  expect(defaultLoadIssues).toHaveBeenCalledTimes(1);
  expect(getSources).toHaveBeenCalledTimes(2);

  await waitAndUpdate(wrapper);
  expect(wrapper.state().loadingSourcesBefore).toBe(false);
  expect(wrapper.state().issues).toHaveLength(2);
});

it('should load sources after', async () => {
  (defaultLoadIssues as jest.Mock).mockResolvedValueOnce([
    mockIssue(false, { key: 'issue1' }),
    mockIssue(false, { key: 'issue2' }),
  ]);
  (getComponentForSourceViewer as jest.Mock).mockResolvedValueOnce(mockSourceViewerFile());
  (getComponentData as jest.Mock).mockResolvedValueOnce({
    component: { leakPeriodDate: '2018-06-20T17:12:19+0200' },
  });
  (getSources as jest.Mock)
    .mockResolvedValueOnce([mockSourceLine()])
    .mockResolvedValueOnce([mockSourceLine()]);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().loadSourcesAfter();
  expect(wrapper.state().loadingSourcesAfter).toBe(true);

  expect(defaultLoadIssues).toHaveBeenCalledTimes(1);
  expect(getSources).toHaveBeenCalledTimes(2);

  await waitAndUpdate(wrapper);

  expect(wrapper.state().loadingSourcesAfter).toBe(false);
  expect(wrapper.state().issues).toHaveLength(2);
});

it('should handle no sources when checking ranges', () => {
  const wrapper = shallowRender();

  wrapper.setState({ sources: undefined });
  expect(wrapper.instance().isLineOutsideOfRange(12)).toBe(true);
});

function shallowRender(overrides: Partial<SourceViewer['props']> = {}) {
  return shallow<SourceViewer>(
    <SourceViewer branchLike={mockMainBranch()} component="my-component" {...overrides} />
  );
}
