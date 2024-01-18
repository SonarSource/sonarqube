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
import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React, { useContext } from 'react';
import { Route } from 'react-router-dom';
import { validateProjectAlmBinding } from '../../../api/alm-settings';
import { getTasksForComponent } from '../../../api/ce';
import { getComponentData } from '../../../api/components';
import { getComponentNavigation } from '../../../api/navigation';
import * as withRouter from '../../../components/hoc/withRouter';
import { mockProjectAlmBindingConfigurationErrors } from '../../../helpers/mocks/alm-settings';
import { mockBranch, mockPullRequest } from '../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockTask } from '../../../helpers/mocks/tasks';
import { HttpStatus } from '../../../helpers/request';
import { renderAppRoutes, renderComponent } from '../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../helpers/testSelector';
import { getProjectUrl, getPullRequestUrl } from '../../../helpers/urls';
import { ComponentQualifier, Visibility } from '../../../types/component';
import { TaskStatuses, TaskTypes } from '../../../types/tasks';
import handleRequiredAuthorization from '../../utils/handleRequiredAuthorization';
import ComponentContainer, { isSameBranch } from '../ComponentContainer';
import { WithAvailableFeaturesProps } from '../available-features/withAvailableFeatures';
import { ComponentContext } from '../componentContext/ComponentContext';

jest.mock('../../../api/ce', () => ({
  getTasksForComponent: jest.fn().mockResolvedValue({ queue: [] }),
}));

jest.mock('../../../api/components', () => ({
  getComponentData: jest
    .fn()
    .mockResolvedValue({ component: { name: 'component name', analysisDate: '2018-07-30' } }),
}));

jest.mock('../../../api/navigation', () => ({
  getComponentNavigation: jest.fn().mockResolvedValue({
    breadcrumbs: [{ key: 'portfolioKey', name: 'portfolio', qualifier: 'VW' }],
    key: 'portfolioKey',
  }),
}));

jest.mock('../../../api/branches', () => ({
  getBranches: jest.fn().mockResolvedValue([mockBranch()]),
  getPullRequests: jest.fn().mockResolvedValue([mockPullRequest({ target: 'dropped-branch' })]),
}));

jest.mock('../../../api/alm-settings', () => ({
  validateProjectAlmBinding: jest.fn().mockResolvedValue(undefined),
}));

jest.mock('../../utils/handleRequiredAuthorization', () => ({
  __esModule: true,
  default: jest.fn(),
}));

jest.mock('../../../components/hoc/withRouter', () => ({
  __esModule: true,
  ...jest.requireActual('../../../components/hoc/withRouter'),
}));

const ui = {
  projectTitle: byRole('link', { name: 'Project' }),
  projectText: byText('project'),
  portfolioTitle: byRole('link', { name: 'portfolio' }),
  portfolioText: byText('portfolio'),
  overviewPageLink: byRole('link', { name: 'overview.page' }),
  issuesPageLink: byRole('link', { name: 'issues.page' }),
  hotspotsPageLink: byRole('link', { name: 'layout.security_hotspots' }),
  measuresPageLink: byRole('link', { name: 'layout.measures' }),
  codePageLink: byRole('link', { name: 'code.page' }),
  activityPageLink: byRole('link', { name: 'project_activity.page' }),
  projectInfoLink: byRole('link', { name: 'project.info.title' }),
  dashboardNotFound: byText('dashboard.project.not_found'),
  goBackToHomePageLink: byRole('link', { name: 'go_back_to_homepage' }),
};

afterEach(() => {
  jest.clearAllMocks();
});

it('should render the component nav correctly for portfolio', async () => {
  renderComponentContainerAsComponent();
  expect(await ui.portfolioTitle.find()).toHaveAttribute('href', '/portfolio?id=portfolioKey');
  expect(ui.issuesPageLink.get()).toHaveAttribute(
    'href',
    '/project/issues?id=portfolioKey&issueStatuses=OPEN%2CCONFIRMED',
  );
  expect(ui.measuresPageLink.get()).toHaveAttribute('href', '/component_measures?id=portfolioKey');
  expect(ui.activityPageLink.get()).toHaveAttribute('href', '/project/activity?id=portfolioKey');

  await waitFor(() => {
    expect(getTasksForComponent).toHaveBeenCalledWith('portfolioKey');
  });
});

it('should render the component nav correctly for projects', async () => {
  const component = mockComponent({
    breadcrumbs: [{ key: 'project', name: 'Project', qualifier: ComponentQualifier.Project }],
    key: 'project-key',
    analysisDate: '2018-07-30',
  });

  jest
    .mocked(getComponentNavigation)
    .mockResolvedValueOnce({} as unknown as Awaited<ReturnType<typeof getComponentNavigation>>);

  jest
    .mocked(getComponentData)
    .mockResolvedValueOnce({ component } as unknown as Awaited<
      ReturnType<typeof getComponentData>
    >);

  renderComponentContainerAsComponent();
  expect(await ui.projectTitle.find()).toHaveAttribute('href', '/dashboard?id=project');
  expect(ui.overviewPageLink.get()).toHaveAttribute('href', '/dashboard?id=project-key');
  expect(ui.issuesPageLink.get()).toHaveAttribute(
    'href',
    '/project/issues?id=project-key&issueStatuses=OPEN%2CCONFIRMED',
  );
  expect(ui.hotspotsPageLink.get()).toHaveAttribute('href', '/security_hotspots?id=project-key');
  expect(ui.measuresPageLink.get()).toHaveAttribute('href', '/component_measures?id=project-key');
  expect(ui.codePageLink.get()).toHaveAttribute('href', '/code?id=project-key');
  expect(ui.activityPageLink.get()).toHaveAttribute('href', '/project/activity?id=project-key');
  expect(ui.projectInfoLink.get()).toHaveAttribute('href', '/project/information?id=project-key');
});

it('should be able to change component', async () => {
  const user = userEvent.setup();
  renderComponentContainer();
  expect(await screen.findByText('This is a test component')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'change component' })).toBeInTheDocument();
  expect(screen.getByText('component name')).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'change component' }));
  expect(screen.getByText('new component name')).toBeInTheDocument();
});

it('should show component not found if it does not exist', async () => {
  jest
    .mocked(getComponentNavigation)
    .mockRejectedValueOnce(new Response(null, { status: HttpStatus.NotFound }));

  renderComponentContainer();

  expect(await ui.dashboardNotFound.find()).toBeInTheDocument();
  expect(ui.goBackToHomePageLink.get()).toBeInTheDocument();
});

it('should show component not found if target branch is not found for fixing pull request', async () => {
  renderComponentContainer(
    { hasFeature: jest.fn().mockReturnValue(true) },
    '?id=foo&fixedInPullRequest=1001',
  );

  expect(await ui.dashboardNotFound.find()).toBeInTheDocument();
});

describe('getTasksForComponent', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  it('reload component after task progress finished', async () => {
    jest
      .mocked(getTasksForComponent)
      .mockResolvedValueOnce({
        queue: [{ id: 'foo', status: TaskStatuses.InProgress, type: TaskTypes.ViewRefresh }],
      } as unknown as Awaited<ReturnType<typeof getTasksForComponent>>)
      .mockResolvedValueOnce({
        queue: [],
      } as unknown as Awaited<ReturnType<typeof getTasksForComponent>>);

    renderComponentContainer();

    // First round, there's something in the queue, and component navigation was
    // not called again (it's called once at mount, hence the 1 times assertion
    // here).
    await waitFor(() => {
      expect(getComponentNavigation).toHaveBeenCalledTimes(1);
    });
    await waitFor(() => expect(getTasksForComponent).toHaveBeenCalledTimes(1));

    act(() => jest.runOnlyPendingTimers());

    // Second round, the queue is now empty, hence we assume the previous task
    // was done. We immediately load the component again.
    await waitFor(() => expect(getTasksForComponent).toHaveBeenCalledTimes(2));

    // Trigger the update.
    // The component was correctly re-loaded.
    await waitFor(() => {
      expect(getComponentNavigation).toHaveBeenCalledTimes(2);
    });
    // The status API call will be called 1 final time after the component is
    // fully loaded, so the total will be 3.
    expect(getTasksForComponent).toHaveBeenCalledTimes(3);

    // Make sure the timeout was cleared. It should not be called again.
    act(() => jest.runAllTimers());

    // The number of calls haven't changed.
    await waitFor(() => {
      expect(getComponentNavigation).toHaveBeenCalledTimes(2);
    });
    expect(getTasksForComponent).toHaveBeenCalledTimes(3);
  });

  it('reloads component after task progress finished, and moves straight to current', async () => {
    jest.mocked(getComponentData).mockResolvedValueOnce({
      component: { key: 'bar' },
    } as unknown as Awaited<ReturnType<typeof getComponentData>>);

    jest
      .mocked(getTasksForComponent)
      .mockResolvedValueOnce({ queue: [] } as unknown as Awaited<
        ReturnType<typeof getTasksForComponent>
      >)
      .mockResolvedValueOnce({
        queue: [],
        current: { id: 'foo', status: TaskStatuses.Success, type: TaskTypes.AppRefresh },
      } as unknown as Awaited<ReturnType<typeof getTasksForComponent>>);

    renderComponentContainer();

    // First round, nothing in the queue, and component navigation was not called
    // again (it's called once at mount, hence the 1 times assertion here).
    await waitFor(() => {
      expect(getComponentNavigation).toHaveBeenCalledTimes(1);
    });
    await waitFor(() => expect(getTasksForComponent).toHaveBeenCalledTimes(1));

    act(() => jest.runOnlyPendingTimers());

    // Second round, nothing in the queue, BUT a success task is current. This
    // means the queue was processed too quick for us to see, and we didn't see
    // any pending tasks in the queue. So we immediately load the component again.
    await waitFor(() => expect(getTasksForComponent).toHaveBeenCalledTimes(2));

    // Trigger the update.
    // The component was correctly re-loaded.
    await waitFor(() => {
      expect(getComponentNavigation).toHaveBeenCalledTimes(2);
    });
    // The status API call will be called 1 final time after the component is
    // fully loaded, so the total will be 3.
    expect(getTasksForComponent).toHaveBeenCalledTimes(3);
  });

  it('only fully loads a non-empty component once', async () => {
    jest.mocked(getComponentData).mockResolvedValueOnce({
      component: { key: 'bar', analysisDate: '2019-01-01' },
    } as unknown as Awaited<ReturnType<typeof getComponentData>>);

    jest.mocked(getTasksForComponent).mockResolvedValueOnce({
      queue: [],
      current: { id: 'foo', status: TaskStatuses.Success, type: TaskTypes.Report },
    } as unknown as Awaited<ReturnType<typeof getTasksForComponent>>);

    renderComponentContainer();
    await waitFor(() => {
      expect(getComponentNavigation).toHaveBeenCalledTimes(1);
    });

    await waitFor(() => expect(getTasksForComponent).toHaveBeenCalledTimes(1));
  });

  it('only fully reloads a non-empty component if there was previously some task in progress', async () => {
    jest.mocked(getComponentData).mockResolvedValueOnce({
      component: { key: 'bar', analysisDate: '2019-01-01' },
    } as unknown as Awaited<ReturnType<typeof getComponentData>>);

    jest
      .mocked(getTasksForComponent)
      .mockResolvedValueOnce({
        queue: [{ id: 'foo', status: TaskStatuses.InProgress, type: TaskTypes.AppRefresh }],
      } as unknown as Awaited<ReturnType<typeof getTasksForComponent>>)
      .mockResolvedValueOnce({
        queue: [],
        current: { id: 'foo', status: TaskStatuses.Success, type: TaskTypes.AppRefresh },
      } as unknown as Awaited<ReturnType<typeof getTasksForComponent>>);

    renderComponentContainer();

    // First round, a pending task in the queue. This should trigger a reload of the
    // status endpoint.
    await waitFor(() => expect(getTasksForComponent).toHaveBeenCalledTimes(1));

    act(() => jest.runOnlyPendingTimers());

    // Second round, nothing in the queue, and a success task is current. This
    // implies the current task was updated, and previously we displayed some information
    // about a pending task. This new information must prompt the component to reload
    // all data.
    await waitFor(() => expect(getTasksForComponent).toHaveBeenCalledTimes(2));

    // The component was correctly re-loaded.
    await waitFor(() => {
      expect(getComponentNavigation).toHaveBeenCalledTimes(2);
    });
    // The status API call will be called 1 final time after the component is
    // fully loaded, so the total will be 3.
    expect(getTasksForComponent).toHaveBeenCalledTimes(3);
  });
});

describe('should correctly validate the project binding depending on the context', () => {
  const COMPONENT = mockComponent({
    breadcrumbs: [{ key: 'foo', name: 'Foo', qualifier: ComponentQualifier.Project }],
  });
  const PROJECT_BINDING_ERRORS = mockProjectAlmBindingConfigurationErrors();

  it.each([
    ["has an analysis; won't perform any check", { ...COMPONENT, analysisDate: '2020-01' }],
    ['has a project binding; check is OK', COMPONENT, undefined, 1],
    ['has a project binding; check is not OK', COMPONENT, PROJECT_BINDING_ERRORS, 1],
  ])('%s', async (_, component, projectBindingErrors = undefined, n = 0) => {
    jest
      .mocked(getComponentNavigation)
      .mockResolvedValueOnce({} as unknown as Awaited<ReturnType<typeof getComponentNavigation>>);

    jest
      .mocked(getComponentData)
      .mockResolvedValueOnce({ component } as unknown as Awaited<
        ReturnType<typeof getComponentData>
      >);

    if (n > 0) {
      jest.mocked(validateProjectAlmBinding).mockResolvedValueOnce(projectBindingErrors);
    }

    renderComponentContainer({ hasFeature: jest.fn().mockReturnValue(true) });
    await waitFor(() => {
      expect(validateProjectAlmBinding).toHaveBeenCalledTimes(n);
    });
  });

  it('should show error message when check is not OK', async () => {
    jest
      .mocked(getComponentNavigation)
      .mockResolvedValueOnce({} as unknown as Awaited<ReturnType<typeof getComponentNavigation>>);

    jest
      .mocked(getComponentData)
      .mockResolvedValueOnce({ component: COMPONENT } as unknown as Awaited<
        ReturnType<typeof getComponentData>
      >);

    jest.mocked(validateProjectAlmBinding).mockResolvedValueOnce(PROJECT_BINDING_ERRORS);

    renderComponentContainerAsComponent({ hasFeature: jest.fn().mockReturnValue(true) });
    expect(
      await screen.findByText('component_navigation.pr_deco.error_detected_X', { exact: false }),
    ).toBeInTheDocument();
  });
});

describe('redirects', () => {
  it('should redirect if the user has no access', async () => {
    jest
      .mocked(getComponentNavigation)
      .mockRejectedValueOnce(new Response(null, { status: HttpStatus.Forbidden }));

    renderComponentContainer();
    await waitFor(() => {
      expect(handleRequiredAuthorization).toHaveBeenCalled();
    });
  });

  it('should redirect to portfolio when using dashboard path', async () => {
    renderComponentContainer(
      { hasFeature: jest.fn().mockReturnValue(true) },
      'dashboard?id=foo',
      '/dashboard',
    );

    expect(await ui.portfolioText.find()).toBeInTheDocument();
  });
});

it.each([
  [ComponentQualifier.Application],
  [ComponentQualifier.Portfolio],
  [ComponentQualifier.SubPortfolio],
])(
  'should not care about PR decoration settings for %s',
  async (componentQualifier: ComponentQualifier) => {
    const component = mockComponent({
      breadcrumbs: [{ key: 'foo', name: 'Foo', qualifier: componentQualifier }],
    });

    jest
      .mocked(getComponentNavigation)
      .mockResolvedValueOnce({} as unknown as Awaited<ReturnType<typeof getComponentNavigation>>);

    jest
      .mocked(getComponentData)
      .mockResolvedValueOnce({ component } as unknown as Awaited<
        ReturnType<typeof getComponentData>
      >);

    renderComponentContainer({ hasFeature: jest.fn().mockReturnValue(true) });
    await waitFor(() => {
      expect(validateProjectAlmBinding).not.toHaveBeenCalled();
    });
  },
);

it('isSameBranch util returns expected result', () => {
  expect(isSameBranch(mockTask())).toBe(true);
  expect(isSameBranch(mockTask({ branch: 'branch' }), 'branch')).toBe(true);
  expect(isSameBranch(mockTask({ pullRequest: 'pr' }), undefined, 'pr')).toBe(true);
});

describe('tutorials', () => {
  beforeEach(() => {
    jest.useFakeTimers({ advanceTimers: true });
  });
  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  it('should redirect to project main branch dashboard from tutorials when receiving new related scan report', async () => {
    const componentKey = 'foo-component';
    jest.mocked(getComponentData).mockResolvedValue({
      ancestors: [],
      component: {
        key: componentKey,
        name: 'component name',
        qualifier: ComponentQualifier.Project,
        visibility: Visibility.Public,
      },
    });
    jest
      .mocked(getTasksForComponent)
      .mockResolvedValueOnce({ queue: [] })
      .mockResolvedValue({
        queue: [{ status: TaskStatuses.InProgress, type: TaskTypes.Report }],
      } as unknown as Awaited<ReturnType<typeof getTasksForComponent>>);

    const mockedReplace = jest.fn();
    jest.spyOn(withRouter, 'useRouter').mockReturnValue({
      replace: mockedReplace,
      push: jest.fn(),
    });

    renderComponentContainer(
      { hasFeature: jest.fn().mockReturnValue(true) },
      `tutorials?id=${componentKey}`,
      '/',
    );

    await waitFor(() => expect(getTasksForComponent).toHaveBeenCalledTimes(1));

    act(() => jest.runOnlyPendingTimers());

    expect(mockedReplace).not.toHaveBeenCalled();
    await waitFor(() => expect(getTasksForComponent).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(mockedReplace).toHaveBeenCalledWith(getProjectUrl(componentKey)));
  });

  it('should redirect to project branch dashboard from tutorials when receiving new related scan report', async () => {
    const componentKey = 'foo-component';
    const branchName = 'fooBranch';
    jest.mocked(getComponentData).mockResolvedValue({
      ancestors: [],
      component: {
        key: componentKey,
        name: 'component name',
        qualifier: ComponentQualifier.Project,
        visibility: Visibility.Public,
      },
    });
    jest
      .mocked(getTasksForComponent)
      .mockResolvedValueOnce({ queue: [] })
      .mockResolvedValue({
        queue: [{ branch: branchName, status: TaskStatuses.InProgress, type: TaskTypes.Report }],
      } as unknown as Awaited<ReturnType<typeof getTasksForComponent>>);

    const mockedReplace = jest.fn();
    jest.spyOn(withRouter, 'useRouter').mockReturnValue({
      replace: mockedReplace,
      push: jest.fn(),
    });

    renderComponentContainer(
      { hasFeature: jest.fn().mockReturnValue(true) },
      `tutorials?id=${componentKey}`,
      '/',
    );

    await waitFor(() => expect(getTasksForComponent).toHaveBeenCalledTimes(1));

    act(() => jest.runOnlyPendingTimers());

    expect(mockedReplace).not.toHaveBeenCalled();
    await waitFor(() => expect(getTasksForComponent).toHaveBeenCalledTimes(2));
    await waitFor(() =>
      expect(mockedReplace).toHaveBeenCalledWith(getProjectUrl(componentKey, branchName)),
    );
  });

  it('should redirect to project pull request dashboard from tutorials when receiving new related scan report', async () => {
    const componentKey = 'foo-component';
    const pullRequestKey = 'fooPR';
    jest.mocked(getComponentData).mockResolvedValue({
      ancestors: [],
      component: {
        key: componentKey,
        name: 'component name',
        qualifier: ComponentQualifier.Project,
        visibility: Visibility.Public,
      },
    });
    jest
      .mocked(getTasksForComponent)
      .mockResolvedValueOnce({ queue: [] })
      .mockResolvedValue({
        queue: [
          { pullRequest: pullRequestKey, status: TaskStatuses.InProgress, type: TaskTypes.Report },
        ],
      } as unknown as Awaited<ReturnType<typeof getTasksForComponent>>);

    const mockedReplace = jest.fn();
    jest.spyOn(withRouter, 'useRouter').mockReturnValue({
      replace: mockedReplace,
      push: jest.fn(),
    });

    renderComponentContainer(
      { hasFeature: jest.fn().mockReturnValue(true) },
      `tutorials?id=${componentKey}`,
      '/',
    );

    await waitFor(() => expect(getTasksForComponent).toHaveBeenCalledTimes(1));

    act(() => jest.runOnlyPendingTimers());

    expect(mockedReplace).not.toHaveBeenCalled();
    await waitFor(() => expect(getTasksForComponent).toHaveBeenCalledTimes(2));
    await waitFor(() =>
      expect(mockedReplace).toHaveBeenCalledWith(getPullRequestUrl(componentKey, pullRequestKey)),
    );
  });
});

function renderComponentContainerAsComponent(props: Partial<WithAvailableFeaturesProps> = {}) {
  return renderComponent(
    <>
      <div id="component-nav-portal" />
      <ComponentContainer {...props} />
    </>,
    '/?id=foo',
  );
}

function renderComponentContainer(
  props: Partial<WithAvailableFeaturesProps> = {},
  navigateTo = '?id=foo',
  path = '/',
) {
  renderAppRoutes(
    path,
    () => (
      <Route element={<ComponentContainer {...props} />}>
        <Route path="*" element={<TestComponent />} />
        <Route path="portfolio" element={<div>portfolio</div>} />
        <Route path="dashboard" element={<div>project</div>} />
      </Route>
    ),
    {
      navigateTo,
    },
  );
}

function TestComponent() {
  const { component, onComponentChange } = useContext(ComponentContext);

  return (
    <div>
      This is a test component
      <span>{component?.name}</span>
      <button
        onClick={() =>
          onComponentChange(
            mockComponent({
              name: 'new component name',
              breadcrumbs: [
                { key: 'portfolioKey', name: 'portfolio', qualifier: ComponentQualifier.Portfolio },
              ],
            }),
          )
        }
        type="button"
      >
        change component
      </button>
    </div>
  );
}
