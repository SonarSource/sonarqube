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
import { UserEvent } from '@testing-library/user-event/dist/types/setup/setup';
import { keyBy, omit, times } from 'lodash';
import {
  QuerySelector,
  byLabelText,
  byRole,
  byTestId,
  byText,
} from '~sonar-aligned/helpers/testSelector';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { MetricKey } from '~sonar-aligned/types/metrics';
import BranchesServiceMock from '../../../api/mocks/BranchesServiceMock';
import ComponentsServiceMock from '../../../api/mocks/ComponentsServiceMock';
import { PARENT_COMPONENT_KEY, RULE_1 } from '../../../api/mocks/data/ids';
import IssuesServiceMock from '../../../api/mocks/IssuesServiceMock';
import SourcesServiceMock from '../../../api/mocks/SourcesServiceMock';
import { CCT_SOFTWARE_QUALITY_METRICS } from '../../../helpers/constants';
import { isDiffMetric } from '../../../helpers/measures';
import { mockComponent } from '../../../helpers/mocks/component';
import {
  mockSnippetsByComponent,
  mockSourceLine,
  mockSourceViewerFile,
} from '../../../helpers/mocks/sources';
import { mockMeasure, mockRawIssue } from '../../../helpers/testMocks';
import { renderAppWithComponentContext } from '../../../helpers/testReactTestingUtils';
import { IssueStatus } from '../../../types/issues';
import { Component } from '../../../types/types';
import routes from '../routes';

jest.mock('../../../components/intl/DateFromNow');

jest.mock('../../../components/SourceViewer/helpers/lines', () => {
  const lines = jest.requireActual('../../../components/SourceViewer/helpers/lines');
  return {
    ...lines,
    LINES_TO_LOAD: 20,
  };
});

jest.mock('../../../api/quality-gates', () => ({
  getQualityGateProjectStatus: jest.fn(),
}));

const DEFAULT_LINES_LOADED = 19;
const originalScrollTo = window.scrollTo;

const branchesHandler = new BranchesServiceMock();
const componentsHandler = new ComponentsServiceMock();
const sourcesHandler = new SourcesServiceMock();
const issuesHandler = new IssuesServiceMock();

const JUPYTER_ISSUE = {
  issue: mockRawIssue(false, {
    key: 'some-issue',
    component: `${PARENT_COMPONENT_KEY}:jpt.ipynb`,
    message: 'Issue on Jupyter Notebook',
    rule: RULE_1,
    textRange: {
      startLine: 1,
      endLine: 1,
      startOffset: 1148,
      endOffset: 1159,
    },
    ruleDescriptionContextKey: 'spring',
    ruleStatus: 'DEPRECATED',
    quickFixAvailable: true,
    tags: ['unused'],
    project: 'org.sonarsource.javascript:javascript',
    assignee: 'email1@sonarsource.com',
    author: 'email3@sonarsource.com',
    issueStatus: IssueStatus.Confirmed,
    prioritizedRule: true,
  }),
  snippets: keyBy(
    [
      mockSnippetsByComponent(
        'jpt.ipynb',
        PARENT_COMPONENT_KEY,
        times(40, (i) => i + 20),
      ),
    ],
    'component.key',
  ),
};

beforeAll(() => {
  Object.defineProperty(window, 'scrollTo', {
    writable: true,
    value: () => {
      /* noop */
    },
  });
});

afterAll(() => {
  Object.defineProperty(window, 'scrollTo', {
    writable: true,
    value: originalScrollTo,
  });
});

beforeEach(() => {
  branchesHandler.reset();
  componentsHandler.reset();
  sourcesHandler.reset();
  issuesHandler.reset();
});

it('should allow navigating through the tree', async () => {
  const ui = getPageObject(userEvent.setup());
  renderCode();
  await ui.appLoaded();

  // Navigate by clicking on an element.
  await ui.clickOnChildComponent(/folderA$/);
  expect(await ui.childComponent(/out\.tsx/).find()).toBeInTheDocument();
  expect(screen.getByRole('navigation', { name: 'breadcrumbs' })).toBeInTheDocument();

  // Navigate back using the breadcrumb.
  await ui.clickOnBreadcrumb(/Foo$/);
  expect(await ui.childComponent(/folderA/).find()).toBeInTheDocument();
  expect(screen.queryByRole('navigation', { name: 'breadcrumbs' })).not.toBeInTheDocument();

  // Open "index.tsx" file using keyboard navigation.
  await ui.arrowDown();
  await ui.arrowDown();
  await ui.arrowRight();
  // Load source viewer.
  expect((await ui.sourceCode.findAll()).length).toEqual(DEFAULT_LINES_LOADED);

  // Navigate back using keyboard.
  await ui.arrowLeft();
  expect(await ui.childComponent(/folderA/).find()).toBeInTheDocument();
});

it('should behave correctly when using search', async () => {
  const ui = getPageObject(userEvent.setup());
  renderCode({
    navigateTo: `code?id=foo&search=nonexistent`,
  });
  await ui.appLoaded();

  // Starts with a query from the URL.
  expect(await ui.noResultsTxt.find()).toBeInTheDocument();
  await ui.clearSearch();

  // Search with results that are deeper than the current level.
  await ui.searchForComponent('out');
  expect(ui.searchResult(/out\.tsx/).get()).toBeInTheDocument();

  // Search with no results.
  await ui.searchForComponent('nonexistent');
  expect(await ui.noResultsTxt.find()).toBeInTheDocument();
  await ui.clearSearch();

  // Open file using keyboard navigation.
  await ui.searchForComponent('index');
  await ui.arrowDown();
  await ui.arrowDown();
  await ui.arrowRight();
  // Load source viewer.
  expect((await ui.sourceCode.findAll()).length).toEqual(DEFAULT_LINES_LOADED);

  // Navigate back using keyboard.
  await ui.arrowLeft();
  expect(await ui.searchResult(/folderA/).find()).toBeInTheDocument();
});

it('should correctly handle long lists of components', async () => {
  const component = mockComponent(componentsHandler.findComponentTree('foo')?.component);
  componentsHandler.registerComponentTree({
    component,
    ancestors: [],
    children: times(300, (n) => ({
      component: mockComponent({
        key: `foo:file${n}`,
        name: `file${n}`,
        qualifier: ComponentQualifier.File,
      }),
      ancestors: [component],
      children: [],
    })),
  });
  const ui = getPageObject(userEvent.setup());
  renderCode();
  await ui.appLoaded();

  expect(ui.showingOutOfTxt(100, 300).get()).toBeInTheDocument();
  await ui.clickLoadMore();
  expect(ui.showingOutOfTxt(200, 300).get()).toBeInTheDocument();
});

it.each([
  ComponentQualifier.Application,
  ComponentQualifier.Project,
  ComponentQualifier.Portfolio,
  ComponentQualifier.SubPortfolio,
])('should render correctly when there are no child components for %s', async (qualifier) => {
  const component = mockComponent({
    ...componentsHandler.findComponentTree('foo')?.component,
    qualifier,
    canBrowseAllChildProjects: true,
  });
  componentsHandler.registerComponentTree({
    component,
    ancestors: [],
    children: [],
  });
  const ui = getPageObject(userEvent.setup());
  renderCode({ component });

  expect(await ui.componentIsEmptyTxt(qualifier).find()).toBeInTheDocument();
});

it.each([ComponentQualifier.Portfolio, ComponentQualifier.SubPortfolio])(
  'should render a warning when not having access to all children for %s',
  async (qualifier) => {
    const ui = getPageObject(userEvent.setup());
    renderCode({
      component: mockComponent({
        ...componentsHandler.findComponentTree('foo')?.component,
        qualifier,
        canBrowseAllChildProjects: false,
      }),
    });

    expect(await ui.notAccessToAllChildrenTxt.find()).toBeInTheDocument();
  },
);

it('should correctly show measures for a project', async () => {
  const component = mockComponent(componentsHandler.findComponentTree('foo')?.component);
  componentsHandler.registerComponentTree({
    component,
    ancestors: [],
    children: [
      {
        component: mockComponent({
          key: 'folderA',
          name: 'folderA',
          qualifier: ComponentQualifier.Directory,
        }),
        ancestors: [component],
        children: [],
      },
      {
        component: mockComponent({
          key: 'index.tsx',
          name: 'index.tsx',
          qualifier: ComponentQualifier.File,
        }),
        ancestors: [component],
        children: [],
      },
    ],
  });
  componentsHandler.registerComponentMeasures({
    foo: { [MetricKey.ncloc]: mockMeasure({ metric: MetricKey.ncloc }) },
    folderA: generateMeasures('2.0'),
    'index.tsx': {},
  });
  const ui = getPageObject(userEvent.setup());
  renderCode();
  await ui.appLoaded(component.name);

  // Folder A
  const folderRow = ui.measureRow(/folderA/);
  [
    [MetricKey.ncloc, '2'],
    [MetricKey.security_issues, '4'],
    [MetricKey.reliability_issues, '4'],
    [MetricKey.maintainability_issues, '4'],
    [MetricKey.security_hotspots, '2'],
    [MetricKey.coverage, '2.0%'],
    [MetricKey.duplicated_lines_density, '2.0%'],
  ].forEach(([domain, value]) => {
    expect(ui.measureValueCell(folderRow, domain, value)).toBeInTheDocument();
  });

  // index.tsx
  const fileRow = ui.measureRow(/index\.tsx/);
  [
    [MetricKey.ncloc, '—'],
    [MetricKey.security_issues, '—'],
    [MetricKey.reliability_issues, '—'],
    [MetricKey.maintainability_issues, '—'],
    [MetricKey.security_hotspots, '—'],
    [MetricKey.coverage, '—'],
    [MetricKey.duplicated_lines_density, '—'],
  ].forEach(([domain, value]) => {
    expect(ui.measureValueCell(fileRow, domain, value)).toBeInTheDocument();
  });
});

it('should correctly show measures for a project when relying on old taxonomy', async () => {
  const component = mockComponent(componentsHandler.findComponentTree('foo')?.component);
  componentsHandler.registerComponentTree({
    component,
    ancestors: [],
    children: [
      {
        component: mockComponent({
          key: 'folderA',
          name: 'folderA',
          qualifier: ComponentQualifier.Directory,
        }),
        ancestors: [component],
        children: [],
      },
      {
        component: mockComponent({
          key: 'index.tsx',
          name: 'index.tsx',
          qualifier: ComponentQualifier.File,
        }),
        ancestors: [component],
        children: [],
      },
    ],
  });
  componentsHandler.registerComponentMeasures({
    foo: { [MetricKey.ncloc]: mockMeasure({ metric: MetricKey.ncloc }) },
    folderA: omit(generateMeasures('2.0'), CCT_SOFTWARE_QUALITY_METRICS),
    'index.tsx': {},
  });
  const ui = getPageObject(userEvent.setup());
  renderCode();
  await ui.appLoaded(component.name);

  // Folder A
  const folderRow = ui.measureRow(/folderA/);
  [
    [MetricKey.ncloc, '2'],
    [MetricKey.security_issues, '2'],
    [MetricKey.reliability_issues, '2'],
    [MetricKey.maintainability_issues, '2'],
    [MetricKey.security_hotspots, '2'],
    [MetricKey.coverage, '2.0%'],
    [MetricKey.duplicated_lines_density, '2.0%'],
  ].forEach(([domain, value]) => {
    expect(ui.measureValueCell(folderRow, domain, value)).toBeInTheDocument();
  });

  // index.tsx
  const fileRow = ui.measureRow(/index\.tsx/);
  [
    [MetricKey.ncloc, '—'],
    [MetricKey.security_issues, '—'],
    [MetricKey.reliability_issues, '—'],
    [MetricKey.maintainability_issues, '—'],
    [MetricKey.security_hotspots, '—'],
    [MetricKey.coverage, '—'],
    [MetricKey.duplicated_lines_density, '—'],
  ].forEach(([domain, value]) => {
    expect(ui.measureValueCell(fileRow, domain, value)).toBeInTheDocument();
  });
});

it('should correctly show new VS overall measures for Portfolios', async () => {
  const component = mockComponent({
    key: 'portfolio',
    name: 'Portfolio',
    qualifier: ComponentQualifier.Portfolio,
    canBrowseAllChildProjects: true,
  });
  componentsHandler.registerComponentTree({
    component,
    ancestors: [],
    children: [
      {
        component: mockComponent({
          analysisDate: '2022-02-01',
          key: 'child1',
          name: 'Child 1',
        }),
        ancestors: [component],
        children: [],
      },
      {
        component: mockComponent({
          key: 'child2',
          name: 'Child 2',
        }),
        ancestors: [component],
        children: [],
      },
    ],
  });
  componentsHandler.registerComponentMeasures({
    portfolio: generateMeasures('1.0', '2.0'),
    child1: generateMeasures('2.0', '3.0'),
    child2: {
      [MetricKey.alert_status]: mockMeasure({
        metric: MetricKey.alert_status,
        value: 'ERROR',
        period: undefined,
      }),
    },
  });
  const ui = getPageObject(userEvent.setup());
  renderCode({ component });
  await ui.appLoaded(component.name);

  // New code measures.
  expect(ui.newCodeBtn.get()).toHaveAttribute('aria-current', 'true');

  // Child 1
  let child1Row = ui.measureRow(/^Child 1/);
  [
    ['Releasability', 'OK'],
    ['security', 'C'],
    ['Reliability', 'C'],
    ['Maintainability', 'C'],
    ['security_review', 'C'],
    ['ncloc', '3'],
    ['last_analysis_date', '2022-02-01'],
  ].forEach(([domain, value]) => {
    expect(ui.measureValueCell(child1Row, domain, value)).toBeInTheDocument();
  });

  // Child 2
  let child2Row = ui.measureRow(/^Child 2/);
  [
    ['Releasability', 'ERROR'],
    ['security', '—'],
    ['Reliability', '—'],
    ['Maintainability', '—'],
    ['security_review', '—'],
    ['ncloc', '—'],
    ['last_analysis_date', '—'],
  ].forEach(([domain, value]) => {
    expect(ui.measureValueCell(child2Row, domain, value)).toBeInTheDocument();
  });

  // Overall code measures
  await ui.showOverallCode();

  // Child 1
  child1Row = ui.measureRow(/^Child 1/);
  [
    ['Releasability', 'OK'],
    ['security', 'B'],
    ['Reliability', 'B'],
    ['Maintainability', 'B'],
    ['security_review', 'B'],
    ['ncloc', '2'],
  ].forEach(([domain, value]) => {
    expect(ui.measureValueCell(child1Row, domain, value)).toBeInTheDocument();
  });

  // Child 2
  child2Row = ui.measureRow(/^Child 2/);
  [
    ['Releasability', 'ERROR'],
    ['security', '—'],
    ['Reliability', '—'],
    ['Maintainability', '—'],
    ['security_review', '—'],
    ['ncloc', '—'],
  ].forEach(([domain, value]) => {
    expect(ui.measureValueCell(child2Row, domain, value)).toBeInTheDocument();
  });
});

it('should render correctly for ipynb files', async () => {
  issuesHandler.setIssueList([JUPYTER_ISSUE]);
  const component = mockComponent({
    ...componentsHandler.findComponentTree('foo')?.component,
    qualifier: ComponentQualifier.Project,
    canBrowseAllChildProjects: true,
  });
  componentsHandler.sourceFiles = [
    {
      component: mockSourceViewerFile('file0.ipynb', 'foo'),
      lines: times(1, (n) =>
        mockSourceLine({
          line: n,
          code: 'function Test() {}',
        }),
      ),
    },
  ];
  componentsHandler.registerComponentTree({
    component,
    ancestors: [],
    children: times(1, (n) => ({
      component: mockComponent({
        key: `foo:file${n}.ipynb`,
        name: `file${n}.ipynb`,
        qualifier: ComponentQualifier.File,
      }),
      ancestors: [component],
      children: [],
    })),
  });
  const ui = getPageObject(userEvent.setup());
  renderCode({ component });

  await ui.appLoaded();

  await ui.clickOnChildComponent(/ipynb$/);

  await ui.clickToggleCode();
  expect(ui.sourceCode.get()).toBeInTheDocument();

  await ui.clickTogglePreview();
  expect(ui.previewToggle.get()).toBeInTheDocument();
  expect(ui.previewToggleOption().get()).toBeChecked();
  expect(ui.previewMarkdown.get()).toBeInTheDocument();
  expect(ui.previewCode.get()).toBeInTheDocument();
  expect(ui.previewOutputImage.get()).toBeInTheDocument();
  expect(ui.previewOutputText.get()).toBeInTheDocument();
  expect(ui.previewOutputStream.get()).toBeInTheDocument();
  expect(ui.previewIssueUnderline.get()).toBeInTheDocument();

  expect(await ui.previewIssueIndicator.find()).toBeInTheDocument();

  await ui.clickIssueIndicator();

  expect(ui.issuesViewPage.get()).toBeInTheDocument();
});

function getPageObject(user: UserEvent) {
  const ui = {
    componentName: (name: string) => byText(name),
    childComponent: (name: string | RegExp) => byRole('cell', { name }),
    searchResult: (name: string | RegExp) => byRole('link', { name }),
    componentIsEmptyTxt: (qualifier: ComponentQualifier) =>
      byText(`code_viewer.no_source_code_displayed_due_to_empty_analysis.${qualifier}`),
    searchInput: byRole('searchbox'),
    previewToggle: byRole('radiogroup'),
    previewToggleOption: (name: string = 'Preview') =>
      byRole('radio', {
        name,
      }),
    noResultsTxt: byText('no_results'),
    sourceCode: byText('function Test() {}'),
    previewCode: byText('numpy', { exact: false }),
    previewIssueUnderline: byTestId('hljs-sonar-underline'),
    previewIssueIndicator: byRole('button', {
      name: 'source_viewer.issues_on_line.multiple_issues_same_category.true.1.issue.clean_code_attribute_category.responsible',
    }),
    issuesViewPage: byText('/project/issues?open=some-issue&id=foo'),
    previewMarkdown: byText('Learning a cosine with keras'),
    previewOutputImage: byRole('img', { name: 'source_viewer.jupyter.output.image' }),
    previewOutputText: byText('[<matplotlib.lines.Line2D at 0x7fb588176b90>]'),
    previewOutputStream: byText('(7500,) (2500,)'),
    notAccessToAllChildrenTxt: byText('code_viewer.not_all_measures_are_shown'),
    showingOutOfTxt: (x: number, y: number) => byText(`x_of_y_shown.${x}.${y}`),
    newCodeBtn: byRole('radio', { name: 'projects.view.new_code' }),
    overallCodeBtn: byRole('radio', { name: 'projects.view.overall_code' }),
    measureRow: (name: string | RegExp) => byLabelText(name),
    measureValueCell: (row: QuerySelector, name: string, value: string) => {
      const i = Array.from(screen.getAllByRole('columnheader')).findIndex((c) =>
        c.textContent?.includes(name),
      );
      if (i < 0) {
        // eslint-disable-next-line testing-library/no-debugging-utils
        screen.debug(screen.getByRole('table'), 40000);
        throw new Error(`Couldn't locate column with header ${name}`);
      }

      const cell = row.byRole('cell').getAll().at(i);

      if (cell === undefined) {
        throw new Error(`Couldn't locate cell with value ${value} for header ${name}`);
      }

      return within(cell).getByText(value);
    },
  };

  return {
    ...ui,
    async searchForComponent(text: string) {
      await user.type(ui.searchInput.get(), text);
    },
    async clearSearch() {
      await user.clear(ui.searchInput.get());
    },
    async clickOnChildComponent(name: string | RegExp) {
      await user.click(screen.getByRole('link', { name }));
    },
    async clickToggleCode() {
      await user.click(ui.previewToggleOption('Code').get());
    },
    async clickTogglePreview() {
      await user.click(ui.previewToggleOption('Preview').get());
    },
    async clickIssueIndicator() {
      await user.click(ui.previewIssueIndicator.get());
    },
    async appLoaded(name = 'Foo') {
      await waitFor(() => {
        expect(ui.componentName(name).get()).toBeInTheDocument();
      });
    },
    async clickOnBreadcrumb(name: string | RegExp) {
      await user.click(screen.getByRole('link', { name }));
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
    async showOverallCode() {
      await user.click(ui.overallCodeBtn.get());
    },
  };
}

function generateMeasures(overallValue = '1.0', newValue = '2.0') {
  return keyBy(
    [
      ...[
        MetricKey.security_issues,
        MetricKey.reliability_issues,
        MetricKey.maintainability_issues,
      ].map((metric) =>
        mockMeasure({ metric, value: JSON.stringify({ total: 4 }), period: undefined }),
      ),
      ...[
        MetricKey.ncloc,
        MetricKey.new_lines,
        MetricKey.bugs,
        MetricKey.new_bugs,
        MetricKey.vulnerabilities,
        MetricKey.new_vulnerabilities,
        MetricKey.code_smells,
        MetricKey.new_code_smells,
        MetricKey.security_hotspots,
        MetricKey.new_security_hotspots,
        MetricKey.coverage,
        MetricKey.new_coverage,
        MetricKey.duplicated_lines_density,
        MetricKey.new_duplicated_lines_density,
        MetricKey.releasability_rating,
        MetricKey.reliability_rating,
        MetricKey.new_reliability_rating,
        MetricKey.sqale_rating,
        MetricKey.new_maintainability_rating,
        MetricKey.security_rating,
        MetricKey.new_security_rating,
        MetricKey.security_review_rating,
        MetricKey.new_security_review_rating,
      ].map((metric) =>
        isDiffMetric(metric)
          ? mockMeasure({ metric, period: { index: 1, value: newValue } })
          : mockMeasure({ metric, value: overallValue, period: undefined }),
      ),
      mockMeasure({
        metric: MetricKey.alert_status,
        value: overallValue === '1.0' || overallValue === '2.0' ? 'OK' : 'ERROR',
        period: undefined,
      }),
    ],
    'metric',
  );
}

function renderCode({
  component = componentsHandler.findComponentTree('foo')?.component,
  navigateTo,
}: { component?: Component; navigateTo?: string } = {}) {
  return renderAppWithComponentContext('code', routes, { navigateTo }, { component });
}
