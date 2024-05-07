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
import * as React from 'react';
import { byRole } from '~sonar-aligned/helpers/testSelector';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { OSs, TutorialModes } from '../../types';
import GithubCFamilyExampleRepositories, {
  GithubCFamilyExampleRepositoriesProps,
} from '../GithubCFamilyExampleRepositories';

const ui = {
  cfamilyExamplesLink: byRole('link', { name: /sonarsource-cfamily-examples/ }),
};

it.each([
  [OSs.Linux, TutorialModes.Jenkins, 'linux', 'jenkins'],
  [OSs.MacOS, TutorialModes.AzurePipelines, 'macos', 'azure'],
])(
  'should set correct value for CFamily examples link for %s and %ss',
  async (os: OSs, ci: TutorialModes, formattedOS: string, formattedCI: string) => {
    renderGithubCFamilyExampleRepositories({ os, ci });
    expect(await ui.cfamilyExamplesLink.find()).toHaveAttribute(
      'href',
      `https://github.com/orgs/sonarsource-cfamily-examples/repositories?q=sq+${formattedOS}+${formattedCI}`,
    );
  },
);

function renderGithubCFamilyExampleRepositories(
  overrides: Partial<GithubCFamilyExampleRepositoriesProps> = {},
) {
  return renderComponent(
    <GithubCFamilyExampleRepositories className="test-class" {...overrides} />,
  );
}
