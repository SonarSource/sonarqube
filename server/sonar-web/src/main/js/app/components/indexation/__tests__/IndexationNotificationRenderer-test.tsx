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
import { ButtonLink } from 'sonar-ui-common/components/controls/buttons';
import { click } from 'sonar-ui-common/helpers/testUtils';
import { IndexationProgression } from '../IndexationNotification';
import IndexationNotificationRenderer, {
  IndexationNotificationRendererProps
} from '../IndexationNotificationRenderer';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('in-progress');
  expect(shallowRender({ displayBackgroundTaskLink: true })).toMatchSnapshot('in-progress-admin');
  expect(shallowRender({ progression: IndexationProgression.Completed })).toMatchSnapshot(
    'completed'
  );
});

it('should propagate the dismiss event', () => {
  const onDismissCompletedNotification = jest.fn();
  const wrapper = shallowRender({
    progression: IndexationProgression.Completed,
    onDismissCompletedNotification
  });

  click(wrapper.find(ButtonLink));
  expect(onDismissCompletedNotification).toHaveBeenCalled();
});

function shallowRender(props: Partial<IndexationNotificationRendererProps> = {}) {
  return shallow<IndexationNotificationRendererProps>(
    <IndexationNotificationRenderer
      progression={IndexationProgression.InProgress}
      percentCompleted={25}
      onDismissCompletedNotification={jest.fn()}
      {...props}
    />
  );
}
