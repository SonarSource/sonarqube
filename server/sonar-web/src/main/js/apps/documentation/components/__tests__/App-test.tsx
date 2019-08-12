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
import { addSideBarClass, removeSideBarClass } from 'sonar-ui-common/helpers/pages';
import { request } from 'sonar-ui-common/helpers/request';
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { isSonarCloud } from '../../../../helpers/system';
import getPages from '../../pages';
import App from '../App';

jest.mock('../../../../components/common/ScreenPositionHelper', () => ({
  default: class ScreenPositionHelper extends React.Component<{
    children: (pos: { top: number }) => React.ReactNode;
  }> {
    static displayName = 'ScreenPositionHelper';
    render() {
      return this.props.children({ top: 0 });
    }
  }
}));

jest.mock('../../../../helpers/system', () => ({
  isSonarCloud: jest.fn().mockReturnValue(false)
}));

jest.mock('Docs/../static/SonarQubeNavigationTree.json', () => ({
  default: [
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
  ]
}));

jest.mock('Docs/../static/SonarCloudNavigationTree.json', () => ({
  default: [
    {
      title: 'SonarCloud',
      children: [
        '/lorem/ipsum/',
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
  ]
}));

jest.mock('sonar-ui-common/helpers/pages', () => ({
  addSideBarClass: jest.fn(),
  removeSideBarClass: jest.fn()
}));

jest.mock('sonar-ui-common/helpers/request', () => {
  const { mockDocumentationMarkdown } = require.requireActual('../../../../helpers/testMocks');
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
  const { mockDocumentationEntry } = require.requireActual('../../../../helpers/testMocks');
  return {
    default: jest
      .fn()
      .mockReturnValue([
        mockDocumentationEntry(),
        mockDocumentationEntry({ url: '/analysis/languages/csharp/' })
      ])
  };
});

jest.mock('../../../../api/plugins', () => ({
  getInstalledPlugins: jest
    .fn()
    .mockResolvedValue([
      { key: 'csharp', documentationPath: 'static/documentation.md' },
      { key: 'vbnet', documentationPath: 'Sstatic/documentation.md' },
      { key: 'vbnett', documentationPath: undefined }
    ])
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

it('should render correctly for SonarCloud', async () => {
  (isSonarCloud as jest.Mock).mockReturnValue(true);
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it("should show a 404 if the page doesn't exist", async () => {
  const wrapper = shallowRender({ params: { splat: 'unknown' } });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should try to fetch language plugin documentation if documentationPath matches', async () => {
  (isSonarCloud as jest.Mock).mockReturnValue(false);

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

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow(<App params={{ splat: 'lorem/ipsum' }} {...props} />);
}
