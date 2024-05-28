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
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { times } from 'lodash';
import selectEvent from 'react-select-event';
import { byLabelText, byRole, byTestId, byText } from '~sonar-aligned/helpers/testSelector';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { MetricKey } from '~sonar-aligned/types/metrics';
import BranchesServiceMock from '../../../api/mocks/BranchesServiceMock';
import ComponentsServiceMock from '../../../api/mocks/ComponentsServiceMock';
import IssuesServiceMock from '../../../api/mocks/IssuesServiceMock';
import { MeasuresServiceMock } from '../../../api/mocks/MeasuresServiceMock';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockMeasure, mockMetric } from '../../../helpers/testMocks';
import { renderAppWithComponentContext } from '../../../helpers/testReactTestingUtils';
import { ComponentContextShape } from '../../../types/component';
import { Feature } from '../../../types/features';
import routes from '../routes';

jest.mock('lodash', () => ({
  ...jest.requireActual('lodash'),
  throttle: (fn: (...args: unknown[]) => unknown) => fn,
}));

jest.mock('../../../api/metrics', () => {
  const { DEFAULT_METRICS } = jest.requireActual('../../../helpers/mocks/metrics');
  const metrics = Object.values(MetricKey).map(
    (key) => DEFAULT_METRICS[key] ?? mockMetric({ key }),
  );
  return {
    getAllMetrics: jest.fn().mockResolvedValue(metrics),
  };
});

const componentsHandler = new ComponentsServiceMock();
const measuresHandler = new MeasuresServiceMock();
const issuesHandler = new IssuesServiceMock();
const branchHandler = new BranchesServiceMock();

afterEach(() => {
  componentsHandler.reset();
  measuresHandler.reset();
  issuesHandler.reset();
  branchHandler.reset();
});

describe('rendering', () => {
  it('should correctly render the default overview and navigation', async () => {
    const { ui, user } = getPageObject();
    renderMeasuresApp();
    await ui.appLoaded();

    // Overview.
    expect(ui.seeDataAsListLink.get()).toBeInTheDocument();
    expect(ui.overviewDomainBtn.get()).toHaveAttribute('aria-current', 'true');
    expect(ui.bubbleChart.get()).toBeInTheDocument();
    expect(within(ui.bubbleChart.get()).getAllByRole('link')).toHaveLength(8);
    expect(ui.newCodePeriodTxt.get()).toBeInTheDocument();

    // Sidebar.
    expect(ui.reliabilityDomainBtn.get()).toBeInTheDocument();
    expect(ui.securityDomainBtn.get()).toBeInTheDocument();
    expect(ui.securityReviewDomainBtn.get()).toBeInTheDocument();
    expect(ui.maintainabilityDomainBtn.get()).toBeInTheDocument();
    expect(ui.coverageDomainBtn.get()).toBeInTheDocument();
    expect(ui.duplicationsDomainBtn.get()).toBeInTheDocument();
    expect(ui.sizeDomainBtn.get()).toBeInTheDocument();
    expect(ui.complexityDomainBtn.get()).toBeInTheDocument();
    expect(ui.issuesDomainBtn.get()).toBeInTheDocument();

    // Check one of the domains.
    await user.click(ui.maintainabilityDomainBtn.get());
    [
      'component_measures.metric.new_maintainability_issues.name 5',
      'Added Technical Debt work_duration.x_minutes.1',
      'Technical Debt Ratio on New Code 1.0%',
      'Maintainability Rating on New Code metric.has_rating_X.E',
      'component_measures.metric.maintainability_issues.name 2',
      'Technical Debt work_duration.x_minutes.1',
      'Technical Debt Ratio 1.0%',
      'Maintainability Rating metric.has_rating_X.E',
      'Effort to Reach Maintainability Rating A work_duration.x_minutes.1',
    ].forEach((measure) => {
      expect(ui.measureBtn(measure).get()).toBeInTheDocument();
    });
  });

  it('should correctly revert to old measures when analysis is missing', async () => {
    measuresHandler.deleteComponentMeasure('foo', MetricKey.maintainability_issues);
    measuresHandler.deleteComponentMeasure('foo', MetricKey.new_maintainability_issues);

    const { ui, user } = getPageObject();
    renderMeasuresApp();
    await ui.appLoaded();

    // Check one of the domains.
    await user.click(ui.maintainabilityDomainBtn.get());
    [
      'component_measures.metric.new_code_smells.name 9',
      'Added Technical Debt work_duration.x_minutes.1',
      'Technical Debt Ratio on New Code 1.0%',
      'Maintainability Rating on New Code metric.has_rating_X.E',
      'component_measures.metric.code_smells.name 9',
      'Technical Debt work_duration.x_minutes.1',
      'Technical Debt Ratio 1.0%',
      'Maintainability Rating metric.has_rating_X.E',
      'Effort to Reach Maintainability Rating A work_duration.x_minutes.1',
    ].forEach((measure) => {
      expect(ui.measureBtn(measure).get()).toBeInTheDocument();
    });
    expect(screen.getByText('overview.missing_project_dataTRK')).toBeInTheDocument();
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

  it('should correctly render a rating treemap view', async () => {
    const { ui } = getPageObject();
    renderMeasuresApp('component_measures?id=foo&metric=sqale_rating&view=treemap');
    await ui.appLoaded();

    expect(ui.treeMap.byRole('link').getAll()).toHaveLength(7);
    expect(ui.treeMapCell(/folderA .+ Maintainability Rating: C/).get()).toBeInTheDocument();
    expect(ui.treeMapCell(/test1\.js .+ Maintainability Rating: B/).get()).toBeInTheDocument();
    expect(ui.treeMapCell(/index\.tsx .+ Maintainability Rating: A/).get()).toBeInTheDocument();
  });

  it('should correctly render a percent treemap view', async () => {
    const { measures } = componentsHandler;

    measures['foo:folderA'][MetricKey.coverage] = {
      metric: MetricKey.coverage,
      value: '74.2',
    };
    measures['foo:test1.js'][MetricKey.coverage] = {
      metric: MetricKey.coverage,
      value: undefined,
    };
    measures['foo:index.tsx'][MetricKey.coverage] = {
      metric: MetricKey.coverage,
      value: '13.1',
    };

    const { ui } = getPageObject();
    renderMeasuresApp('component_measures?id=foo&metric=coverage&view=treemap');
    await ui.appLoaded();

    expect(ui.treeMap.byRole('link').getAll()).toHaveLength(7);

    expect(ui.treeMapCell(/folderA .+ Coverage: 74.2%/).get()).toBeInTheDocument();
    expect(ui.treeMapCell(/test1\.js .+ Coverage: —/).get()).toBeInTheDocument();
    expect(ui.treeMapCell(/index\.tsx .+ Coverage: 13.1%/).get()).toBeInTheDocument();
  });

  it('should render correctly for an unknown metric', async () => {
    const { ui } = getPageObject();
    renderMeasuresApp('component_measures?id=foo&metric=unknown');
    await ui.appLoaded();

    // Fall back to a known metric.
    expect(screen.getAllByText('Releasability rating').length).toBeGreaterThan(0);
  });

  it('should render issues measures when query by open_issues', async () => {
    const { ui } = getPageObject();
    renderMeasuresApp('component_measures?id=foo&metric=open_issues');
    await ui.appLoaded();

    expect(screen.getAllByText('Issues').length).toEqual(1);
    [
      'component_measures.metric.new_violations.name 1',
      'component_measures.metric.violations.name 1',
      'component_measures.metric.confirmed_issues.name 1',
      'component_measures.metric.accepted_issues.name 1',
      'component_measures.metric.new_accepted_issues.name 1',
      'component_measures.metric.false_positive_issues.name 1',
    ].forEach((measure) => {
      expect(ui.measureBtn(measure).get()).toBeInTheDocument();
    });
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
    renderMeasuresApp('component_measures?id=foo&metric=coverage&pullRequest=01');
    await ui.appLoaded();

    expect(await ui.detailsUnavailableText.find()).toBeInTheDocument();
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
      within(ui.noAccessWarning.get()).getByText('component_measures.not_all_measures_are_shown'),
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
      })),
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
      }, measures),
    );

    const { ui, user } = getPageObject();
    renderMeasuresApp('component_measures?id=foo&metric=sqale_rating&view=list');
    await ui.appLoaded();

    expect(ui.notShowingAllComponentsTxt.get()).toBeInTheDocument();
    await user.click(ui.showAllBtn.get());
    expect(ui.notShowingAllComponentsTxt.query()).not.toBeInTheDocument();
  });

  it('should correctly render a link to the activity page', async () => {
    const { ui, user } = getPageObject();
    renderMeasuresApp('component_measures?id=foo&metric=new_maintainability_issues');
    await ui.appLoaded();

    expect(ui.goToActivityLink.query()).not.toBeInTheDocument();
    await user.click(
      ui.measureBtn('component_measures.metric.maintainability_issues.name 2').get(),
    );
    expect(ui.goToActivityLink.get()).toHaveAttribute(
      'href',
      '/project/activity?id=foo&graph=custom&custom_metrics=maintainability_issues',
    );
  });

  it('should not render View select options for application metrics', async () => {
    const { ui } = getPageObject();
    const app = mockComponent({ key: 'app', qualifier: ComponentQualifier.Application });
    const tree = {
      component: app,
      children: [],
      ancestors: [],
    };

    componentsHandler.registerComponentTree(tree, true);
    measuresHandler.setComponents(tree);
    renderMeasuresApp('component_measures?id=app&metric=new_code_smells', { component: app });
    await ui.appLoaded();

    expect(ui.viewSelect.query()).not.toBeInTheDocument();
  });
});

describe('navigation', () => {
  it('should be able to drilldown through the file structure', async () => {
    const { ui, user } = getPageObject();
    renderMeasuresApp();
    await ui.appLoaded();

    // Drilldown to the file level.
    await user.click(ui.maintainabilityDomainBtn.get());

    await user.click(
      ui.measureBtn('component_measures.metric.maintainability_issues.name 2').get(),
    );
    expect(
      within(ui.measuresRow('folderA').get()).getByRole('cell', { name: '2' }),
    ).toBeInTheDocument();
    expect(
      within(ui.measuresRow('test1.js').get()).getByRole('cell', { name: '2' }),
    ).toBeInTheDocument();

    await user.click(ui.fileLink('folderA').get());
    expect(
      within(ui.measuresRow('out.tsx').get()).getByRole('cell', { name: '2' }),
    ).toBeInTheDocument();
    expect(
      within(ui.measuresRow('in.tsx').get()).getByRole('cell', { name: '2' }),
    ).toBeInTheDocument();

    await user.click(ui.fileLink('out.tsx').get());
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

    await user.click(ui.maintainabilityDomainBtn.get());
    await user.click(
      ui.measureBtn('component_measures.metric.maintainability_issues.name 2').get(),
    );
    await waitFor(() => ui.changeViewToList());

    expect(
      within(await ui.measuresRow('out.tsx').find()).getByRole('cell', { name: '2' }),
    ).toBeInTheDocument();
    expect(
      within(ui.measuresRow('test1.js').get()).getByRole('cell', { name: '2' }),
    ).toBeInTheDocument();

    await user.click(ui.fileLink('out.tsx').get());
    expect((await ui.sourceCode.findAll()).length).toBeGreaterThan(0);
  });

  it('should be able to drilldown thanks to a tree map', async () => {
    const { ui, user } = getPageObject();
    renderMeasuresApp();
    await ui.appLoaded();

    await user.click(ui.maintainabilityDomainBtn.get());
    await user.click(ui.measureBtn('Maintainability Rating metric.has_rating_X.E').get());
    await waitFor(() => ui.changeViewToTreeMap());

    expect(await ui.treeMapCell(/folderA/).find()).toBeInTheDocument();
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
    await user.click(ui.maintainabilityDomainBtn.get());
    await user.click(
      ui.measureBtn('component_measures.metric.maintainability_issues.name 2').get(),
    );

    await ui.arrowDown(); // Select the 1st element ("folderA")
    await ui.arrowRight(); // Open "folderA"

    expect(
      within(ui.measuresRow('out.tsx').get()).getByRole('cell', { name: '2' }),
    ).toBeInTheDocument();
    expect(
      within(ui.measuresRow('in.tsx').get()).getByRole('cell', { name: '2' }),
    ).toBeInTheDocument();

    // Move back to project
    await ui.arrowLeft(); // Close "folderA"

    expect(
      within(ui.measuresRow('folderA').get()).getByRole('cell', { name: '2' }),
    ).toBeInTheDocument();

    await ui.arrowRight(); // Open "folderA"

    await ui.arrowDown(); // Select the 1st element ("out.tsx")
    await ui.arrowRight(); // Open "out.tsx"

    expect((await ui.sourceCode.findAll()).length).toBeGreaterThan(0);
    expect(screen.getAllByText('out.tsx').length).toBeGreaterThan(0);
  });
});

describe('redirects', () => {
  it('should redirect old history route', () => {
    renderMeasuresApp('component_measures/metric/bugs/history?id=foo');
    expect(
      screen.getByText('/project/activity?id=foo&graph=custom&custom_metrics=bugs'),
    ).toBeInTheDocument();
  });

  it('should redirect old metric route', async () => {
    measuresHandler.deleteComponentMeasure('foo', MetricKey.maintainability_issues);
    measuresHandler.deleteComponentMeasure('foo', MetricKey.new_maintainability_issues);

    const { ui } = getPageObject();
    renderMeasuresApp('component_measures/metric/bugs?id=foo');
    await ui.appLoaded();
    expect(ui.measureBtn('component_measures.metric.bugs.name 0').get()).toHaveAttribute(
      'aria-current',
      'true',
    );
  });

  it('should redirect old metric route for software qualities', async () => {
    const { ui } = getPageObject();
    renderMeasuresApp('component_measures/metric/security_issues?id=foo');
    await ui.appLoaded();
    expect(ui.measureBtn('component_measures.metric.security_issues.name 1').get()).toHaveAttribute(
      'aria-current',
      'true',
    );
  });

  it('should redirect old domain route', async () => {
    measuresHandler.deleteComponentMeasure('foo', MetricKey.maintainability_issues);
    measuresHandler.deleteComponentMeasure('foo', MetricKey.new_maintainability_issues);

    const { ui } = getPageObject();
    renderMeasuresApp('component_measures/domain/bugs?id=foo');
    await ui.appLoaded();
    expect(ui.reliabilityDomainBtn.get()).toHaveAttribute('aria-expanded', 'true');
  });

  it('should redirect old domain route for software qualities', async () => {
    const { ui } = getPageObject();
    renderMeasuresApp('component_measures/domain/reliability_issues?id=foo');
    await ui.appLoaded();
    expect(ui.reliabilityDomainBtn.get()).toHaveAttribute('aria-expanded', 'true');
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
    })),
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

function getPageObject() {
  const user = userEvent.setup();

  const selectors = {
    // Overview
    seeDataAsListLink: byRole('link', { name: 'component_measures.overview.see_data_as_list' }),
    bubbleChart: byTestId('bubble-chart'),
    newCodePeriodTxt: byText('component_measures.leak_legend.new_code'),

    // Navigation
    overviewDomainBtn: byRole('button', {
      name: 'component_measures.overview.project_overview.subnavigation',
    }),
    releasabilityDomainBtn: byRole('button', {
      name: 'Releasability component_measures.domain_subnavigation.Releasability.help',
    }),
    reliabilityDomainBtn: byRole('button', {
      name: 'Reliability component_measures.domain_subnavigation.Reliability.help',
    }),
    securityDomainBtn: byRole('button', {
      name: 'Security component_measures.domain_subnavigation.Security.help',
    }),
    securityReviewDomainBtn: byRole('button', {
      name: 'SecurityReview component_measures.domain_subnavigation.SecurityReview.help',
    }),
    maintainabilityDomainBtn: byRole('button', {
      name: 'Maintainability component_measures.domain_subnavigation.Maintainability.help',
    }),
    coverageDomainBtn: byRole('button', {
      name: 'Coverage component_measures.domain_subnavigation.Coverage.help',
    }),
    duplicationsDomainBtn: byRole('button', {
      name: 'Duplications component_measures.domain_subnavigation.Duplications.help',
    }),
    sizeDomainBtn: byRole('button', {
      name: 'Size component_measures.domain_subnavigation.Size.help',
    }),
    complexityDomainBtn: byRole('button', {
      name: 'Complexity component_measures.domain_subnavigation.Complexity.help',
    }),
    issuesDomainBtn: byRole('button', {
      name: 'Issues component_measures.domain_subnavigation.Issues.help',
    }),
    measureBtn: (name: string) => byRole('button', { name }),

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
    loading: byText('loading'),
    breadcrumbLink: (name: string) => byRole('link', { name }),
    viewSelect: byLabelText('component_measures.view_as'),
    emptyText: byText('component_measures.empty'),
    detailsUnavailableText: byText('component_measures.details_are_not_available'),
    noAccessWarning: byText('component_measures.not_all_measures_are_shown'),
    showingOutOfTxt: (x: string, y: string) => byText(`x_of_y_shown.${x}.${y}`),
    showAllBtn: byRole('button', {
      name: 'component_measures.hidden_best_score_metrics_show_label',
    }),
    goToActivityLink: byRole('link', { name: 'component_measures.see_metric_history' }),
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
    'component_measures?id=foo',
    routes,
    { navigateTo, featureList: [Feature.BranchSupport] },
    { component: mockComponent({ key: 'foo' }), ...componentContext },
  );
}
