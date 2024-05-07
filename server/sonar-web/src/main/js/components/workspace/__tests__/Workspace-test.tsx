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
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { mockBranch } from '../../../helpers/mocks/branch-like';
import { get, save } from '../../../helpers/storage';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { BranchLike } from '../../../types/branch-like';
import Workspace, { TYPE_KEY, WorkspaceTypes } from '../Workspace';
import { WorkspaceContext } from '../context';

jest.mock('../../../helpers/storage', () => {
  return {
    get: jest.fn(() => {
      throw Error('no local storage');
    }),
    save: jest.fn(),
  };
});

jest.mock('../../../api/rules', () => ({
  getRulesApp: jest.fn().mockResolvedValue({
    repositories: [{ key: 'repo', language: 'xoo', name: 'Xoo Repository' }],
  }),
}));

// Simplify the SourceViewer
jest.mock('../../SourceViewer/SourceViewer', () => {
  function SourceViewer({
    component,
    onLoaded,
  }: {
    component: string;
    onLoaded: (component: { path: string; q: string }) => void;
  }) {
    // Trigger the "loadComponent" to update the name of the component
    React.useEffect(() => {
      onLoaded({ path: `path/to/component/${component}`, q: ComponentQualifier.File });
    }, [component, onLoaded]);

    return <div />;
  }

  return {
    __esModule: true,
    default: SourceViewer,
  };
});

const WINDOW_HEIGHT = 1000;
const originalHeight = window.innerHeight;

beforeAll(() => {
  Object.defineProperty(window, 'innerHeight', {
    writable: true,
    configurable: true,
    value: WINDOW_HEIGHT,
  });
});

afterAll(() => {
  Object.defineProperty(window, 'innerHeight', {
    writable: true,
    configurable: true,
    value: originalHeight,
  });
});

beforeEach(jest.clearAllMocks);

it('should load data from local storage and allow to open another component', async () => {
  const user = userEvent.setup();
  const component = {
    [TYPE_KEY]: WorkspaceTypes.Component,
    branchLike: mockBranch(),
    key: 'foo',
    name: 'previously opened file',
  };
  jest.mocked(get).mockReturnValueOnce(JSON.stringify([component]));

  renderWorkspace();

  expect(byText('previously opened file').get()).toBeInTheDocument();
  expect(
    byRole('heading', { name: 'qualifier.FIL path/to/component/k1' }).query(),
  ).not.toBeInTheDocument();

  await user.click(ui.componentOpenButton.get());

  expect(
    byRole('heading', { name: 'qualifier.FIL path/to/component/k1' }).get(),
  ).toBeInTheDocument();
  expect(save).toHaveBeenCalled();
});

it('should be resizable', async () => {
  const user = userEvent.setup();

  renderWorkspace();

  await user.click(ui.componentOpenButton.get());

  expect(ui.workspaceViewerContainer.get()).toHaveStyle({ height: '300px' });

  await user.click(ui.fullWindowButton.get());

  // 85% of window height, forced at 1000 in the test (WINDOW_HEIGHT)
  expect(ui.workspaceViewerContainer.get()).toHaveStyle({ height: '850px' });

  expect(ui.normalWindowButton.get()).toBeInTheDocument();
  expect(ui.fullWindowButton.query()).not.toBeInTheDocument();
  await user.click(ui.normalWindowButton.get());

  expect(ui.workspaceViewerContainer.get()).toHaveStyle({ height: '300px' });

  await user.click(ui.collapseButton.get());

  expect(ui.workspaceViewerContainer.query()).not.toBeInTheDocument();

  const fileButton = byRole('button', { name: 'qualifier.FIL path/to/component/k1' });
  expect(fileButton.get()).toBeInTheDocument();
  await user.click(fileButton.get());

  await user.click(ui.closeButton.get());
  expect(fileButton.query()).not.toBeInTheDocument();
});

function renderWorkspace(componentKey = 'k1', branchLike?: BranchLike) {
  return renderComponent(
    <Workspace>
      <TestComponent componentKey={componentKey} branchLike={branchLike} />
    </Workspace>,
  );
}

function TestComponent({
  componentKey,
  branchLike,
}: {
  componentKey: string;
  branchLike?: BranchLike;
}) {
  const { openComponent } = React.useContext(WorkspaceContext);

  const clickHandler = React.useCallback(() => {
    openComponent({
      key: componentKey,
      branchLike,
      name: componentKey,
      qualifier: ComponentQualifier.File,
    });
  }, [openComponent, componentKey, branchLike]);

  return (
    <button type="button" onClick={clickHandler}>
      open component
    </button>
  );
}

const ui = {
  componentOpenButton: byRole('button', { name: 'open component' }),
  collapseButton: byRole('button', { name: 'workspace.minimize' }),
  normalWindowButton: byRole('button', { name: 'workspace.normal_size' }),
  fullWindowButton: byRole('button', { name: 'workspace.full_window' }),
  closeButton: byRole('button', { name: 'workspace.close' }),
  workspaceViewerContainer: byRole('complementary'),
};
