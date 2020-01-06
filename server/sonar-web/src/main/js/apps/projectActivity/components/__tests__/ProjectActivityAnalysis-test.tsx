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
/* eslint-disable sonarjs/no-duplicate-string */
import { mount, shallow } from 'enzyme';
import * as React from 'react';
import { IntlProvider } from 'react-intl';
import { scrollToElement } from 'sonar-ui-common/helpers/scrolling';
import { click } from 'sonar-ui-common/helpers/testUtils';
import TimeFormatter from '../../../../components/intl/TimeFormatter';
import { mockAnalysisEvent, mockParsedAnalysis } from '../../../../helpers/testMocks';
import AddEventForm from '../forms/AddEventForm';
import RemoveAnalysisForm from '../forms/RemoveAnalysisForm';
import { ProjectActivityAnalysis, ProjectActivityAnalysisProps } from '../ProjectActivityAnalysis';

jest.mock('sonar-ui-common/helpers/dates', () => ({
  parseDate: () => ({
    valueOf: () => 1546333200000,
    toISOString: () => '2019-01-01T09:00:00.000Z'
  })
}));

jest.mock('sonar-ui-common/helpers/scrolling', () => ({
  scrollToElement: jest.fn()
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(
    shallowRender({ analysis: mockParsedAnalysis({ events: [mockAnalysisEvent()] }) })
  ).toMatchSnapshot('with events');
  expect(
    shallowRender({ analysis: mockParsedAnalysis({ buildString: '1.0.234' }) })
  ).toMatchSnapshot('with build string');
  expect(shallowRender({ isBaseline: true })).toMatchSnapshot('with baseline marker');
  expect(
    shallowRender({
      canAdmin: true,
      canCreateVersion: true,
      canDeleteAnalyses: true
    })
  ).toMatchSnapshot('with admin options');

  const timeFormatter = shallowRender()
    .find(TimeFormatter)
    .prop('children');
  if (!timeFormatter) {
    fail('TimeFormatter instance not found');
  } else {
    expect(timeFormatter('formatted_time')).toMatchSnapshot('formatted time');
  }
});

it('should show the correct admin options', () => {
  const wrapper = shallowRender({
    canAdmin: true,
    canCreateVersion: true,
    canDeleteAnalyses: true
  });

  expect(wrapper.find('.js-add-version').exists()).toBe(true);
  click(wrapper.find('.js-add-version'));
  const addVersionForm = wrapper.find(AddEventForm);
  expect(addVersionForm.exists()).toBe(true);
  addVersionForm.prop('onClose')();
  expect(wrapper.find(AddEventForm).exists()).toBe(false);

  expect(wrapper.find('.js-add-event').exists()).toBe(true);
  click(wrapper.find('.js-add-event'));
  const addEventForm = wrapper.find(AddEventForm);
  expect(addEventForm.exists()).toBe(true);
  addEventForm.prop('onClose')();
  expect(wrapper.find(AddEventForm).exists()).toBe(false);

  expect(wrapper.find('.js-delete-analysis').exists()).toBe(true);
  click(wrapper.find('.js-delete-analysis'));
  const removeAnalysisForm = wrapper.find(RemoveAnalysisForm);
  expect(removeAnalysisForm.exists()).toBe(true);
  removeAnalysisForm.prop('onClose')();
  expect(wrapper.find(RemoveAnalysisForm).exists()).toBe(false);
});

it('should not allow the first item to be deleted', () => {
  expect(
    shallowRender({
      canAdmin: true,
      canCreateVersion: true,
      canDeleteAnalyses: true,
      isFirst: true
    })
      .find('.js-delete-analysis')
      .exists()
  ).toBe(false);
});

it('should be clickable', () => {
  const date = new Date('2018-03-01T09:37:01+0100');
  const updateSelectedDate = jest.fn();
  const wrapper = shallowRender({ analysis: mockParsedAnalysis({ date }), updateSelectedDate });
  click(wrapper);
  expect(updateSelectedDate).toBeCalledWith(date);
});

it('should trigger a scroll to itself if selected', () => {
  mountRender({ parentScrollContainer: document.createElement('ul'), selected: true });
  expect(scrollToElement).toBeCalled();
});

function shallowRender(props: Partial<ProjectActivityAnalysisProps> = {}) {
  return shallow(createComponent(props));
}

function mountRender(props: Partial<ProjectActivityAnalysisProps> = {}) {
  return mount(<IntlProvider locale="en">{createComponent(props)}</IntlProvider>);
}

function createComponent(props: Partial<ProjectActivityAnalysisProps> = {}) {
  return (
    <ProjectActivityAnalysis
      addCustomEvent={jest.fn()}
      addVersion={jest.fn()}
      analysis={mockParsedAnalysis()}
      canCreateVersion={false}
      changeEvent={jest.fn()}
      deleteAnalysis={jest.fn()}
      deleteEvent={jest.fn()}
      isBaseline={false}
      isFirst={false}
      selected={false}
      updateSelectedDate={jest.fn()}
      {...props}
    />
  );
}
