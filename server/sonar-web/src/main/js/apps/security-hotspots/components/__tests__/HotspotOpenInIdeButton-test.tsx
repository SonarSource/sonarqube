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
import { Button } from '../../../../components/controls/buttons';
import * as sonarlint from '../../../../helpers/sonarlint';
import HotspotOpenInIdeButton from '../HotspotOpenInIdeButton';

jest.mock('../../../../helpers/sonarlint');

describe('HotspotOpenInIdeButton', () => {
  beforeEach(jest.resetAllMocks);

  it('should render correctly', async () => {
    const projectKey = 'my-project:key';
    const hotspotKey = 'AXWsgE9RpggAQesHYfwm';
    const port = 42001;

    const wrapper = shallow(
      <HotspotOpenInIdeButton projectKey={projectKey} hotspotKey={hotspotKey} />
    );
    expect(wrapper).toMatchSnapshot();

    (sonarlint.probeSonarLintServers as jest.Mock).mockResolvedValue([
      { port, ideName: 'BlueJ IDE', description: 'Hello World' },
    ]);
    (sonarlint.openHotspot as jest.Mock).mockResolvedValue(null);

    wrapper.find(Button).simulate('click');

    await new Promise(setImmediate);
    expect(sonarlint.openHotspot).toHaveBeenCalledWith(port, projectKey, hotspotKey);
  });

  it('should gracefully handle zero IDE detected', async () => {
    const wrapper = shallow(<HotspotOpenInIdeButton projectKey="polop" hotspotKey="palap" />);
    (sonarlint.probeSonarLintServers as jest.Mock).mockResolvedValue([]);
    wrapper.find(Button).simulate('click');

    await new Promise(setImmediate);
    expect(sonarlint.openHotspot).not.toHaveBeenCalled();
  });

  it('should handle several IDE', async () => {
    const projectKey = 'my-project:key';
    const hotspotKey = 'AXWsgE9RpggAQesHYfwm';
    const port1 = 42000;
    const port2 = 42001;

    const wrapper = shallow(
      <HotspotOpenInIdeButton projectKey={projectKey} hotspotKey={hotspotKey} />
    );
    expect(wrapper).toMatchSnapshot();

    (sonarlint.probeSonarLintServers as jest.Mock).mockResolvedValue([
      { port: port1, ideName: 'BlueJ IDE', description: 'Hello World' },
      { port: port2, ideName: 'Arduino IDE', description: 'Blink' },
    ]);

    wrapper.find(Button).simulate('click');

    await new Promise(setImmediate);
    expect(wrapper).toMatchSnapshot('dropdown open');
  });
});
