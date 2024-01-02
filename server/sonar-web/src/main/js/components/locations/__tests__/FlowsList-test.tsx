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
import { render, screen } from '@testing-library/react';
import React from 'react';
import { FlowLocation, FlowType } from '../../../types/types';
import FlowsList, { Props } from '../FlowsList';

const componentName1 = 'file1';
const componentName2 = 'file2';

const component1 = `project:dir1/dir2/${componentName1}`;
const component2 = `project:dir1/dir2/${componentName2}`;

const mockLocation: FlowLocation = {
  msg: 'location message',
  component: component1,
  textRange: { startLine: 1, startOffset: 2, endLine: 3, endOffset: 4 },
};

it('should display file names for multi-file issues', () => {
  renderComponent({
    flows: [
      {
        locations: [
          { ...mockLocation, component: component1, componentName: componentName1 },
          { ...mockLocation, component: component1, componentName: componentName1 },
          { ...mockLocation, component: component2, componentName: componentName2 },
        ],
        type: FlowType.EXECUTION,
      },
      { locations: [], type: FlowType.DATA },
    ],
    selectedFlowIndex: 1,
  });

  expect(screen.getByText(componentName1)).toBeInTheDocument();
  expect(screen.getByText(componentName2)).toBeInTheDocument();
});

it('should not display file names for single-file issues', () => {
  renderComponent({
    flows: [
      {
        locations: [
          { ...mockLocation, component: component1, componentName: componentName1 },
          { ...mockLocation, component: component1, componentName: componentName1 },
        ],
        type: FlowType.EXECUTION,
      },
      { locations: [], type: FlowType.DATA },
    ],
    selectedFlowIndex: 1,
  });

  expect(screen.queryByText(componentName1)).not.toBeInTheDocument();
});

const renderComponent = (props?: Partial<Props>) =>
  render(<FlowsList flows={[]} onFlowSelect={jest.fn()} onLocationSelect={jest.fn()} {...props} />);
