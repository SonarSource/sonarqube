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
import {
  changeProfileParent,
  copyProfile,
  createQualityProfile,
  deleteProfile,
  renameProfile,
  setDefaultProfile,
} from '../../../../api/quality-profiles';
import { mockQualityProfile, mockRouter } from '../../../../helpers/testMocks';
import { click, waitAndUpdate } from '../../../../helpers/testUtils';
import { queryToSearch } from '../../../../helpers/urls';
import { PROFILE_PATH } from '../../constants';
import { ProfileActionModals } from '../../types';
import DeleteProfileForm from '../DeleteProfileForm';
import { ProfileActions } from '../ProfileActions';
import ProfileModalForm from '../ProfileModalForm';

jest.mock('../../../../api/quality-profiles', () => {
  const { mockQualityProfile } = jest.requireActual('../../../../helpers/testMocks');

  return {
    ...jest.requireActual('../../../../api/quality-profiles'),
    copyProfile: jest.fn().mockResolvedValue(null),
    changeProfileParent: jest.fn().mockResolvedValue(null),
    createQualityProfile: jest
      .fn()
      .mockResolvedValue({ profile: mockQualityProfile({ key: 'newProfile' }) }),
    deleteProfile: jest.fn().mockResolvedValue(null),
    setDefaultProfile: jest.fn().mockResolvedValue(null),
    renameProfile: jest.fn().mockResolvedValue(null),
  };
});

const PROFILE = mockQualityProfile({
  activeRuleCount: 68,
  activeDeprecatedRuleCount: 0,
  depth: 0,
  language: 'js',
  rulesUpdatedAt: '2017-06-28T12:58:44+0000',
});

beforeEach(() => jest.clearAllMocks());

it('renders correctly', () => {
  expect(shallowRender()).toMatchSnapshot('no permissions');
  expect(shallowRender({ profile: { ...PROFILE, actions: { edit: true } } })).toMatchSnapshot(
    'edit only'
  );
  expect(
    shallowRender({
      profile: {
        ...PROFILE,
        actions: {
          copy: true,
          edit: true,
          delete: true,
          setAsDefault: true,
          associateProjects: true,
        },
      },
    })
  ).toMatchSnapshot('all permissions');

  expect(shallowRender().setState({ openModal: ProfileActionModals.Copy })).toMatchSnapshot(
    'copy modal'
  );
  expect(shallowRender().setState({ openModal: ProfileActionModals.Extend })).toMatchSnapshot(
    'extend modal'
  );
  expect(shallowRender().setState({ openModal: ProfileActionModals.Rename })).toMatchSnapshot(
    'rename modal'
  );
  expect(shallowRender().setState({ openModal: ProfileActionModals.Delete })).toMatchSnapshot(
    'delete modal'
  );
});

describe('copy a profile', () => {
  it('should correctly copy a profile', async () => {
    const name = 'new-name';
    const updateProfiles = jest.fn().mockResolvedValue(null);
    const push = jest.fn();
    const wrapper = shallowRender({
      profile: { ...PROFILE, actions: { copy: true } },
      router: mockRouter({ push }),
      updateProfiles,
    });

    click(wrapper.find('.it__quality-profiles__copy'));
    expect(wrapper.find(ProfileModalForm).exists()).toBe(true);

    wrapper.find(ProfileModalForm).props().onSubmit(name);
    expect(copyProfile).toHaveBeenCalledWith(PROFILE.key, name);
    await waitAndUpdate(wrapper);

    expect(updateProfiles).toHaveBeenCalled();
    expect(push).toHaveBeenCalledWith({
      pathname: '/profiles/show',
      search: queryToSearch({ name, language: 'js' }),
    });
    expect(wrapper.find(ProfileModalForm).exists()).toBe(false);
  });

  it('should correctly keep the modal open in case of an error', async () => {
    (copyProfile as jest.Mock).mockRejectedValueOnce(null);

    const name = 'new-name';
    const updateProfiles = jest.fn();
    const push = jest.fn();
    const wrapper = shallowRender({
      profile: { ...PROFILE, actions: { copy: true } },
      router: mockRouter({ push }),
      updateProfiles,
    });
    wrapper.setState({ openModal: ProfileActionModals.Copy });

    wrapper.instance().handleProfileCopy(name);
    await waitAndUpdate(wrapper);

    expect(updateProfiles).not.toHaveBeenCalled();
    await waitAndUpdate(wrapper);

    expect(push).not.toHaveBeenCalled();
    expect(wrapper.state().openModal).toBe(ProfileActionModals.Copy);
  });
});

describe('extend a profile', () => {
  it('should correctly extend a profile', async () => {
    const name = 'new-name';
    const profile = { ...PROFILE, actions: { copy: true } };
    const updateProfiles = jest.fn().mockResolvedValue(null);
    const push = jest.fn();
    const wrapper = shallowRender({
      profile,
      router: mockRouter({ push }),
      updateProfiles,
    });

    click(wrapper.find('.it__quality-profiles__extend'));
    expect(wrapper.find(ProfileModalForm).exists()).toBe(true);

    wrapper.find(ProfileModalForm).props().onSubmit(name);
    expect(createQualityProfile).toHaveBeenCalledWith({ language: profile.language, name });
    await waitAndUpdate(wrapper);
    expect(changeProfileParent).toHaveBeenCalledWith(
      expect.objectContaining({
        key: 'newProfile',
      }),
      profile
    );
    await waitAndUpdate(wrapper);

    expect(updateProfiles).toHaveBeenCalled();
    await waitAndUpdate(wrapper);

    expect(push).toHaveBeenCalledWith({
      pathname: '/profiles/show',
      search: queryToSearch({ name, language: 'js' }),
    });
    expect(wrapper.find(ProfileModalForm).exists()).toBe(false);
  });

  it('should correctly keep the modal open in case of an error', async () => {
    (createQualityProfile as jest.Mock).mockRejectedValueOnce(null);

    const name = 'new-name';
    const updateProfiles = jest.fn();
    const push = jest.fn();
    const wrapper = shallowRender({
      profile: { ...PROFILE, actions: { copy: true } },
      router: mockRouter({ push }),
      updateProfiles,
    });
    wrapper.setState({ openModal: ProfileActionModals.Extend });

    wrapper.instance().handleProfileExtend(name);
    await waitAndUpdate(wrapper);

    expect(updateProfiles).not.toHaveBeenCalled();
    expect(changeProfileParent).not.toHaveBeenCalled();
    expect(push).not.toHaveBeenCalled();
    expect(wrapper.state().openModal).toBe(ProfileActionModals.Extend);
  });
});

describe('rename a profile', () => {
  it('should correctly rename a profile', async () => {
    const name = 'new-name';
    const updateProfiles = jest.fn().mockResolvedValue(null);
    const push = jest.fn();
    const wrapper = shallowRender({
      profile: { ...PROFILE, actions: { edit: true } },
      router: mockRouter({ push }),
      updateProfiles,
    });

    click(wrapper.find('.it__quality-profiles__rename'));
    expect(wrapper.find(ProfileModalForm).exists()).toBe(true);

    wrapper.find(ProfileModalForm).props().onSubmit(name);
    expect(renameProfile).toHaveBeenCalledWith(PROFILE.key, name);
    await waitAndUpdate(wrapper);

    expect(updateProfiles).toHaveBeenCalled();
    expect(push).toHaveBeenCalledWith({
      pathname: '/profiles/show',
      search: queryToSearch({ name, language: 'js' }),
    });
    expect(wrapper.find(ProfileModalForm).exists()).toBe(false);
  });

  it('should correctly keep the modal open in case of an error', async () => {
    (renameProfile as jest.Mock).mockRejectedValueOnce(null);

    const name = 'new-name';
    const updateProfiles = jest.fn();
    const push = jest.fn();
    const wrapper = shallowRender({
      profile: { ...PROFILE, actions: { copy: true } },
      router: mockRouter({ push }),
      updateProfiles,
    });
    wrapper.setState({ openModal: ProfileActionModals.Rename });

    wrapper.instance().handleProfileRename(name);
    await waitAndUpdate(wrapper);

    expect(updateProfiles).not.toHaveBeenCalled();
    await waitAndUpdate(wrapper);

    expect(push).not.toHaveBeenCalled();
    expect(wrapper.state().openModal).toBe(ProfileActionModals.Rename);
  });
});

describe('delete a profile', () => {
  it('should correctly delete a profile', async () => {
    const updateProfiles = jest.fn().mockResolvedValue(null);
    const replace = jest.fn();
    const profile = { ...PROFILE, actions: { delete: true } };
    const wrapper = shallowRender({
      profile,
      router: mockRouter({ replace }),
      updateProfiles,
    });

    click(wrapper.find('.it__quality-profiles__delete'));
    expect(wrapper.find(DeleteProfileForm).exists()).toBe(true);

    wrapper.find(DeleteProfileForm).props().onDelete();
    expect(deleteProfile).toHaveBeenCalledWith(profile);
    await waitAndUpdate(wrapper);

    expect(updateProfiles).toHaveBeenCalled();
    expect(replace).toHaveBeenCalledWith(PROFILE_PATH);
    expect(wrapper.find(ProfileModalForm).exists()).toBe(false);
  });

  it('should correctly keep the modal open in case of an error', async () => {
    (deleteProfile as jest.Mock).mockRejectedValueOnce(null);

    const updateProfiles = jest.fn();
    const replace = jest.fn();
    const wrapper = shallowRender({
      profile: { ...PROFILE, actions: { copy: true } },
      router: mockRouter({ replace }),
      updateProfiles,
    });
    wrapper.setState({ openModal: ProfileActionModals.Delete });

    wrapper.instance().handleProfileDelete();
    await waitAndUpdate(wrapper);

    expect(updateProfiles).not.toHaveBeenCalled();
    await waitAndUpdate(wrapper);

    expect(replace).not.toHaveBeenCalled();
    expect(wrapper.state().openModal).toBe(ProfileActionModals.Delete);
  });
});

it('should correctly set a profile as the default', async () => {
  const updateProfiles = jest.fn();

  const wrapper = shallowRender({ updateProfiles });
  wrapper.instance().handleSetDefaultClick();
  await waitAndUpdate(wrapper);

  expect(setDefaultProfile).toHaveBeenCalledWith(PROFILE);
  expect(updateProfiles).toHaveBeenCalled();
});

it('should not allow to set a profile as the default if the profile has no active rules', async () => {
  const profile = mockQualityProfile({
    activeRuleCount: 0,
    actions: {
      setAsDefault: true,
    },
  });

  const wrapper = shallowRender({ profile });
  wrapper.instance().handleSetDefaultClick();
  await waitAndUpdate(wrapper);

  expect(setDefaultProfile).not.toHaveBeenCalled();
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<ProfileActions['props']> = {}) {
  const router = mockRouter();
  return shallow<ProfileActions>(
    <ProfileActions
      isComparable={true}
      profile={PROFILE}
      router={router}
      updateProfiles={jest.fn()}
      {...props}
    />
  );
}
