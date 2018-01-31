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
import App from '../App';
import { searchWebhooks } from '../../../../api/webhooks';
import { Visibility } from '../../../../app/types';

jest.mock('../../../../api/webhooks', () => ({
  searchWebhooks: jest.fn(() => Promise.resolve({ webhooks: [] }))
}));

const organization = { key: 'foo', name: 'Foo', projectVisibility: Visibility.Private };
const component = { key: 'bar', organization: 'foo', qualifier: 'TRK' };

beforeEach(() => {
  (searchWebhooks as jest.Mock<any>).mockClear();
});

it('should be in loading status', () => {
  expect(shallow(<App organization={undefined} />)).toMatchSnapshot();
});

it('should fetch webhooks and display them', async () => {
  const wrapper = shallow(<App organization={organization} />);
  expect(wrapper.state('loading')).toBeTruthy();

  await new Promise(setImmediate);
  expect(searchWebhooks).toHaveBeenCalledWith({ organization: organization.key });

  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

describe('should correctly fetch webhooks when', () => {
  it('on global scope', async () => {
    shallow(<App organization={undefined} />);

    await new Promise(setImmediate);
    expect(searchWebhooks).toHaveBeenCalledWith({ organization: undefined });
  });

  it('on project scope', async () => {
    shallow(<App organization={undefined} component={component} />);

    await new Promise(setImmediate);
    expect(searchWebhooks).toHaveBeenCalledWith({ project: component.key });
  });

  it('on organization scope', async () => {
    shallow(<App organization={organization} component={undefined} />);

    await new Promise(setImmediate);
    expect(searchWebhooks).toHaveBeenCalledWith({ organization: organization.key });
  });

  it('on project scope within an organization', async () => {
    shallow(<App organization={organization} component={component} />);

    await new Promise(setImmediate);
    expect(searchWebhooks).toHaveBeenCalledWith({
      organization: organization.key,
      project: component.key
    });
  });
});
