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
import { submit, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { bulkActivateRules, bulkDeactivateRules } from '../../../../api/quality-profiles';
import { mockLanguage, mockQualityProfile } from '../../../../helpers/testMocks';
import { Query } from '../../query';
import BulkChangeModal from '../BulkChangeModal';

jest.mock('../../../../api/quality-profiles', () => ({
  bulkActivateRules: jest.fn().mockResolvedValue({ failed: 0, succeeded: 2 }),
  bulkDeactivateRules: jest.fn().mockResolvedValue({ failed: 2, succeeded: 0 })
}));

beforeEach(jest.clearAllMocks);

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ profile: undefined })).toMatchSnapshot('no profile pre-selected');
  expect(shallowRender({ action: 'deactivate' })).toMatchSnapshot('deactivate action');
  expect(
    shallowRender().setState({
      results: [
        { failed: 2, profile: 'foo', succeeded: 0 },
        { failed: 0, profile: 'bar', succeeded: 2 }
      ]
    })
  ).toMatchSnapshot('results');
  expect(shallowRender().setState({ submitting: true })).toMatchSnapshot('submitting');
  expect(shallowRender().setState({ finished: true })).toMatchSnapshot('finished');
});

it('should pre-select a profile if only 1 is available', () => {
  const profile = mockQualityProfile({
    actions: { edit: true },
    isBuiltIn: false,
    key: 'foo',
    language: 'js'
  });
  const wrapper = shallowRender({ profile: undefined, referencedProfiles: { foo: profile } });
  expect(wrapper.state().selectedProfiles).toEqual(['foo']);
});

it('should handle profile selection', () => {
  const wrapper = shallowRender();
  wrapper.instance().handleProfileSelect([{ value: 'foo' }, { value: 'bar' }]);
  expect(wrapper.state().selectedProfiles).toEqual(['foo', 'bar']);
});

it('should handle form submission', async () => {
  const wrapper = shallowRender({ profile: undefined });
  wrapper.setState({ selectedProfiles: ['foo', 'bar'] });

  // Activate.
  submit(wrapper.find('form'));
  await waitAndUpdate(wrapper);
  expect(bulkActivateRules).toBeCalledWith(expect.objectContaining({ targetKey: 'foo' }));

  await waitAndUpdate(wrapper);
  expect(bulkActivateRules).toBeCalledWith(expect.objectContaining({ targetKey: 'bar' }));

  await waitAndUpdate(wrapper);
  expect(wrapper.state().results).toEqual([
    { failed: 0, profile: 'foo', succeeded: 2 },
    { failed: 0, profile: 'bar', succeeded: 2 }
  ]);

  // Deactivate.
  wrapper.setProps({ action: 'deactivate' }).setState({ results: [] });
  submit(wrapper.find('form'));
  await waitAndUpdate(wrapper);
  expect(bulkDeactivateRules).toBeCalledWith(expect.objectContaining({ targetKey: 'foo' }));

  await waitAndUpdate(wrapper);
  expect(bulkDeactivateRules).toBeCalledWith(expect.objectContaining({ targetKey: 'bar' }));

  await waitAndUpdate(wrapper);
  expect(wrapper.state().results).toEqual([
    { failed: 2, profile: 'foo', succeeded: 0 },
    { failed: 2, profile: 'bar', succeeded: 0 }
  ]);
});

function shallowRender(props: Partial<BulkChangeModal['props']> = {}) {
  return shallow<BulkChangeModal>(
    <BulkChangeModal
      action="activate"
      languages={{ js: mockLanguage() }}
      onClose={jest.fn()}
      organization={undefined}
      profile={mockQualityProfile()}
      query={{ languages: ['js'] } as Query}
      referencedProfiles={{
        foo: mockQualityProfile({ key: 'foo' }),
        bar: mockQualityProfile({ key: 'bar' })
      }}
      total={42}
      {...props}
    />
  );
}
