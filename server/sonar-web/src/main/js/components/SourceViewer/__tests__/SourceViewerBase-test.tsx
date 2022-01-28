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
import { getComponentData, getComponentForSourceViewer, getSources } from '../../../api/components';
import { mockMainBranch } from '../../../helpers/mocks/branch-like';
import { mockIssue, mockSourceLine, mockSourceViewerFile } from '../../../helpers/testMocks';
import { waitAndUpdate } from '../../../helpers/testUtils';
import defaultLoadIssues from '../helpers/loadIssues';
import SourceViewerBase from '../SourceViewerBase';

jest.mock('../helpers/loadIssues', () => jest.fn().mockRejectedValue({}));

jest.mock('../../../api/components', () => ({
  getComponentForSourceViewer: jest.fn().mockRejectedValue(''),
  getComponentData: jest.fn().mockRejectedValue(''),
  getSources: jest.fn().mockRejectedValue('')
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
    component: { leakPeriodDate: '2018-06-20T17:12:19+0200' }
  });
  (getSources as jest.Mock).mockResolvedValueOnce([]);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(wrapper).toMatchSnapshot();
});

it('should use load props if provided', () => {
  const loadComponent = jest.fn().mockResolvedValue({});
  const loadIssues = jest.fn().mockResolvedValue([]);
  const loadSources = jest.fn().mockResolvedValue([]);
  const wrapper = shallowRender({
    loadComponent,
    loadIssues,
    loadSources
  });

  expect(wrapper.instance().loadComponent).toBe(loadComponent);
});

it('should reload', async () => {
  (defaultLoadIssues as jest.Mock)
    .mockResolvedValueOnce([mockIssue()])
    .mockResolvedValueOnce([mockIssue()]);
  (getComponentForSourceViewer as jest.Mock).mockResolvedValueOnce(mockSourceViewerFile());
  (getComponentData as jest.Mock).mockResolvedValueOnce({
    component: { leakPeriodDate: '2018-06-20T17:12:19+0200' }
  });
  (getSources as jest.Mock).mockResolvedValueOnce([mockSourceLine()]);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().reloadIssues();

  expect(defaultLoadIssues).toBeCalledTimes(2);

  await waitAndUpdate(wrapper);

  expect(wrapper.state().issues).toHaveLength(1);
});

it('should load sources before', async () => {
  (defaultLoadIssues as jest.Mock)
    .mockResolvedValueOnce([mockIssue(false, { key: 'issue1' })])
    .mockResolvedValueOnce([mockIssue(false, { key: 'issue2' })]);
  (getComponentForSourceViewer as jest.Mock).mockResolvedValueOnce(mockSourceViewerFile());
  (getComponentData as jest.Mock).mockResolvedValueOnce({
    component: { leakPeriodDate: '2018-06-20T17:12:19+0200' }
  });
  (getSources as jest.Mock)
    .mockResolvedValueOnce([mockSourceLine()])
    .mockResolvedValueOnce([mockSourceLine()]);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().loadSourcesBefore();
  expect(wrapper.state().loadingSourcesBefore).toBe(true);

  expect(defaultLoadIssues).toBeCalledTimes(2);
  expect(getSources).toBeCalledTimes(2);

  await waitAndUpdate(wrapper);
  expect(wrapper.state().loadingSourcesBefore).toBe(false);
  expect(wrapper.state().issues).toHaveLength(2);
});

it('should load sources after', async () => {
  (defaultLoadIssues as jest.Mock)
    .mockResolvedValueOnce([mockIssue(false, { key: 'issue1' })])
    .mockResolvedValueOnce([mockIssue(false, { key: 'issue2' })]);
  (getComponentForSourceViewer as jest.Mock).mockResolvedValueOnce(mockSourceViewerFile());
  (getComponentData as jest.Mock).mockResolvedValueOnce({
    component: { leakPeriodDate: '2018-06-20T17:12:19+0200' }
  });
  (getSources as jest.Mock)
    .mockResolvedValueOnce([mockSourceLine()])
    .mockResolvedValueOnce([mockSourceLine()]);

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().loadSourcesAfter();
  expect(wrapper.state().loadingSourcesAfter).toBe(true);

  expect(defaultLoadIssues).toBeCalledTimes(2);
  expect(getSources).toBeCalledTimes(2);

  await waitAndUpdate(wrapper);

  expect(wrapper.state().loadingSourcesAfter).toBe(false);
  expect(wrapper.state().issues).toHaveLength(2);
});

it('should handle no sources when checking ranges', () => {
  const wrapper = shallowRender();

  wrapper.setState({ sources: undefined });
  expect(wrapper.instance().isLineOutsideOfRange(12)).toBe(true);
});

function shallowRender(overrides: Partial<SourceViewerBase['props']> = {}) {
  return shallow<SourceViewerBase>(
    <SourceViewerBase branchLike={mockMainBranch()} component="my-component" {...overrides} />
  );
}
