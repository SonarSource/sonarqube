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
import { mockEvent, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import {
  assignSecurityHotspot,
  commentSecurityHotspot,
  setSecurityHotspotStatus
} from '../../../../api/security-hotspots';
import { mockHotspot } from '../../../../helpers/mocks/security-hotspots';
import { mockLoggedInUser } from '../../../../helpers/testMocks';
import {
  HotspotResolution,
  HotspotStatus,
  HotspotStatusOption
} from '../../../../types/security-hotspots';
import HotspotActionsForm from '../HotspotActionsForm';

jest.mock('../../../../api/security-hotspots', () => ({
  assignSecurityHotspot: jest.fn().mockResolvedValue(undefined),
  commentSecurityHotspot: jest.fn().mockResolvedValue(undefined),
  setSecurityHotspotStatus: jest.fn().mockResolvedValue(undefined)
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should handle option selection', () => {
  const wrapper = shallowRender();
  expect(wrapper.state().selectedOption).toBe(HotspotStatusOption.FIXED);
  wrapper.instance().handleSelectOption(HotspotStatusOption.SAFE);
  expect(wrapper.state().selectedOption).toBe(HotspotStatusOption.SAFE);
});

it('should handle comment change', () => {
  const wrapper = shallowRender();
  wrapper.instance().handleCommentChange('new comment');
  expect(wrapper.state().comment).toBe('new comment');
});

describe('submit', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should be handled for additional review', async () => {
    const onSubmit = jest.fn();
    const wrapper = shallowRender({ onSubmit });
    wrapper.setState({ selectedOption: HotspotStatusOption.ADDITIONAL_REVIEW });

    const promise = wrapper.instance().handleSubmit(mockEvent());

    expect(wrapper.state().submitting).toBe(true);
    await promise;
    expect(setSecurityHotspotStatus).toBeCalledWith('key', {
      status: HotspotStatus.TO_REVIEW
    });
    expect(onSubmit).toBeCalled();
  });

  it('should be handled for SAFE', async () => {
    const wrapper = shallowRender();
    wrapper.setState({ comment: 'commentsafe', selectedOption: HotspotStatusOption.SAFE });
    await wrapper.instance().handleSubmit(mockEvent());
    expect(setSecurityHotspotStatus).toBeCalledWith('key', {
      status: HotspotStatus.REVIEWED,
      resolution: HotspotResolution.SAFE
    });
    expect(commentSecurityHotspot).toBeCalledWith('key', 'commentsafe');
  });

  it('should be handled for FIXED', async () => {
    const wrapper = shallowRender({
      hotspot: mockHotspot({ key: 'key', status: HotspotStatus.TO_REVIEW })
    });
    wrapper.setState({ comment: 'commentfixed', selectedOption: HotspotStatusOption.FIXED });
    await wrapper.instance().handleSubmit(mockEvent());
    expect(setSecurityHotspotStatus).toBeCalledWith('key', {
      status: HotspotStatus.REVIEWED,
      resolution: HotspotResolution.FIXED
    });
    expect(commentSecurityHotspot).toBeCalledWith('key', 'commentfixed');
  });

  it('should ignore no change', async () => {
    const wrapper = shallowRender();
    wrapper.setState({ selectedOption: HotspotStatusOption.FIXED });
    await wrapper.instance().handleSubmit(mockEvent());
    expect(setSecurityHotspotStatus).not.toBeCalled();
  });
});

it('should handle assignment', async () => {
  const onSubmit = jest.fn();
  const wrapper = shallowRender({ onSubmit });
  wrapper.setState({
    comment: 'assignment comment',
    selectedOption: HotspotStatusOption.ADDITIONAL_REVIEW
  });

  wrapper.instance().handleAssign(mockLoggedInUser({ login: 'userLogin' }));
  await waitAndUpdate(wrapper);

  const promise = wrapper.instance().handleSubmit({ preventDefault: jest.fn() } as any);

  expect(wrapper.state().submitting).toBe(true);
  await promise;

  expect(setSecurityHotspotStatus).toBeCalledWith('key', {
    status: HotspotStatus.TO_REVIEW
  });
  expect(assignSecurityHotspot).toBeCalledWith('key', {
    assignee: 'userLogin'
  });
  expect(commentSecurityHotspot).toBeCalledWith('key', 'assignment comment');
  expect(onSubmit).toBeCalled();
});

it('should handle submit failure', async () => {
  const onSubmit = jest.fn();
  (setSecurityHotspotStatus as jest.Mock).mockRejectedValueOnce('failure');
  const wrapper = shallowRender({ onSubmit });
  wrapper.setState({ selectedOption: HotspotStatusOption.ADDITIONAL_REVIEW });
  const promise = wrapper.instance().handleSubmit({ preventDefault: jest.fn() } as any);
  expect(wrapper.state().submitting).toBe(true);
  await promise;
  await waitAndUpdate(wrapper);
  expect(wrapper.state().submitting).toBe(false);
  expect(onSubmit).not.toBeCalled();
});

function shallowRender(props: Partial<HotspotActionsForm['props']> = {}) {
  return shallow<HotspotActionsForm>(
    <HotspotActionsForm hotspot={mockHotspot({ key: 'key' })} onSubmit={jest.fn()} {...props} />
  );
}
