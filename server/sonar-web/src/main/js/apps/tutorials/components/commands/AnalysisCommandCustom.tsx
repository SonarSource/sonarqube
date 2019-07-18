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
import { getHostUrl } from 'sonar-ui-common/helpers/urls';
import { RenderOS, RenderOSProps } from '../LanguageForm';
import { getProjectKey } from '../ProjectAnalysisStep';
import { ProjectAnalysisModes } from '../ProjectAnalysisStepFromBuildTool';
import { AnalysisCommandCommon } from './AnalysisCommandOtherCI';
import ClangGCCCustom from './Custom/ClangGCCCustom';
import JavaGradleCustom from './Custom/JavaGradleCustom';
import JavaMavenCustom from './Custom/JavaMavenCustom';
import OtherCustom from './Custom/OtherCustom';
import { AnalysisCommandProps, AnalysisCommandRenderProps } from './utils';

export function RenderCommandForMaven({
  component,
  mode,
  onDone,
  organization,
  toggleModal,
  token
}: AnalysisCommandRenderProps) {
  if (!token) {
    return null;
  }

  return (
    <JavaMavenCustom
      host={getHostUrl()}
      mode={mode}
      onDone={onDone}
      organization={organization}
      projectKey={component && component.key}
      toggleModal={toggleModal}
      token={token}
    />
  );
}

export function RenderCommandForGradle({
  component,
  mode,
  onDone,
  organization,
  toggleModal,
  token
}: AnalysisCommandRenderProps) {
  if (!token) {
    return null;
  }

  return (
    <JavaGradleCustom
      host={getHostUrl()}
      mode={mode}
      onDone={onDone}
      organization={organization}
      projectKey={component && component.key}
      toggleModal={toggleModal}
      token={token}
    />
  );
}

export function RenderCommandForClangOrGCC({
  component,
  mode,
  onDone,
  organization,
  os,
  small,
  toggleModal,
  token
}: AnalysisCommandRenderProps) {
  const projectKey = getProjectKey(undefined, component);
  if (!projectKey || !os || !token) {
    return null;
  }
  return (
    <ClangGCCCustom
      host={getHostUrl()}
      mode={mode}
      onDone={onDone}
      organization={organization}
      os={os}
      projectKey={projectKey}
      small={small}
      toggleModal={toggleModal}
      token={token}
    />
  );
}

export function RenderCommandForOther({
  component,
  currentUser,
  mode,
  onDone,
  organization,
  os,
  toggleModal,
  token
}: AnalysisCommandRenderProps) {
  const projectKey = getProjectKey(undefined, component);
  if (!component || !projectKey || !os || !token) {
    return null;
  }
  return (
    <OtherCustom
      component={component}
      currentUser={currentUser}
      host={getHostUrl()}
      mode={mode}
      onDone={onDone}
      organization={organization}
      os={os}
      projectKey={projectKey}
      toggleModal={toggleModal}
      token={token}
    />
  );
}

function getBuildOptions({
  os,
  setOS
}: RenderOSProps): { [k: string]: (props: AnalysisCommandRenderProps) => JSX.Element | null } {
  return {
    gradle: RenderCommandForGradle,
    make: function make(props: AnalysisCommandRenderProps) {
      return (
        <>
          <RenderOS os={os} setOS={setOS} />
          <RenderCommandForClangOrGCC {...props} />
        </>
      );
    },
    maven: RenderCommandForMaven,
    other: function other(props: AnalysisCommandRenderProps) {
      return (
        <>
          <RenderOS os={os} setOS={setOS} />
          <RenderCommandForOther {...props} />
        </>
      );
    }
  };
}

export default function AnalysisCommandCustom(props: AnalysisCommandProps) {
  return (
    <AnalysisCommandCommon
      {...props}
      getBuildOptions={getBuildOptions}
      mode={ProjectAnalysisModes.Custom}
    />
  );
}
