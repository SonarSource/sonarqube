/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

export const PROJECT_ONBOARDING_DONE = 'sonarcloud.project.onboarding.finished';
export const PROJECT_ONBOARDING_MODE_ID = 'sonarcloud.project.onboarding.mode.id';
export const PROJECT_STEP_PROGRESS = 'sonarcloud.project.onboarding.step.progress';

export interface AlmLanguagesStats {
  [k: string]: number;
}

export interface Alm {
  id: string;
  name: string;
}

export interface AnalysisMode {
  icon?: string;
  id: string;
  name: string;
}

export enum ALM_KEYS {
  BITBUCKET = 'BITBUCKET',
  GITHUB = 'GITHUB',
  MICROSOFT = 'MICROSOFT'
}

export const alms: { [k: string]: Alm } = {
  [ALM_KEYS.BITBUCKET]: {
    id: ALM_KEYS.BITBUCKET,
    name: 'BitBucket'
  },
  [ALM_KEYS.GITHUB]: {
    id: ALM_KEYS.GITHUB,
    name: 'GitHub'
  },
  [ALM_KEYS.MICROSOFT]: {
    id: ALM_KEYS.MICROSOFT,
    name: 'Microsoft'
  }
};

export const modes: AnalysisMode[] = [
  {
    id: 'travis',
    name: 'With Travis CI'
  },
  {
    id: 'other',
    name: 'With other CI tools'
  },
  {
    id: 'manual',
    name: 'Manually'
  }
];

export const autoScanMode: AnalysisMode = {
  id: 'autoscan',
  name: 'SonarCloud Automatic Analysis'
};

export interface TutorialProps {
  component: T.Component;
  currentUser: T.LoggedInUser;
  onDone: VoidFunction;
  setToken: (token: string) => void;
  style?: object;
  token: string | undefined;
}

interface AutoScannableProps {
  [k: string]: number;
}

export function isAutoScannable(languages: AutoScannableProps = {}) {
  const allowed = [
    'ABAP',
    'Apex',
    'CSS',
    'Flex',
    'Go',
    'HTML',
    'JavaScript',
    'Kotlin',
    'PHP',
    'Python',
    'Ruby',
    'Scala',
    'Swift',
    'TypeScript',
    'TSQL',
    'XML'
  ];
  const notAllowed = ['Java', 'C#', 'Visual Basic', 'C', 'C++', 'Objective-C'];

  const withAllowedLanguages = !!Object.keys(languages).find(l => allowed.includes(l));
  const withNotAllowedLanguages = !!Object.keys(languages).find(l => notAllowed.includes(l));

  return { withAllowedLanguages, withNotAllowedLanguages };
}
