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
import { waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import AlmSettingsServiceMock from '../../../../api/mocks/AlmSettingsServiceMock';
import UserTokensMock from '../../../../api/mocks/UserTokensMock';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockLanguage } from '../../../../helpers/testMocks';
import { RenderContext, renderApp } from '../../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../../helpers/testSelector';
import { AlmKeys } from '../../../../types/alm-settings';
import { Feature } from '../../../../types/features';
import {
  getCopyToClipboardValue,
  getTutorialActionButtons,
  getTutorialBuildButtons,
} from '../../test-utils';
import { GradleBuildDSL } from '../../types';
import JenkinsTutorial, { JenkinsTutorialProps } from '../JenkinsTutorial';

jest.mock('../../../../api/settings', () => ({
  getAllValues: jest.fn().mockResolvedValue([]),
}));

const tokenMock = new UserTokensMock();
const almMock = new AlmSettingsServiceMock();

afterEach(() => {
  tokenMock.reset();
  almMock.reset();
});

const ui = {
  devopsPlatformTitle: byRole('heading', {
    name: 'onboarding.tutorial.with.jenkins.alm_selection.title',
  }),
  devopsPlatformButton: (alm: AlmKeys) => byRole('radio', { name: `alm.${alm}.long` }),
  prerequisitesTitle: byRole('heading', { name: 'onboarding.tutorial.with.jenkins.prereqs.title' }),
  branchSourcePluginBulletPoint: (alm: AlmKeys) =>
    byText(`onboarding.tutorial.with.jenkins.prereqs.plugins.branch_source.${alm}`),
  multiBranchStepTitle: byRole('heading', {
    name: 'onboarding.tutorial.with.jenkins.multi_branch_pipeline.title',
  }),
  multiBranchPipelineSecondListItem: (alm: AlmKeys) =>
    byText(`onboarding.tutorial.with.jenkins.multi_branch_pipeline.step2.${alm}.sentence`),
  pipelineStepTitle: byRole('heading', { name: 'onboarding.tutorial.with.jenkins.pipeline.title' }),
  pipelineIntroText: byText('onboarding.tutorial.with.jenkins.pipeline.intro'),
  webhookStepTitle: (alm: AlmKeys) =>
    byRole('heading', {
      name: `onboarding.tutorial.with.jenkins.webhook.${alm}.title`,
    }),
  webhookStepIntroSentence: byText('onboarding.tutorial.with.jenkins.webhook.intro.sentence'),
  webhookGHLink: byRole('link', {
    name: 'onboarding.tutorial.with.jenkins.webhook.github.step1.link',
  }),
  webhookAlmLink: (alm: AlmKeys) =>
    byRole('link', {
      name: new RegExp(`onboarding.tutorial.with.jenkins.webhook.${alm}.step1.link`),
    }),
  jenkinsStepTitle: byRole('heading', {
    name: 'onboarding.tutorial.with.jenkins.jenkinsfile.title',
  }),
  allSetSentence: byText('onboarding.tutorial.ci_outro.done'),
  ...getTutorialActionButtons(),
  ...getTutorialBuildButtons(),
};

it.each([AlmKeys.BitbucketCloud, AlmKeys.BitbucketServer, AlmKeys.GitHub, AlmKeys.GitLab])(
  '%s: can select devops platform and complete all the steps with copying code snippets',
  async (alm: AlmKeys) => {
    const user = userEvent.setup();
    renderJenkinsTutorial();

    expect(await ui.devopsPlatformTitle.find()).toBeInTheDocument();

    // 1. Select platform and go to prerequisites step
    await user.click(ui.devopsPlatformButton(alm).get());

    // 2. Prerequisites
    expect(ui.branchSourcePluginBulletPoint(alm).get()).toBeInTheDocument();

    // 3. Multibranch Pipeline Job
    expect(ui.multiBranchPipelineSecondListItem(alm).get()).toBeInTheDocument();
    expect(getCopyToClipboardValue(0, 'Copy')).toMatchSnapshot(`ref spec`);

    // 4. Create DevOps platform webhook
    expect(ui.webhookStepTitle(alm).get()).toBeInTheDocument();
    expect(getCopyToClipboardValue(1, 'Copy')).toMatchSnapshot(`jenkins url`);

    // 5. Create jenkinsfile
    // Maven
    await user.click(ui.mavenBuildButton.get());
    expect(getCopyToClipboardValue(2, 'Copy')).toMatchSnapshot(`maven jenkinsfile`);

    // Gradle (Groovy)
    await user.click(ui.gradleBuildButton.get());
    expect(getCopyToClipboardValue(2, 'Copy')).toMatchSnapshot(`Groovy: build.gradle file`);
    // Gradle(Kotlin)
    await user.click(ui.gradleDSLButton(GradleBuildDSL.Kotlin).get());
    expect(getCopyToClipboardValue(2, 'Copy')).toMatchSnapshot(`Kotlin: build.gradle.kts file`);
    expect(getCopyToClipboardValue(3, 'Copy')).toMatchSnapshot(`gradle jenkinsfile`);

    // .NET
    await user.click(ui.dotnetBuildButton.get());
    await user.click(ui.windowsDotnetCoreButton.get());
    expect(getCopyToClipboardValue(2, 'Copy')).toMatchSnapshot(`windows dotnet core jenkinsfile`);

    await user.click(ui.windowsDotnetFrameworkButton.get());
    expect(getCopyToClipboardValue(2, 'Copy')).toMatchSnapshot(
      `windows dotnet framework jenkinsfile`,
    );

    await user.click(ui.linuxDotnetCoreButton.get());
    expect(getCopyToClipboardValue(2, 'Copy')).toMatchSnapshot(`linux dotnet core jenkinsfile`);

    // CFamilly
    await user.click(ui.cFamilyBuildButton.get());
    expect(getCopyToClipboardValue(2, 'Copy')).toMatchSnapshot(`sonar-project.properties code`);

    await user.click(ui.linuxButton.get());
    expect(getCopyToClipboardValue(3, 'Copy')).toMatchSnapshot(`cfamily linux jenkinsfile`);

    await user.click(ui.windowsButton.get());
    expect(getCopyToClipboardValue(3, 'Copy')).toMatchSnapshot(`cfamily windows jenkinsfile`);

    await user.click(ui.macosButton.get());
    expect(getCopyToClipboardValue(3, 'Copy')).toMatchSnapshot(`cfamily macos jenkinsfile`);

    // Other
    await user.click(ui.otherBuildButton.get());
    expect(getCopyToClipboardValue(2, 'Copy')).toMatchSnapshot(
      `other build tools sonar-project.properties code`,
    );
    expect(getCopyToClipboardValue(3, 'Copy')).toMatchSnapshot(`other build tools jenkinsfile`);

    expect(ui.allSetSentence.get()).toBeInTheDocument();
  },
);

it.each([AlmKeys.GitHub, AlmKeys.GitLab, AlmKeys.BitbucketCloud])(
  '%s: has Pipeline step instead of MultiBranchPipeline step if branchSupport is not enabled',
  async (alm: AlmKeys) => {
    const user = userEvent.setup();
    renderJenkinsTutorial({}, { featureList: [] });

    expect(await ui.devopsPlatformTitle.find()).toBeInTheDocument();
    await user.click(ui.devopsPlatformButton(alm).get());
    expect(ui.pipelineIntroText.get()).toBeInTheDocument();
  },
);

it.each([AlmKeys.GitHub, AlmKeys.BitbucketCloud])(
  '%s: completes tutorial with bound alm and project',
  async (alm: AlmKeys) => {
    const user = userEvent.setup();
    await almMock.handleSetProjectBinding(alm, {
      almSetting: 'my-project',
      project: 'my-project',
      repository: 'my-project',
      monorepo: true,
    });
    renderJenkinsTutorial({
      almBinding: {
        alm,
        url: 'http://localhost/qube',
        key: 'my-project',
      },
    });

    await waitFor(() => expect(ui.devopsPlatformTitle.query()).not.toBeInTheDocument());

    expect(ui.webhookAlmLink(alm).get()).toBeInTheDocument();
    await user.click(ui.mavenBuildButton.get());
    expect(ui.allSetSentence.get()).toBeInTheDocument();
  },
);

it('navigates between steps', async () => {
  const user = userEvent.setup();
  renderJenkinsTutorial();

  await user.click(ui.devopsPlatformButton(AlmKeys.GitHub).get());
  await user.click(ui.mavenBuildButton.get());
  expect(ui.allSetSentence.get()).toBeInTheDocument();

  // Navigate back
  await user.click(ui.jenkinsStepTitle.get());
  expect(ui.mavenBuildButton.get()).toBeInTheDocument();

  await user.click(ui.webhookStepTitle(AlmKeys.GitHub).get());
  expect(ui.webhookStepIntroSentence.get()).toBeInTheDocument();

  await user.click(ui.multiBranchStepTitle.get());
  expect(ui.multiBranchPipelineSecondListItem(AlmKeys.GitHub).get()).toBeInTheDocument();

  await user.click(ui.prerequisitesTitle.get());

  await user.click(ui.devopsPlatformTitle.get());
  expect(ui.devopsPlatformButton(AlmKeys.BitbucketCloud).get()).toBeInTheDocument();
});

function renderJenkinsTutorial(
  overrides: Partial<JenkinsTutorialProps> = {},
  {
    featureList = [Feature.BranchSupport],
    languages = { c: mockLanguage({ key: 'c' }) },
  }: RenderContext = {},
) {
  return renderApp(
    '/',
    <JenkinsTutorial baseUrl="http://localhost:9000" component={mockComponent()} {...overrides} />,
    { featureList, languages },
  );
}
