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
import * as React from 'react';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getHostUrl } from 'sonar-ui-common/helpers/urls';
import CodeSnippet from '../../../../components/common/CodeSnippet';
import { getProjectKey } from '../ProjectAnalysisStep';
import { ClangGCCTravisSonarCloud } from './TravisSonarCloud/ClangGCCTravisSonarCloud';
import { JavaGradleTravisSonarCloud } from './TravisSonarCloud/JavaGradleTravisSonarCloud';
import { JavaMavenTravisSonarCloud } from './TravisSonarCloud/JavaMavenTravisSonarCloud';
import { OtherTravisSonarCloud } from './TravisSonarCloud/OtherTravisSonarCloud';

interface Props {
  buildType: string | undefined;
  component?: T.Component;
  organization?: string;
  small?: boolean;
  token?: string;
}

interface RenderProps {
  component?: T.Component;
  organization?: string;
  small?: boolean;
  token?: string;
}

export function getSonarcloudAddonYml(organization: string = '') {
  return `addons:
  sonarcloud:
    organization: ${organization ? `"${organization}"` : `"Add your organization key"`}
    token:
      secure: "**************************" # encrypted value of your token`;
}

export function getSonarcloudAddonYmlRender(organization: string = '') {
  return (
    <>
      {`addons:
  sonarcloud:
    organization: ${organization ? `"${organization}"` : `"Add your organization key"`}
    token:
      secure: `}
      {
        <span className="highlight">
          {'"**************************"'} # encrypted value of your token
        </span>
      }
      <br />
    </>
  );
}

export function RequirementJavaBuild() {
  return (
    <>
      <p className="spacer-bottom">{translate('onboarding.analysis.with.travis.environments')}</p>

      <div className="flex-columns">
        <div className="flex-column flex-column-half">
          <a
            href="https://docs.travis-ci.com/user/reference/precise/"
            rel="noopener noreferrer"
            target="_blank">
            {translate('onboarding.analysis.with.travis.environment.image.java')}
          </a>
          <CodeSnippet isOneLine={true} noCopy={true} snippet="language: java" />
        </div>

        <div className="display-flex-stretch">
          <div className="vertical-pipe-separator">
            <div className="vertical-separator " />
            <span className="note">{translate('or')}</span>
            <div className="vertical-separator" />
          </div>
        </div>

        <div className="flex-column flex-column-half">
          <a
            href="https://docs.travis-ci.com/user/reference/trusty/"
            rel="noopener noreferrer"
            target="_blank">
            {translate('onboarding.analysis.with.travis.environment.image.ci')}
          </a>
          <CodeSnippet isOneLine={true} noCopy={true} snippet="dist: trusty" />
        </div>
      </div>
    </>
  );
}

export function RequirementOtherBuild() {
  return (
    <>
      <p>
        {translate('onboarding.analysis.with.travis.environment')}{' '}
        <a
          href="https://docs.travis-ci.com/user/reference/trusty/"
          rel="noopener noreferrer"
          target="_blank">
          {translate('onboarding.analysis.with.travis.environment.image.ci')}
        </a>
      </p>

      <CodeSnippet isOneLine={true} noCopy={true} snippet="dist: trusty" />
    </>
  );
}

export function RenderCommandForClangOrGCC({ component, organization, small, token }: RenderProps) {
  const projectKey = getProjectKey(undefined, component);
  if (!projectKey || !token) {
    return null;
  }
  return (
    <ClangGCCTravisSonarCloud
      host={getHostUrl()}
      organization={organization}
      os="linux"
      projectKey={projectKey}
      small={small}
      token={token}
    />
  );
}

export function RenderCommandForGradle({ component, organization, token }: RenderProps) {
  if (!token) {
    return null;
  }

  return (
    <JavaGradleTravisSonarCloud
      host={getHostUrl()}
      organization={organization}
      projectKey={component && component.key}
      token={token}
    />
  );
}

export function RenderCommandForMaven({ component, organization, token }: RenderProps) {
  if (!token) {
    return null;
  }

  return (
    <JavaMavenTravisSonarCloud
      host={getHostUrl()}
      organization={organization}
      projectKey={component && component.key}
      token={token}
    />
  );
}

export function RenderCommandForOther({ component, organization, token }: RenderProps) {
  const projectKey = getProjectKey(undefined, component);
  if (!projectKey || !token) {
    return null;
  }
  return (
    <OtherTravisSonarCloud
      host={getHostUrl()}
      organization={organization}
      os="linux"
      projectKey={projectKey}
      token={token}
    />
  );
}

function getBuildOptions(): {
  [k: string]: (props: Props) => JSX.Element | null;
} {
  return {
    gradle: RenderCommandForGradle,
    make: RenderCommandForClangOrGCC,
    maven: RenderCommandForMaven,
    other: RenderCommandForOther
  };
}

export default function AnalysisCommandTravis(props: Props) {
  const { buildType } = props;

  const Build = (buildType && getBuildOptions()[buildType]) || undefined;

  return Build ? <Build {...props} /> : null;
}
