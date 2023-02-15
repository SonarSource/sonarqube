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
import { screen } from '@testing-library/react';
import { ShallowWrapper } from 'enzyme';
import { byRole } from 'testing-library-selector';
import Step from './components/Step';
import { BuildTools, OSs } from './types';

export function renderStepContent(wrapper: ShallowWrapper<React.ReactNode>, n = 0) {
  return wrapper.find(Step).at(n).props().renderForm();
}

export function getCopyToClipboardValue(i = 0, name = 'copy_to_clipboard') {
  return screen.getAllByRole('button', { name })[i].getAttribute('data-clipboard-text');
}

export function getTutorialBuildButtons() {
  return {
    describeBuildTitle: byRole('heading', { name: 'onboarding.build' }),
    mavenBuildButton: byRole('button', { name: `onboarding.build.${BuildTools.Maven}` }),
    gradleBuildButton: byRole('button', { name: `onboarding.build.${BuildTools.Gradle}` }),
    dotnetBuildButton: byRole('button', { name: `onboarding.build.${BuildTools.DotNet}` }),
    cFamilyBuildButton: byRole('button', { name: `onboarding.build.${BuildTools.CFamily}` }),
    otherBuildButton: byRole('button', { name: `onboarding.build.${BuildTools.Other}` }),
    dotnetCoreButton: byRole('button', { name: 'onboarding.build.dotnet.variant.dotnet_core' }),
    dotnetFrameworkButton: byRole('button', {
      name: 'onboarding.build.dotnet.variant.dotnet_framework',
    }),
    linuxButton: byRole('button', { name: `onboarding.build.other.os.${OSs.Linux}` }),
    windowsButton: byRole('button', { name: `onboarding.build.other.os.${OSs.Windows}` }),
    macosButton: byRole('button', { name: `onboarding.build.other.os.${OSs.MacOS}` }),
  };
}
