/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import { shallow } from 'enzyme';
import RemoteRepositories from '../RemoteRepositories';
import { getRepositories, provisionProject } from '../../../../api/alm-integration';
import { waitAndUpdate, submit } from '../../../../helpers/testUtils';

jest.mock('../../../../api/alm-integration', () => ({
  getRepositories: jest.fn().mockResolvedValue({
    repositories: [
      {
        label: 'Cool Project',
        installationKey: 'github/cool',
        linkedProjectKey: 'proj_cool',
        linkedProjectName: 'Proj Cool'
      },
      {
        label: 'Awesome Project',
        installationKey: 'github/awesome'
      }
    ]
  }),
  provisionProject: jest.fn().mockResolvedValue({ projects: [{ projectKey: 'awesome' }] })
}));

const almApplication = {
  backgroundColor: 'blue',
  iconPath: 'icon/path',
  installationUrl: 'https://alm.installation.url',
  key: 'github',
  name: 'GitHub'
};

beforeEach(() => {
  (getRepositories as jest.Mock<any>).mockClear();
  (provisionProject as jest.Mock<any>).mockClear();
});

it('should display the list of repositories', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  await waitAndUpdate(wrapper);
  expect(getRepositories).toHaveBeenCalledWith({ organization: 'sonarsource' });
  expect(wrapper).toMatchSnapshot();
});

it('should correctly create a project', async () => {
  const onProjectCreate = jest.fn();
  const wrapper = shallowRender({ onProjectCreate });
  (wrapper.instance() as RemoteRepositories).toggleRepository({
    label: 'Awesome Project',
    installationKey: 'github/awesome'
  });
  await waitAndUpdate(wrapper);

  expect(wrapper.find('SubmitButton')).toMatchSnapshot();
  submit(wrapper.find('form'));
  expect(provisionProject).toBeCalledWith({
    installationKeys: ['github/awesome'],
    organization: 'sonarsource'
  });

  await waitAndUpdate(wrapper);
  expect(onProjectCreate).toBeCalledWith(['awesome'], 'sonarsource');
});

function shallowRender(props: Partial<RemoteRepositories['props']> = {}) {
  return shallow(
    <RemoteRepositories
      almApplication={almApplication}
      onProjectCreate={jest.fn()}
      organization="sonarsource"
      {...props}
    />
  );
}
