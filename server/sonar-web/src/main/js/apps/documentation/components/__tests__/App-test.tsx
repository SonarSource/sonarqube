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
import { addSideBarClass, removeSideBarClass } from '../../../../helpers/pages';
import { request } from '../../../../helpers/request';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { InstalledPlugin } from '../../../../types/plugins';
import getPages from '../../pages';
import App from '../App';

jest.mock('../../../../components/common/ScreenPositionHelper');

jest.mock('Docs/../static/SonarQubeNavigationTree.json', () => [
  {
    title: 'SonarQube',
    children: [
      '/lorem/ipsum/',
      '/analysis/languages/csharp/',
      {
        title: 'Child category',
        children: [
          '/lorem/ipsum/dolor',
          {
            title: 'Grandchild category',
            children: ['/lorem/ipsum/sit']
          },
          '/lorem/ipsum/amet'
        ]
      }
    ]
  }
]);

jest.mock('../../../../helpers/pages', () => ({
  addSideBarClass: jest.fn(),
  removeSideBarClass: jest.fn()
}));

jest.mock('../../../../helpers/request', () => {
  const { mockDocumentationMarkdown } = jest.requireActual('../../../../helpers/testMocks');
  return {
    request: jest.fn(() => ({
      submit: jest.fn().mockResolvedValue({
        status: 200,
        text: jest.fn().mockResolvedValue(mockDocumentationMarkdown({ key: 'csharp' }))
      })
    }))
  };
});

jest.mock('../../pages', () => {
  const { mockDocumentationEntry } = jest.requireActual('../../../../helpers/testMocks');
  return jest
    .fn()
    .mockReturnValue([
      mockDocumentationEntry(),
      mockDocumentationEntry({ url: '/analysis/languages/csharp/' })
    ]);
});

jest.mock('../../../../api/plugins', () => ({
  getInstalledPlugins: jest.fn().mockResolvedValue([
    {
      key: 'csharp',
      documentationPath: 'static/documentation.md',
      issueTrackerUrl: 'csharp_plugin_issue_tracker_url'
    },
    { key: 'vbnet', documentationPath: 'Sstatic/documentation.md' },
    { key: 'vbnett', documentationPath: undefined }
  ] as InstalledPlugin[])
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly for SonarQube', async () => {
  const wrapper = shallowRender();
  expect(wrapper.find('DeferredSpinner').exists()).toBe(true);
  expect(addSideBarClass).toBeCalled();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('ScreenPositionHelper').dive()).toMatchSnapshot();

  wrapper.unmount();
  expect(removeSideBarClass).toBeCalled();
});

it("should show a 404 if the page doesn't exist", async () => {
  const wrapper = shallowRender({ params: { splat: 'unknown' } });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should try to fetch language plugin documentation if documentationPath matches', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  expect(request).toHaveBeenCalledWith('/static/csharp/documentation.md');
  expect(request).not.toHaveBeenCalledWith('/static/vbnet/documentation.md');
  expect(request).not.toHaveBeenCalledWith('/static/vbnett/documentation.md');
  expect(getPages).toHaveBeenCalledWith(
    expect.objectContaining({
      'analysis/languages/csharp': expect.any(Object)
    })
  );
});

it('should display the issue tracker url of the plugin if it exists', async () => {
  const wrapper = shallowRender({ params: { splat: 'analysis/languages/csharp/' } });
  await waitAndUpdate(wrapper);

  const { content } = (getPages as jest.Mock).mock.calls[0][0]['analysis/languages/csharp'];

  expect(content).toContain('csharp_plugin_issue_tracker_url');
});

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow(<App params={{ splat: 'lorem/ipsum' }} location={{ hash: '#foo' }} {...props} />);
}
