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
import { BuildTools, OSs } from '../../types';
import { BuildToolForm } from '../BuildToolForm';

it('renders correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ hasCLanguageFeature: false })).toMatchSnapshot('without C');
  expect(shallowRender().setState({ config: { buildTool: BuildTools.Maven } })).toMatchSnapshot(
    'with "maven" selected'
  );
  expect(shallowRender().setState({ config: { buildTool: BuildTools.Other } })).toMatchSnapshot(
    'with "other" selected'
  );
});

it('correctly calls the onDone prop', () => {
  const onDone = jest.fn();
  const wrapper = shallowRender({ onDone });

  wrapper.instance().handleBuildToolChange(BuildTools.Gradle);
  expect(onDone).toHaveBeenCalledWith(expect.objectContaining({ buildTool: BuildTools.Gradle }));

  wrapper.setState({ config: { buildTool: BuildTools.Other } });
  wrapper.instance().handleOSChange(OSs.Windows);
  expect(onDone).toHaveBeenCalledWith(
    expect.objectContaining({ os: OSs.Windows, buildTool: BuildTools.Other })
  );
});

function shallowRender(props: Partial<BuildToolForm['props']> = {}) {
  return shallow<BuildToolForm>(
    <BuildToolForm onDone={jest.fn()} hasCLanguageFeature={true} {...props} />
  );
}
