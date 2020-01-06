/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import {
  associateProject,
  dissociateProject,
  searchQualityProfiles
} from '../../../api/quality-profiles';
import handleRequiredAuthorization from '../../../app/utils/handleRequiredAuthorization';
import { mockComponent, mockQualityProfile } from '../../../helpers/testMocks';
import App from '../App';
import Table from '../Table';

beforeEach(() => jest.clearAllMocks());

jest.mock('../../../api/quality-profiles', () => ({
  associateProject: jest.fn().mockResolvedValue({}),
  dissociateProject: jest.fn().mockResolvedValue({}),
  searchQualityProfiles: jest.fn().mockResolvedValue({})
}));

jest.mock('../../../app/utils/addGlobalSuccessMessage', () => ({
  default: jest.fn()
}));

jest.mock('../../../app/utils/handleRequiredAuthorization', () => ({
  default: jest.fn()
}));

const component = mockComponent({ configuration: { showQualityProfiles: true } });

it('checks permissions', () => {
  shallowRender({ component: { ...component, configuration: undefined } });
  expect(handleRequiredAuthorization).toBeCalled();
});

it('fetches profiles', () => {
  shallowRender();
  expect(searchQualityProfiles).toHaveBeenCalledTimes(2);
  expect(searchQualityProfiles).toBeCalledWith({ organization: component.organization });
  expect(searchQualityProfiles).toBeCalledWith({
    organization: component.organization,
    project: component.key
  });
});

it('changes profile', () => {
  const wrapper = shallowRender();

  const fooJava = mockQualityProfile({ key: 'foo-java', language: 'java' });
  const fooJs = mockQualityProfile({ key: 'foo-js', language: 'js' });
  const bar = mockQualityProfile({ key: 'bar-java', language: 'java' });
  const baz = mockQualityProfile({ key: 'baz-java', language: 'java', isDefault: true });
  const allProfiles = [fooJava, bar, baz, fooJs];
  const profiles = [fooJava, fooJs];
  wrapper.setState({ allProfiles, loading: false, profiles });

  wrapper
    .find(Table)
    .props()
    .onChangeProfile(fooJava.key, bar.key);
  expect(associateProject).toBeCalledWith(bar, component.key);

  wrapper
    .find(Table)
    .props()
    .onChangeProfile(fooJava.key, baz.key);
  expect(dissociateProject).toBeCalledWith(fooJava, component.key);
});

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow<App>(<App component={component} {...props} />);
}
