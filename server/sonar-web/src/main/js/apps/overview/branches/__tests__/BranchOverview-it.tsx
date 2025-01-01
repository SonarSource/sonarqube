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

import { screen, waitFor } from '@testing-library/react';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { MetricKey } from '~sonar-aligned/types/metrics';
import AlmSettingsServiceMock from '../../../../api/mocks/AlmSettingsServiceMock';
import ApplicationServiceMock from '../../../../api/mocks/ApplicationServiceMock';
import BranchesServiceMock from '../../../../api/mocks/BranchesServiceMock';
import { MeasuresServiceMock } from '../../../../api/mocks/MeasuresServiceMock';
import MessagesServiceMock from '../../../../api/mocks/MessagesServiceMock';
import { ModeServiceMock } from '../../../../api/mocks/ModeServiceMock';
import { ProjectActivityServiceMock } from '../../../../api/mocks/ProjectActivityServiceMock';
import { QualityGatesServiceMock } from '../../../../api/mocks/QualityGatesServiceMock';
import { TimeMachineServiceMock } from '../../../../api/mocks/TimeMachineServiceMock';
import UsersServiceMock from '../../../../api/mocks/UsersServiceMock';
import { PARENT_COMPONENT_KEY } from '../../../../api/mocks/data/ids';
import { getProjectActivity } from '../../../../api/projectActivity';
import { getQualityGateProjectStatus } from '../../../../api/quality-gates';
import CurrentUserContextProvider from '../../../../app/components/current-user/CurrentUserContextProvider';
import { Header } from '../../../../app/components/nav/component/Header';
import { parseDate } from '../../../../helpers/dates';
import { mockMainBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockAnalysis, mockAnalysisEvent } from '../../../../helpers/mocks/project-activity';
import { mockQualityGateProjectStatus } from '../../../../helpers/mocks/quality-gates';
import { mockLoggedInUser, mockMeasure, mockPaging } from '../../../../helpers/testMocks';
import { renderComponent, RenderContext } from '../../../../helpers/testReactTestingUtils';
import { ComponentPropsType } from '../../../../helpers/testUtils';
import { SoftwareQuality } from '../../../../types/clean-code-taxonomy';
import { Mode } from '../../../../types/mode';
import { ProjectAnalysisEventCategory } from '../../../../types/project-activity';
import { CaycStatus } from '../../../../types/types';
import BranchOverview, { NO_CI_DETECTED } from '../BranchOverview';
import { getPageObjects } from '../test-utils';

const almHandler = new AlmSettingsServiceMock();
const modeHandler = new ModeServiceMock();
let branchesHandler: BranchesServiceMock;
let measuresHandler: MeasuresServiceMock;
let applicationHandler: ApplicationServiceMock;
let projectActivityHandler: ProjectActivityServiceMock;
let usersHandler: UsersServiceMock;
let timeMarchineHandler: TimeMachineServiceMock;
let qualityGatesHandler: QualityGatesServiceMock;
let messageshandler: MessagesServiceMock;

jest.mock('../../../../api/ce', () => ({
  getAnalysisStatus: jest.fn().mockResolvedValue({ component: { warnings: [] } }),
}));

beforeAll(() => {
  branchesHandler = new BranchesServiceMock();
  measuresHandler = new MeasuresServiceMock();
  applicationHandler = new ApplicationServiceMock();
  projectActivityHandler = new ProjectActivityServiceMock();
  usersHandler = new UsersServiceMock();
  projectActivityHandler.setAnalysesList([
    mockAnalysis({ key: 'a1', detectedCI: 'Cirrus CI' }),
    mockAnalysis({ key: 'a2' }),
    mockAnalysis({ key: 'a3' }),
    mockAnalysis({ key: 'a4' }),
    mockAnalysis({ key: 'a5' }),
  ]);
  timeMarchineHandler = new TimeMachineServiceMock();
  timeMarchineHandler.setMeasureHistory([
    { metric: MetricKey.bugs, history: [{ date: parseDate('2019-01-05'), value: '2.0' }] },
    { metric: MetricKey.vulnerabilities, history: [{ date: parseDate('2019-01-05'), value: '0' }] },
    { metric: MetricKey.sqale_index, history: [{ date: parseDate('2019-01-01'), value: '1.0' }] },
    {
      metric: MetricKey.duplicated_lines_density,
      history: [{ date: parseDate('2019-01-02'), value: '1.0' }],
    },
    { metric: MetricKey.ncloc, history: [{ date: parseDate('2019-01-03'), value: '10000' }] },
    { metric: MetricKey.coverage, history: [{ date: parseDate('2019-01-04'), value: '95.5' }] },
  ]);
  qualityGatesHandler = new QualityGatesServiceMock();
  qualityGatesHandler.setQualityGateProjectStatus({
    status: 'ERROR',
    conditions: [
      {
        actualValue: '2',
        comparator: 'GT',
        errorThreshold: '1',
        metricKey: MetricKey.new_reliability_rating,
        periodIndex: 1,
        status: 'ERROR',
      },
      {
        actualValue: '5',
        comparator: 'GT',
        errorThreshold: '2.0',
        metricKey: MetricKey.bugs,
        periodIndex: 0,
        status: 'ERROR',
      },
      {
        actualValue: '2',
        comparator: 'GT',
        errorThreshold: '1.0',
        metricKey: 'unknown_metric',
        periodIndex: 0,
        status: 'ERROR',
      },
    ],
  });
  messageshandler = new MessagesServiceMock();
});

afterEach(() => {
  jest.clearAllMocks();
  branchesHandler.reset();
  measuresHandler.reset();
  applicationHandler.reset();
  projectActivityHandler.reset();
  usersHandler.reset();
  timeMarchineHandler.reset();
  qualityGatesHandler.reset();
  almHandler.reset();
  modeHandler.reset();
  messageshandler.reset();
});

describe('project overview', () => {
  it('should render a successful quality gate', async () => {
    qualityGatesHandler.setQualityGateProjectStatus(
      mockQualityGateProjectStatus({
        status: 'OK',
      }),
    );
    const { user } = getPageObjects();
    renderBranchOverview();

    // Meta info
    expect(await screen.findByText('master')).toBeInTheDocument();
    expect(screen.getByText('version-1.0')).toBeInTheDocument();

    // QG panel
    expect(screen.getByText('metric.level.OK')).toBeInTheDocument();
    expect(
      screen.queryByText('overview.quality_gate.conditions.cayc.warning.title.TRK'),
    ).not.toBeInTheDocument();

    // Measures panel
    expect(screen.getByText('overview.new_issues')).toBeInTheDocument();
    expect(
      byRole('link', {
        name: 'overview.see_more_details_on_x_of_y.1.metric.new_accepted_issues.name',
      }).get(),
    ).toBeInTheDocument();
    expect(byText('overview.accepted_issues.help').get()).toBeVisible();

    await user.click(byRole('tab', { name: 'overview.overall_code' }).get());

    expect(byText('overview.accepted_issues.help').get()).toBeVisible();
  });

  it('should show a successful non-compliant QG', async () => {
    jest
      .mocked(getQualityGateProjectStatus)
      .mockResolvedValueOnce(
        mockQualityGateProjectStatus({ status: 'OK', caycStatus: CaycStatus.NonCompliant }),
      );

    renderBranchOverview();

    expect(await screen.findByText('metric.level.OK')).toBeInTheDocument();
    expect(
      screen.queryByText('overview.quality_gate.conditions.cayc.warning.title.TRK'),
    ).not.toBeInTheDocument();
  });

  it('should show a successful non-compliant QG as admin', async () => {
    jest
      .mocked(getQualityGateProjectStatus)
      .mockResolvedValueOnce(
        mockQualityGateProjectStatus({ status: 'OK', caycStatus: CaycStatus.NonCompliant }),
      );
    qualityGatesHandler.setIsAdmin(true);
    qualityGatesHandler.setGetGateForProjectName('Non Cayc QG');

    renderBranchOverview();

    await screen.findByText('metric.level.OK');
    expect(
      await screen.findByText('overview.quality_gate.conditions.cayc.warning.title.TRK'),
    ).toBeInTheDocument();
  });

  it('should show a failed QG', async () => {
    qualityGatesHandler.setQualityGateProjectStatus(
      mockQualityGateProjectStatus({
        status: 'ERROR',
        conditions: [
          {
            actualValue: '2',
            comparator: 'GT',
            errorThreshold: '1',
            metricKey: MetricKey.new_reliability_rating,
            periodIndex: 1,
            status: 'ERROR',
          },
          {
            actualValue: '5',
            comparator: 'GT',
            errorThreshold: '2.0',
            metricKey: MetricKey.bugs,
            periodIndex: 0,
            status: 'ERROR',
          },
          {
            actualValue: '10',
            comparator: 'PT',
            errorThreshold: '85',
            metricKey: MetricKey.new_coverage,
            periodIndex: 0,
            status: 'ERROR',
          },
          {
            actualValue: '5',
            comparator: 'GT',
            errorThreshold: '2.0',
            metricKey: MetricKey.new_security_hotspots_reviewed,
            periodIndex: 0,
            status: 'ERROR',
          },
          {
            actualValue: '5',
            comparator: 'GT',
            errorThreshold: '2.0',
            metricKey: MetricKey.new_violations,
            periodIndex: 0,
            status: 'ERROR',
          },
          {
            actualValue: '2',
            comparator: 'GT',
            errorThreshold: '1.0',
            metricKey: 'unknown_metric',
            periodIndex: 0,
            status: 'ERROR',
          },
        ],
      }),
    );

    renderBranchOverview();

    expect(await screen.findByText('metric.level.ERROR')).toBeInTheDocument();
    expect(screen.getAllByText(/overview.quality_gate.required_x/)).toHaveLength(3);
    expect(
      screen.getByRole('link', {
        name: '1 1 new_security_hotspots_reviewed quality_gates.operator.GT 2',
      }),
    ).toHaveAttribute('href', '/security_hotspots?id=foo&inNewCodePeriod=true');
  });

  it('should correctly show a project as empty', async () => {
    measuresHandler.registerComponentMeasures({});

    renderBranchOverview();

    expect(await screen.findByText('overview.project.main_branch_empty')).toBeInTheDocument();
  });

  // eslint-disable-next-line jest/expect-expect
  it('should render software impact measure cards', async () => {
    qualityGatesHandler.setQualityGateProjectStatus(
      mockQualityGateProjectStatus({
        status: 'ERROR',
        conditions: [
          {
            actualValue: '2',
            comparator: 'GT',
            errorThreshold: '1',
            metricKey: MetricKey.reliability_rating,
            periodIndex: 1,
            status: 'ERROR',
          },
        ],
      }),
    );
    const { user, ui } = getPageObjects();
    renderBranchOverview();

    await user.click(await ui.overallCodeButton.find());

    ui.expectSoftwareImpactMeasureCard(SoftwareQuality.Security, 'B', 1);
    await ui.expectSoftwareImpactMeasureCardRatingTooltip(
      SoftwareQuality.Security,
      'B',
      'overview.measures.software_impact.improve_rating_tooltip.software_quality.SECURITY.software_quality.security.B.overview.measures.software_impact.severity.LOW.improve_tooltip',
    );

    ui.expectSoftwareImpactMeasureCard(SoftwareQuality.Reliability, 'A', 3, undefined, true);
    await ui.expectSoftwareImpactMeasureCardRatingTooltip(
      SoftwareQuality.Reliability,
      'A',
      'overview.measures.software_impact.improve_rating_tooltip.A.software_quality.RELIABILITY.software_quality.reliability.A.overview.measures.software_impact.severity.LOW.improve_tooltip',
    );

    ui.expectSoftwareImpactMeasureCard(SoftwareQuality.Maintainability, 'D', 2);
    await ui.expectSoftwareImpactMeasureCardRatingTooltip(
      SoftwareQuality.Maintainability,
      'D',
      'overview.measures.software_impact.improve_rating_tooltip.MAINTAINABILITY.software_quality.MAINTAINABILITY.software_quality.maintainability.D.overview.measures.software_impact.severity.HIGH.improve_tooltip',
    );
  });

  // eslint-disable-next-line jest/expect-expect
  it('should render overall tab without branch specified', async () => {
    const { user, ui } = getPageObjects();
    renderBranchOverview({ branch: undefined });

    await user.click(await ui.overallCodeButton.find());

    ui.expectSoftwareImpactMeasureCard(SoftwareQuality.Maintainability, 'D', 2, '');
  });

  it('should render old measures if software impact are missing', async () => {
    // Make as if new analysis after upgrade is missing
    measuresHandler.deleteComponentMeasure(
      'foo',
      MetricKey.software_quality_maintainability_issues,
    );
    measuresHandler.deleteComponentMeasure(
      'foo',
      MetricKey.software_quality_maintainability_rating,
    );
    measuresHandler.deleteComponentMeasure('foo', MetricKey.software_quality_security_issues);
    measuresHandler.deleteComponentMeasure('foo', MetricKey.software_quality_security_rating);
    measuresHandler.deleteComponentMeasure('foo', MetricKey.software_quality_reliability_issues);
    measuresHandler.deleteComponentMeasure('foo', MetricKey.software_quality_reliability_rating);

    const { user, ui } = getPageObjects();
    renderBranchOverview();

    await user.click(await ui.overallCodeButton.find());

    expect(await ui.softwareImpactMeasureCard(SoftwareQuality.Security).find()).toBeInTheDocument();

    ui.expectSoftwareImpactMeasureCard(SoftwareQuality.Security);
    ui.expectSoftwareImpactMeasureCardToHaveOldMeasures(
      SoftwareQuality.Security,
      'B',
      2,
      'VULNERABILITY',
    );

    ui.expectSoftwareImpactMeasureCard(SoftwareQuality.Reliability);
    ui.expectSoftwareImpactMeasureCardToHaveOldMeasures(SoftwareQuality.Reliability, 'A', 0, 'BUG');

    ui.expectSoftwareImpactMeasureCard(SoftwareQuality.Maintainability);
    ui.expectSoftwareImpactMeasureCardToHaveOldMeasures(
      SoftwareQuality.Maintainability,
      'E',
      9,
      'CODE_SMELL',
    );
  });

  it('should render missing software impact measure cards if both software qualities and old measures are missing', async () => {
    // Make as if no measures at all
    measuresHandler.deleteComponentMeasure(
      'foo',
      MetricKey.software_quality_maintainability_issues,
    );
    measuresHandler.deleteComponentMeasure('foo', MetricKey.code_smells);
    measuresHandler.deleteComponentMeasure(
      'foo',
      MetricKey.software_quality_maintainability_rating,
    );

    measuresHandler.deleteComponentMeasure('foo', MetricKey.software_quality_security_issues);
    measuresHandler.deleteComponentMeasure('foo', MetricKey.vulnerabilities);
    measuresHandler.deleteComponentMeasure('foo', MetricKey.software_quality_security_rating);

    measuresHandler.deleteComponentMeasure('foo', MetricKey.software_quality_reliability_issues);
    measuresHandler.deleteComponentMeasure('foo', MetricKey.bugs);
    measuresHandler.deleteComponentMeasure('foo', MetricKey.software_quality_reliability_rating);

    const { user, ui } = getPageObjects();
    renderBranchOverview();

    await user.click(await ui.overallCodeButton.find());

    expect(await ui.softwareImpactMeasureCard(SoftwareQuality.Security).find()).toBeInTheDocument();

    expect(byText('-', { exact: true }).getAll()).toHaveLength(3);

    ui.expectSoftwareImpactMeasureCard(SoftwareQuality.Security);
    expect(
      ui.softwareImpactMeasureCardRating(SoftwareQuality.Security, 'B').get(),
    ).toBeInTheDocument();

    ui.expectSoftwareImpactMeasureCard(SoftwareQuality.Reliability);
    expect(
      ui.softwareImpactMeasureCardRating(SoftwareQuality.Reliability, 'A').get(),
    ).toBeInTheDocument();

    ui.expectSoftwareImpactMeasureCard(SoftwareQuality.Maintainability);
    expect(
      ui.softwareImpactMeasureCardRating(SoftwareQuality.Maintainability, 'E').get(),
    ).toBeInTheDocument();
  });

  it.each([
    [MetricKey.software_quality_security_issues],
    [MetricKey.software_quality_reliability_issues],
    [MetricKey.software_quality_maintainability_issues],
  ])(
    'should display info about missing analysis if a project is not computed for %s',
    async (missingMetricKey) => {
      measuresHandler.deleteComponentMeasure('foo', missingMetricKey);
      const { user, ui } = getPageObjects();
      renderBranchOverview();

      await user.click(await ui.overallCodeButton.find());

      expect(
        await ui.softwareImpactMeasureCard(SoftwareQuality.Security).find(),
      ).toBeInTheDocument();

      await user.click(await ui.overallCodeButton.find());

      expect(await screen.findByText('overview.missing_project_dataTRK')).toBeInTheDocument();
    },
  );

  it('should display info about missing analysis if a project did not compute ratings', async () => {
    measuresHandler.deleteComponentMeasure('foo', MetricKey.software_quality_security_rating);
    measuresHandler.deleteComponentMeasure(
      'foo',
      MetricKey.software_quality_maintainability_rating,
    );
    measuresHandler.deleteComponentMeasure('foo', MetricKey.software_quality_reliability_rating);
    const { user, ui } = getPageObjects();
    renderBranchOverview();

    await user.click(await ui.overallCodeButton.find());

    expect(await ui.softwareImpactMeasureCard(SoftwareQuality.Security).find()).toBeInTheDocument();

    await user.click(await ui.overallCodeButton.find());

    expect(await screen.findByText('overview.missing_project_dataTRK')).toBeInTheDocument();
    ui.expectSoftwareImpactMeasureCard(SoftwareQuality.Security);
    expect(
      ui.softwareImpactMeasureCardRating(SoftwareQuality.Security, 'B').get(),
    ).toBeInTheDocument();

    ui.expectSoftwareImpactMeasureCard(SoftwareQuality.Reliability);
    expect(
      ui.softwareImpactMeasureCardRating(SoftwareQuality.Reliability, 'A').get(),
    ).toBeInTheDocument();

    ui.expectSoftwareImpactMeasureCard(SoftwareQuality.Maintainability);
    expect(
      ui.softwareImpactMeasureCardRating(SoftwareQuality.Maintainability, 'E').get(),
    ).toBeInTheDocument();
  });

  it('should display old measures if in legacy mode', async () => {
    modeHandler.setMode(Mode.Standard);
    const { user, ui } = getPageObjects();
    renderBranchOverview();

    await user.click(await ui.overallCodeButton.find());

    expect(await ui.softwareImpactMeasureCard(SoftwareQuality.Security).find()).toBeInTheDocument();

    await user.click(await ui.overallCodeButton.find());

    expect(await ui.softwareImpactMeasureCard(SoftwareQuality.Security).find()).toBeInTheDocument();

    ui.expectSoftwareImpactMeasureCard(SoftwareQuality.Security);
    ui.expectSoftwareImpactMeasureCardToHaveOldMeasures(
      SoftwareQuality.Security,
      'B',
      2,
      'VULNERABILITY',
    );

    ui.expectSoftwareImpactMeasureCard(SoftwareQuality.Reliability);
    ui.expectSoftwareImpactMeasureCardToHaveOldMeasures(SoftwareQuality.Reliability, 'A', 0, 'BUG');

    ui.expectSoftwareImpactMeasureCard(SoftwareQuality.Maintainability);
    ui.expectSoftwareImpactMeasureCardToHaveOldMeasures(
      SoftwareQuality.Maintainability,
      'E',
      9,
      'CODE_SMELL',
    );
  });

  it('should not show analysis is missing message in legacy mode', async () => {
    measuresHandler.deleteComponentMeasure('foo', MetricKey.software_quality_security_rating);
    measuresHandler.deleteComponentMeasure(
      'foo',
      MetricKey.software_quality_maintainability_rating,
    );
    measuresHandler.deleteComponentMeasure('foo', MetricKey.software_quality_reliability_rating);
    modeHandler.setMode(Mode.Standard);
    const { user, ui } = getPageObjects();
    renderBranchOverview();

    await user.click(await ui.overallCodeButton.find());

    expect(await ui.softwareImpactMeasureCard(SoftwareQuality.Security).find()).toBeInTheDocument();

    await user.click(await ui.overallCodeButton.find());

    expect(await ui.softwareImpactMeasureCard(SoftwareQuality.Security).find()).toBeInTheDocument();

    expect(screen.queryByText('overview.missing_project_dataTRK')).not.toBeInTheDocument();
  });

  it('should dismiss CaYC promoted section', async () => {
    qualityGatesHandler.setQualityGateProjectStatus(
      mockQualityGateProjectStatus({
        status: 'OK',
      }),
    );
    const { user } = getPageObjects();
    renderBranchOverview();

    // Meta info
    expect(await byText('overview.promoted_section.title').find()).toBeInTheDocument();

    await user.click(
      byRole('button', { name: 'overview.promoted_section.button_secondary' }).get(),
    );

    expect(byText('overview.promoted_section.title').query()).not.toBeInTheDocument();

    expect(byText('guiding.replay_tour_button.1.title').get()).toBeInTheDocument();
  });

  it('should show CaYC tour', async () => {
    qualityGatesHandler.setQualityGateProjectStatus(
      mockQualityGateProjectStatus({
        status: 'OK',
      }),
    );
    const { user } = getPageObjects();
    renderBranchOverview();

    expect(await byText('overview.promoted_section.title').find()).toBeInTheDocument();

    await user.click(byRole('button', { name: 'overview.promoted_section.button_primary' }).get());

    expect(byText('overview.promoted_section.title').query()).not.toBeInTheDocument();

    expect(await byText('guiding.cayc_promotion.1.title').find()).toBeInTheDocument();

    await user.click(byRole('button', { name: 'next' }).get());

    expect(byText('guiding.cayc_promotion.2.title').get()).toBeInTheDocument();

    await user.click(byRole('button', { name: 'next' }).get());

    expect(byText('guiding.cayc_promotion.3.title').get()).toBeInTheDocument();

    await user.click(await byRole('button', { name: 'next' }).find());

    expect(byText('guiding.cayc_promotion.4.title').get()).toBeInTheDocument();

    await user.click(byRole('button', { name: 'complete' }).get());

    expect(byText('guiding.replay_tour_button.tour_completed.1.title').get()).toBeInTheDocument();
  });
});

describe('application overview', () => {
  const component = mockComponent({
    key: PARENT_COMPONENT_KEY,
    name: 'FooApp',
    qualifier: ComponentQualifier.Application,
    breadcrumbs: [
      {
        key: PARENT_COMPONENT_KEY,
        name: 'FooApp',
        qualifier: ComponentQualifier.Application,
      },
    ],
  });

  beforeEach(() => {
    // We mock this application structure:
    // App (foo)
    // -- Project 1 (1) - QG OK
    // -- Project 2 (2) - QG OK
    // -- Project 3 (3) - QG OK
    // -- Project 4 (4) - 1 failed condition on new code (new_violations)
    const components = Array.from({ length: 4 }).map((_, i) =>
      mockComponent({
        key: (i + 1).toString(),
        name: (i + 1).toString(),
        breadcrumbs: [
          ...component.breadcrumbs,
          {
            key: (i + 1).toString(),
            name: (i + 1).toString(),
            qualifier: ComponentQualifier.Project,
          },
        ],
      }),
    );
    measuresHandler.setComponents({
      component,
      ancestors: [],
      children: components.map((component) => ({
        component,
        ancestors: [component],
        children: [],
      })),
    });
    const componentMeasures = measuresHandler.getComponentMeasures();
    componentMeasures['4'] = {
      [MetricKey.new_violations]: mockMeasure({
        metric: MetricKey.new_violations,
      }),
    };
    qualityGatesHandler.setApplicationQualityGateStatus({
      projects: [
        {
          key: '1',
          name: 'first project',
          conditions: [],
          caycStatus: CaycStatus.NonCompliant,
          status: 'OK',
        },
        {
          key: '2',
          name: 'second project',
          conditions: [],
          caycStatus: CaycStatus.Compliant,
          status: 'OK',
        },
        {
          key: '3',
          name: 'third project',
          conditions: [],
          caycStatus: CaycStatus.NonCompliant,
          status: 'OK',
        },
        {
          key: '4',
          name: 'fourth project',
          conditions: [
            {
              comparator: 'GT',
              metric: MetricKey.new_violations,
              status: 'ERROR',
              value: '3',
              errorThreshold: '0',
            },
          ],
          caycStatus: CaycStatus.NonCompliant,
          status: 'ERROR',
        },
      ],
    });
  });

  it('should show failed conditions for child projects', async () => {
    renderBranchOverview({ component });

    expect(await screen.findByText('metric.level.ERROR')).toBeInTheDocument();
    expect(
      byRole('button', {
        name: 'overview.quality_gate.hide_project_conditions_x.fourth project',
      }).get(),
    ).toBeInTheDocument();
    expect(byText(/quality_gates.conditions.x_conditions_failed/).get()).toBeInTheDocument();
    expect(
      byRole('link', { name: '1 1 new_violations quality_gates.operator.GT 0' }).get(),
    ).toBeInTheDocument();
  });

  it("should show projects that don't have a compliant quality gate", async () => {
    renderBranchOverview({ component });
    expect(
      await screen.findByText('overview.quality_gate.conditions.cayc.details.APP'),
    ).toBeInTheDocument();
    expect(screen.getByText('first project')).toBeInTheDocument();
    expect(screen.queryByText('second project')).not.toBeInTheDocument();
    expect(screen.getByText('third project')).toBeInTheDocument();
  });

  it('should correctly show an app as empty', async () => {
    measuresHandler.registerComponentMeasures({});

    renderBranchOverview({ component });

    expect(await screen.findByText('portfolio.app.empty')).toBeInTheDocument();
  });

  it.each([
    [MetricKey.software_quality_security_issues],
    [MetricKey.software_quality_reliability_issues],
    [MetricKey.software_quality_maintainability_issues],
  ])(
    'should ask to reanalyze all projects if a project is not computed for %s',
    async (missingMetricKey) => {
      const { ui, user } = getPageObjects();

      measuresHandler.deleteComponentMeasure('foo', missingMetricKey as MetricKey);
      renderBranchOverview({ component });
      await user.click(await ui.overallCodeButton.find());

      expect(await screen.findByText('overview.missing_project_dataAPP')).toBeInTheDocument();
    },
  );
});

it.each([
  ['no analysis', [], true],
  ['1 analysis, no CI data', [mockAnalysis()], false],
  ['1 analysis, no CI detected', [mockAnalysis({ detectedCI: NO_CI_DETECTED })], false],
  ['1 analysis, CI detected', [mockAnalysis({ detectedCI: 'Cirrus CI' })], true],
])(
  "should correctly flag a project that wasn't analyzed using a CI (%s)",
  async (_, analyses, expected) => {
    jest.mocked(getProjectActivity).mockResolvedValue({ analyses, paging: mockPaging() });

    renderBranchOverview();

    // wait for loading
    await screen.findByText('overview.quality_gate');

    await waitFor(() => {
      expect(screen.queryByText('overview.project.next_steps.set_up_ci') === null).toBe(expected);
    });
  },
);

it.each([
  [
    'no upgrade event',
    [
      mockAnalysis({
        events: [mockAnalysisEvent({ category: ProjectAnalysisEventCategory.Other })],
      }),
    ],
    false,
  ],
  [
    'upgrade event too old',
    [
      mockAnalysis({
        date: '2023-04-02T12:10:30+0200',
        events: [mockAnalysisEvent({ category: ProjectAnalysisEventCategory.SqUpgrade })],
      }),
    ],
    false,
  ],
  [
    'upgrade event too far down in the list',
    [
      mockAnalysis({
        key: 'a1',
        date: '2023-04-13T08:10:30+0200',
      }),
      mockAnalysis({
        key: 'a2',
        date: '2023-04-13T09:10:30+0200',
      }),
      mockAnalysis({
        key: 'a3',
        date: '2023-04-13T10:10:30+0200',
      }),
      mockAnalysis({
        key: 'a4',
        date: '2023-04-13T11:10:30+0200',
      }),
      mockAnalysis({
        key: 'a5',
        date: '2023-04-13T12:10:30+0200',
        events: [mockAnalysisEvent({ category: ProjectAnalysisEventCategory.SqUpgrade })],
      }),
    ],
    false,
  ],
  [
    'upgrade event without QP update event',
    [
      mockAnalysis({
        date: '2023-04-13T12:10:30+0200',
        events: [mockAnalysisEvent({ category: ProjectAnalysisEventCategory.SqUpgrade })],
      }),
    ],
    false,
  ],
  [
    'upgrade event with QP update event',
    [
      mockAnalysis({
        date: '2023-04-13T12:10:30+0200',
        events: [
          mockAnalysisEvent({ category: ProjectAnalysisEventCategory.SqUpgrade }),
          mockAnalysisEvent({ category: ProjectAnalysisEventCategory.QualityProfile }),
        ],
      }),
    ],
    true,
  ],
])(
  'should correctly display message about SQ upgrade updating QPs',
  async (_, analyses, expected) => {
    jest.useFakeTimers({
      advanceTimers: true,
      now: new Date('2023-04-25T12:00:00+0200'),
    });

    jest.mocked(getProjectActivity).mockResolvedValue({
      analyses,
      paging: mockPaging(),
    });

    const { user, ui } = getPageObjects();
    renderBranchOverview();

    await user.click(await ui.overallCodeButton.find());

    expect(await byText('overview.quality_gate').find()).toBeInTheDocument();

    await waitFor(() =>
      expect(
        screen.queryByText(/overview.quality_profiles_update_after_sq_upgrade.message/) !== null,
      ).toBe(expected),
    );

    jest.useRealTimers();
  },
);

function renderBranchOverview(
  props: Partial<ComponentPropsType<typeof BranchOverview>> = {},
  context: RenderContext = {},
) {
  const user = mockLoggedInUser();
  const component = mockComponent({
    breadcrumbs: [mockComponent({ key: 'foo' })],
    key: 'foo',
    name: 'Foo',
    version: 'version-1.0',
  });
  return renderComponent(
    <CurrentUserContextProvider currentUser={user}>
      <Header component={component} currentUser={user} />
      <BranchOverview branch={mockMainBranch()} component={component} {...props} />
    </CurrentUserContextProvider>,
    '/',
    context,
  );
}
