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

import { screen } from '@testing-library/react';
import { byLabelText, byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { BuildTools, GradleBuildDSL, OSs, TutorialModes } from './types';

const CI_TRANSLATE_MAP: Partial<Record<TutorialModes, string>> = {
  [TutorialModes.BitbucketPipelines]: 'bitbucket_pipelines',
  [TutorialModes.GitHubActions]: 'github_action',
  [TutorialModes.GitLabCI]: 'gitlab_ci',
};

interface GetCopyToClipboardValueArgs {
  i?: number;
  inlineSnippet?: boolean;
  name?: string;
}

export function getCopyToClipboardValue({
  i = 0,
  inlineSnippet = false,
  name = 'copy_to_clipboard',
}: GetCopyToClipboardValueArgs = {}) {
  const button = screen.getAllByRole('button', { name })[i];

  return inlineSnippet
    ? button.previousSibling?.firstChild?.textContent
    : button.nextSibling?.firstChild?.textContent;
}

export function getCopyToClipboardHostURLValue({
  i = 0,
  name = 'copy_to_clipboard',
}: Omit<GetCopyToClipboardValueArgs, 'inlineSnippet'> = {}) {
  return screen.getAllByRole('button', { name })[i].nextSibling?.nextSibling?.firstChild
    ?.textContent;
}

export function getCommonNodes(ci: TutorialModes) {
  return {
    secretsStepTitle: byRole('heading', {
      name: `onboarding.tutorial.with.${CI_TRANSLATE_MAP[ci]}.${
        ci === TutorialModes.GitHubActions ? 'create_secret' : 'variables'
      }.title`,
    }),
    ymlFileStepTitle: byRole('heading', {
      name: `onboarding.tutorial.with.${CI_TRANSLATE_MAP[ci]}.yaml.title`,
    }),
    genTokenDialogButton: byRole('button', {
      name: 'onboarding.token.generate.long',
    }),
    tokenNameInput: byRole('textbox', { name: 'onboarding.token.name.label' }),
    expiresInSelect: byRole('combobox', { name: '' }),
    tokenValue: byText('generatedtoken2'),
    linkToRepo: byRole('link', {
      name: new RegExp(
        `onboarding.tutorial.with.${CI_TRANSLATE_MAP[ci]}.${
          ci === TutorialModes.GitHubActions ? 'secret' : 'variables'
        }.intro.link`,
      ),
    }),
    allSetSentence: byText('onboarding.tutorial.ci_outro.done'),
  };
}

export function getTutorialActionButtons() {
  return {
    continueButton: byRole('button', { name: 'continue' }),
    generateTokenButton: byRole('button', { name: 'onboarding.token.generate' }),
    deleteTokenButton: byRole('button', { name: 'onboarding.token.delete' }),
    finishTutorialButton: byRole('button', { name: 'tutorials.finish' }),
  };
}

export function getTutorialBuildButtons() {
  return {
    describeBuildTitle: byLabelText('onboarding.build'),
    mavenBuildButton: byRole('radio', { name: `onboarding.build.${BuildTools.Maven}` }),
    gradleBuildButton: byRole('radio', { name: `onboarding.build.${BuildTools.Gradle}` }),
    gradleDSLButton: (name: GradleBuildDSL) => byRole('radio', { name }),
    dotnetBuildButton: byRole('radio', { name: `onboarding.build.${BuildTools.DotNet}` }),
    cppBuildButton: byRole('radio', { name: `onboarding.build.${BuildTools.Cpp}` }),
    objCBuildButton: byRole('radio', { name: `onboarding.build.${BuildTools.ObjectiveC}` }),
    dartBuildButton: byRole('radio', { name: `onboarding.build.${BuildTools.Dart}` }),
    otherBuildButton: byRole('radio', { name: `onboarding.build.${BuildTools.Other}` }),
    windowsDotnetCoreButton: byRole('radio', {
      name: `onboarding.build.${BuildTools.DotNet}.win_core`,
    }),
    windowsDotnetFrameworkButton: byRole('radio', {
      name: `onboarding.build.${BuildTools.DotNet}.win_msbuild`,
    }),
    linuxDotnetCoreButton: byRole('radio', {
      name: `onboarding.build.${BuildTools.DotNet}.linux_core`,
    }),
    dotnetCoreButton: byRole('radio', {
      name: `onboarding.build.${BuildTools.DotNet}.variant.dotnet_core`,
    }),
    dotnetFrameworkButton: byRole('radio', {
      name: `onboarding.build.${BuildTools.DotNet}.variant.dotnet_framework`,
    }),
    linuxButton: byRole('radio', { name: `onboarding.build.other.os.${OSs.Linux}` }),
    windowsButton: byRole('radio', { name: `onboarding.build.other.os.${OSs.Windows}` }),
    macosButton: byRole('radio', { name: `onboarding.build.other.os.${OSs.MacOS}` }),
    arm64Button: byRole('radio', { name: `onboarding.build.other.architecture.arm64` }),
    x86_64Button: byRole('radio', { name: `onboarding.build.other.architecture.x86_64` }),
    autoConfigAutomatic: byRole('radio', { name: 'onboarding.build.cpp.autoconfig.automatic' }),
    autoConfigManual: byRole('radio', { name: 'onboarding.build.cpp.autoconfig.manual' }),
  };
}
