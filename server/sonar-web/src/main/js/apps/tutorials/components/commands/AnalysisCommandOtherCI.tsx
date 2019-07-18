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
import EditTokenModal from '../../analyzeProject/steps/EditTokenModal';
import { RenderOS, RenderOSProps } from '../LanguageForm';
import { getProjectKey } from '../ProjectAnalysisStep';
import { ProjectAnalysisModes } from '../ProjectAnalysisStepFromBuildTool';
import { RenderCommandForGradle, RenderCommandForMaven } from './AnalysisCommandCustom';
import ClangGCCOtherCI from './OtherCI/ClangGCCOtherCI';
import OtherOtherCI from './OtherCI/OtherOtherCI';
import { AnalysisCommandProps, AnalysisCommandRenderProps } from './utils';

export function RenderCommandForClangOrGCC({
  component,
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
    <ClangGCCOtherCI
      host={getHostUrl()}
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
    <OtherOtherCI
      component={component}
      currentUser={currentUser}
      host={getHostUrl()}
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
    make: function make(props) {
      return (
        <>
          <RenderOS os={os} setOS={setOS} />
          <RenderCommandForClangOrGCC {...props} />
        </>
      );
    },
    maven: RenderCommandForMaven,
    other: function other(props) {
      return (
        <>
          <RenderOS os={os} setOS={setOS} />
          <RenderCommandForOther {...props} />
        </>
      );
    }
  };
}

interface AnalysisCommandExtraProps {
  mode: ProjectAnalysisModes;
  getBuildOptions: (
    props: RenderOSProps
  ) => { [k: string]: (props: AnalysisCommandRenderProps) => JSX.Element | null };
}

export function AnalysisCommandCommon(props: AnalysisCommandProps & AnalysisCommandExtraProps) {
  const [os, setOS] = React.useState<string | undefined>(undefined);
  const [isModalVisible, toggleModal] = React.useState<boolean>(false);
  const { buildType } = props;

  if (!os && props.os) {
    setOS(props.os);
  }

  const toggleTokenModal = () => toggleModal(!isModalVisible);

  const close = () => toggleModal(false);

  const save = (t: string) => {
    props.setToken(t);
    close();
  };

  const callOnDone = () => {
    props.onDone({ os });
  };

  const Build = (buildType && props.getBuildOptions({ os, setOS })[buildType]) || undefined;

  return Build ? (
    <>
      {isModalVisible && (
        <EditTokenModal
          component={props.component}
          currentUser={props.currentUser}
          onClose={close}
          onSave={save}
        />
      )}

      <Build
        {...props}
        mode={props.mode}
        onDone={callOnDone}
        os={os}
        toggleModal={toggleTokenModal}
        token={props.token}
      />
    </>
  ) : null;
}

export default function AnalysisCommandOtherCI(props: AnalysisCommandProps) {
  return (
    <AnalysisCommandCommon
      {...props}
      getBuildOptions={getBuildOptions}
      mode={ProjectAnalysisModes.CI}
    />
  );
}
