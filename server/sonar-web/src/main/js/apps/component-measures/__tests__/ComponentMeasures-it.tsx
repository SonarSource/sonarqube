/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { times } from 'lodash';
import selectEvent from 'react-select-event';
import { byLabelText, byRole, byTestId, byText } from 'testing-library-selector';
import ComponentsServiceMock from '../../../api/mocks/ComponentsServiceMock';
import IssuesServiceMock from '../../../api/mocks/IssuesServiceMock';
import { MeasuresServiceMock } from '../../../api/mocks/MeasuresServiceMock';
import { mockPullRequest } from '../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockMeasure, mockMetric } from '../../../helpers/testMocks';
import { renderAppWithComponentContext } from '../../../helpers/testReactTestingUtils';
import { ComponentContextShape, ComponentQualifier } from '../../../types/component';
import { MetricKey } from '../../../types/metrics';
import routes from '../routes';

jest.mock('../../../api/metrics', () => {
  const { DEFAULT_METRICS } = jest.requireActual('../../../helpers/mocks/metrics');
  const metrics = Object.values(MetricKey).map(
    (key) => DEFAULT_METRICS[key] ?? mockMetric({ key })
  );
  return {
    getAllMetrics: jest.fn().mockResolvedValue(metrics),
  };
});

const componentsHandler = new ComponentsServiceMock();
const measuresHandler = new MeasuresServiceMock();
const issuesHandler = new IssuesServiceMock();

afterEach(() => {
  componentsHandler.reset();
  measuresHandler.reset();
  issuesHandler.reset();
});

describe('rendering', () => {
  it('should correctly render the default overview', async () => {
    const { ui } = getPageObject();
    renderMeasuresApp();
    await ui.appLoaded();

    expect(ui.seeDataAsListLink.get()).toBeInTheDocument();
    expect(ui.overviewFacetBtn.get()).toHaveAttribute('aria-current', 'true');
    expect(ui.bubbleChart.get()).toBeInTheDocument();
    expect(within(ui.bubbleChart.get()).getAllByRole('link')).toHaveLength(8);
    expect(ui.newCodePeriodTxt.get()).toBeInTheDocument();

    // TODO: check all child facets?
    expect(ui.reliabilityFacetBtn.get()).toBeInTheDocument();
    expect(ui.securityFacetBtn.get()).toBeInTheDocument();
    expect(ui.securityReviewFacetBtn.get()).toBeInTheDocument();
    expect(ui.maintainabilityFacetBtn.get()).toBeInTheDocument();
    expect(ui.coverageFacetBtn.get()).toBeInTheDocument();
    expect(ui.duplicationsFacetBtn.get()).toBeInTheDocument();
    expect(ui.sizeFacetBtn.get()).toBeInTheDocument();
    expect(ui.complexityFacetBtn.get()).toBeInTheDocument();
    expect(ui.issuesFacetBtn.get()).toBeInTheDocument();
  });

  it('should correctly render a list view', async () => {
    const { ui } = getPageObject();
    renderMeasuresApp('component_measures?id=foo&metric=code_smells&view=list');
    await ui.appLoaded();

    expect(ui.measuresTable.get()).toBeInTheDocument();
    expect(ui.measuresRows.getAll()).toHaveLength(8);
  });

  it('should correctly render a tree view', async () => {
    const { ui } = getPageObject();
    renderMeasuresApp('component_measures?id=foo&metric=code_smells&view=tree');
    await ui.appLoaded();

    expect(ui.measuresTable.get()).toBeInTheDocument();
    expect(ui.measuresRows.getAll()).toHaveLength(7);
  });

  it('should correctly render a treemap view', async () => {
    const { ui } = getPageObject();
    renderMeasuresApp('component_measures?id=foo&metric=sqale_rating&view=treemap');
    await ui.appLoaded();

    expect(within(ui.treeMap.get()).getAllByRole('link')).toHaveLength(7);
    expect(ui.treeMapCell(/folderA .+ Maintainability Rating: C/).get()).toBeInTheDocument();
    expect(ui.treeMapCell(/test1\.js .+ Maintainability Rating: B/).get()).toBeInTheDocument();
    expect(ui.treeMapCell(/index\.tsx .+ Maintainability Rating: A/).get()).toBeInTheDocument();
  });

  it('should render correctly for an unknown metric', async () => {
    const { ui } = getPageObject();
    renderMeasuresApp('component_measures?id=foo&metric=unknown');
    await ui.appLoaded();

    // Fall back to a known metric.
    expect(screen.getAllByText('Releasability rating').length).toBeGreaterThan(0);
  });

  it('should render correctly if there are no measures', async () => {
    componentsHandler.registerComponentMeasures({});
    measuresHandler.registerComponentMeasures({});
    const { ui } = getPageObject();
    renderMeasuresApp();
    await ui.appLoaded();

    expect(ui.emptyText.get()).toBeInTheDocument();
  });

  it('should render correctly if on a pull request and viewing coverage', async () => {
    const { ui } = getPageObject();
    renderMeasuresApp('component_measures?id=foo&metric=coverage&pullRequest=1', {
      branchLike: mockPullRequest({ key: '1' }),
    });
    await ui.appLoaded();

    expect(ui.detailsUnavailableText.get()).toBeInTheDocument();
  });

  it('should render a warning message if the user does not have access to all components', async () => {
    const { ui } = getPageObject();
    renderMeasuresApp('component_measures?id=foo&metric=code_smells', {
      component: mockComponent({
        key: 'foo',
        qualifier: ComponentQualifier.Portfolio,
        canBrowseAllChildProjects: false,
      }),
    });
    await ui.appLoaded();

    expect(
      within(ui.noAccessWarning.get()).getByText('component_measures.not_all_measures_are_shown')
    ).toBeInTheDocument();
  });

  it('should correctly render the language distribution', async () => {
    const { ui } = getPageObject();
    renderMeasuresApp('component_measures?id=foo&metric=ncloc');
    await ui.appLoaded();

    expect(screen.getByText('10short_number_suffix.k')).toBeInTheDocument();
    expect(screen.getByText('java')).toBeInTheDocument();
    expect(screen.getByText('5short_number_suffix.k')).toBeInTheDocument();
    expect(screen.getByText('javascript')).toBeInTheDocument();
    expect(screen.getByText('1short_number_suffix.k')).toBeInTheDocument();
    expect(screen.getByText('css')).toBeInTheDocument();
  });

  it('should only show the best values in list mode', async () => {
    const tree = componentsHandler.findComponentTree('foo');
    /* eslint-disable-next-line jest/no-conditional-in-test */
    if (!tree) {
      throw new Error('Could not find base tree');
    }
    const measures = measuresHandler.getComponentMeasures();

    /* eslint-disable-next-line testing-library/no-node-access */
    tree.children.push(
      ...times(100, (n) => ({
        component: mockComponent({
          key: `foo:file${n}`,
          name: `file${n}`,
          qualifier: ComponentQualifier.File,
        }),
        ancestors: [tree.component],
        children: [],
      }))
    );
    componentsHandler.registerComponentTree(tree);
    measuresHandler.registerComponentMeasures(
      times(100, (n) => `foo:file${n}`).reduce((acc, key) => {
        acc[key] = {
          [MetricKey.sqale_rating]: mockMeasure({
            metric: MetricKey.sqale_rating,
            value: '1.0',
            bestValue: true,
          }),
        };
        return acc;
      }, measures)
    );

    const { ui, user } = getPageObject();
    renderMeasuresApp('component_measures?id=foo&metric=sqale_rating&view=list');
    await ui.appLoaded();

    expect(ui.notShowingAllComponentsTxt.get()).toBeInTheDocument();
    await user.click(ui.showAllBtn.get());
    expect(ui.notShowingAllComponentsTxt.query()).not.toBeInTheDocument();
  });
});

describe('navigation', () => {
  it('should be able to drilldown through the file structure', async () => {
    const { ui, user } = getPageObject();
    renderMeasuresApp();
    await ui.appLoaded();

    // Drilldown to the file level.
    await user.click(ui.maintainabilityFacetBtn.get());

    await user.click(ui.metricBtn('Code Smells 8').get());
    expect(
      within(ui.measuresRow('folderA').get()).getByRole('cell', { name: '3' })
    ).toBeInTheDocument();
    expect(
      within(ui.measuresRow('test1.js').get()).getByRole('cell', { name: '2' })
    ).toBeInTheDocument();

    await user.click(ui.fileLink('foo:folderA').get());
    expect(
      within(ui.measuresRow('out.tsx').get()).getByRole('cell', { name: '1' })
    ).toBeInTheDocument();
    expect(
      within(ui.measuresRow('in.tsx').get()).getByRole('cell', { name: '2' })
    ).toBeInTheDocument();

    await user.click(ui.fileLink('foo:folderA/out.tsx').get());
    expect((await ui.sourceCode.findAll()).length).toBeGreaterThan(0);

    // Go back using the breadcrumbs.
    await user.click(ui.breadcrumbLink('folderA').get());
    expect(ui.measuresRow('out.tsx').get()).toBeInTheDocument();
    expect(ui.measuresRow('in.tsx').get()).toBeInTheDocument();
  });

  it('should be able to drilldown thanks to a files list', async () => {
    const { ui, user } = getPageObject();
    renderMeasuresApp();
    await ui.appLoaded();

    await user.click(ui.maintainabilityFacetBtn.get());
    await user.click(ui.metricBtn('Code Smells 8').get());
    await ui.changeViewToList();

    expect(
      within(ui.measuresRow('out.tsx').get()).getByRole('cell', { name: '1' })
    ).toBeInTheDocument();
    expect(
      within(ui.measuresRow('test1.js').get()).getByRole('cell', { name: '2' })
    ).toBeInTheDocument();

    await user.click(ui.fileLink('foo:folderA/out.tsx').get());
    expect((await ui.sourceCode.findAll()).length).toBeGreaterThan(0);
  });

  it('should be able to drilldown thanks to a tree map', async () => {
    const { ui, user } = getPageObject();
    renderMeasuresApp();
    await ui.appLoaded();

    await user.click(ui.maintainabilityFacetBtn.get());
    await user.click(ui.metricBtn('Maintainability Rating metric.has_rating_X.E').get());
    await ui.changeViewToTreeMap();

    expect(ui.treeMapCell(/folderA/).get()).toBeInTheDocument();
    expect(ui.treeMapCell(/test1\.js/).get()).toBeInTheDocument();

    await user.click(ui.treeMapCell(/folderA/).get());

    expect(ui.treeMapCell(/out\.tsx/).get()).toBeInTheDocument();
    expect(ui.treeMapCell(/in\.tsx/).get()).toBeInTheDocument();

    await user.click(ui.treeMapCell(/out.tsx/).get());
    expect((await ui.sourceCode.findAll()).length).toBeGreaterThan(0);
  });

  it('should be able to drilldown using the keyboard', async () => {
    const { ui, user } = getPageObject();
    renderMeasuresApp();
    await ui.appLoaded();

    // Drilldown to the file level.
    await user.click(ui.maintainabilityFacetBtn.get());
    await user.click(ui.metricBtn('Code Smells 8').get());

    // Select "folderA".
    await ui.arrowDown();
    await act(async () => {
      await ui.arrowRight();
    });

    expect(
      within(ui.measuresRow('out.tsx').get()).getByRole('cell', { name: '1' })
    ).toBeInTheDocument();
    expect(
      within(ui.measuresRow('in.tsx').get()).getByRole('cell', { name: '2' })
    ).toBeInTheDocument();

    // Move back to project.
    await act(async () => {
      await ui.arrowLeft();
    });

    expect(
      within(ui.measuresRow('folderA').get()).getByRole('cell', { name: '3' })
    ).toBeInTheDocument();

    // Go to "folderA/out.tsx".
    await act(async () => {
      await ui.arrowRight();
    });
    await ui.arrowDown();
    await ui.arrowDown();
    await act(async () => {
      await ui.arrowRight();
    });

    expect((await ui.sourceCode.findAll()).length).toBeGreaterThan(0);
    expect(screen.getAllByText('out.tsx').length).toBeGreaterThan(0);
  });
});

describe('redirects', () => {
  it('should redirect old history route', () => {
    renderMeasuresApp('component_measures/metric/bugs/history?id=foo');
    expect(
      screen.getByText('/project/activity?id=foo&graph=custom&custom_metrics=bugs')
    ).toBeInTheDocument();
  });

  it('should redirect old metric route', async () => {
    const { ui } = getPageObject();
    renderMeasuresApp('component_measures/metric/bugs');
    await ui.appLoaded();
    expect(ui.metricBtn('Bugs 0').get()).toHaveAttribute('aria-current', 'true');
  });

  it('should redirect old domain route', async () => {
    const { ui } = getPageObject();
    renderMeasuresApp('component_measures/domain/bugs');
    await ui.appLoaded();
    expect(ui.reliabilityFacetBtn.get()).toHaveAttribute('aria-expanded', 'true');
  });
});

it('should allow to load more components', async () => {
  const tree = componentsHandler.findComponentTree('foo');
  /* eslint-disable-next-line jest/no-conditional-in-test */
  if (!tree) {
    throw new Error('Could not find base tree');
  }

  /* eslint-disable-next-line testing-library/no-node-access */
  tree.children.push(
    ...times(1000, (n) => ({
      component: mockComponent({
        key: `foo:file${n}`,
        name: `file${n}`,
        qualifier: ComponentQualifier.File,
      }),
      ancestors: [tree.component],
      children: [],
    }))
  );
  componentsHandler.registerComponentTree(tree);

  const { ui, user } = getPageObject();
  renderMeasuresApp('component_measures?id=foo&metric=code_smells&view=list');
  await ui.appLoaded();
  await user.click(ui.showAllBtn.get());

  expect(ui.showingOutOfTxt('500', '1,008').get()).toBeInTheDocument();
  await ui.clickLoadMore();
  expect(ui.showingOutOfTxt('1,000', '1,008').get()).toBeInTheDocument();
});

// TODO:
// - activity links
// - sidebar facet values (issue count, rating, etc)

function getPageObject() {
  const user = userEvent.setup();

  const selectors = {
    // Overview
    seeDataAsListLink: byRole('link', { name: 'component_measures.overview.see_data_as_list' }),
    bubbleChart: byTestId('bubble-chart'),
    newCodePeriodTxt: byText(
      'overview.new_code_period_x.overview.period.previous_version_only_date'
    ),

    // Facets
    overviewFacetBtn: byRole('button', {
      name: 'component_measures.overview.project_overview.subnavigation',
    }),
    releasabilityFacetBtn: byRole('button', {
      name: 'Releasability component_measures.domain_subnavigation.Releasability.help',
    }),
    reliabilityFacetBtn: byRole('button', {
      name: 'Reliability component_measures.domain_subnavigation.Reliability.help',
    }),
    securityFacetBtn: byRole('button', {
      name: 'Security component_measures.domain_subnavigation.Security.help',
    }),
    securityReviewFacetBtn: byRole('button', {
      name: 'SecurityReview component_measures.domain_subnavigation.SecurityReview.help',
    }),
    maintainabilityFacetBtn: byRole('button', {
      name: 'Maintainability component_measures.domain_subnavigation.Maintainability.help',
    }),
    coverageFacetBtn: byRole('button', {
      name: 'Coverage component_measures.domain_subnavigation.Coverage.help',
    }),
    duplicationsFacetBtn: byRole('button', {
      name: 'Duplications component_measures.domain_subnavigation.Duplications.help',
    }),
    sizeFacetBtn: byRole('button', {
      name: 'Size component_measures.domain_subnavigation.Size.help',
    }),
    complexityFacetBtn: byRole('button', {
      name: 'Complexity component_measures.domain_subnavigation.Complexity.help',
    }),
    issuesFacetBtn: byRole('button', {
      name: 'Issues component_measures.domain_subnavigation.Issues.help',
    }),
    metricBtn: (name: string) => byRole('button', { name }),

    // Measure content
    measuresTable: byRole('table'),
    measuresRows: byRole('row'),
    measuresRow: (name: string) => byRole('row', { name: new RegExp(name) }),
    treeMap: byTestId('treemap'),
    treeMapCells: byRole('link'),
    treeMapCell: (name: string | RegExp) => byRole('link', { name }),
    fileLink: (name: string) => byRole('link', { name }),
    sourceCode: byText('function Test() {}'),
    notShowingAllComponentsTxt: byText(/component_measures.hidden_best_score_metrics/),

    // Misc
    loading: byLabelText('loading'),
    breadcrumbLink: (name: string) => byRole('link', { name }),
    viewSelect: byLabelText('component_measures.view_as'),
    emptyText: byText('component_measures.empty'),
    detailsUnavailableText: byText('component_measures.details_are_not_available'),
    noAccessWarning: byRole('alert'),
    showingOutOfTxt: (x: string, y: string) => byText(`x_of_y_shown.${x}.${y}`),
    showAllBtn: byRole('button', { name: 'show_them' }),
  };

  const ui = {
    ...selectors,

    async appLoaded() {
      await waitFor(() => {
        expect(selectors.loading.query()).not.toBeInTheDocument();
      });
    },
    async changeViewToList() {
      await selectEvent.select(ui.viewSelect.get(), 'component_measures.tab.list');
    },
    async changeViewToTreeMap() {
      await selectEvent.select(ui.viewSelect.get(), 'component_measures.tab.treemap');
    },
    async arrowDown() {
      await user.keyboard('[ArrowDown]');
    },
    async arrowRight() {
      await user.keyboard('[ArrowRight]');
    },
    async arrowLeft() {
      await user.keyboard('[ArrowLeft]');
    },
    async clickLoadMore() {
      await user.click(screen.getByRole('button', { name: 'show_more' }));
    },
  };

  return {
    ui,
    user,
  };
}

function renderMeasuresApp(navigateTo?: string, componentContext?: Partial<ComponentContextShape>) {
  return renderAppWithComponentContext(
    'component_measures',
    routes,
    { navigateTo },
    { component: mockComponent({ key: 'foo' }), ...componentContext }
  );
}
