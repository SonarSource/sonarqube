/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { AlmKeys } from '../../../types/alm-settings';
import RenderOptions from '../components/RenderOptions';
import { BuildTools } from '../types';
import AllSet from './AllSet';

export interface YamlFileStepProps {
  alm: AlmKeys;
  children?: (buildTool?: BuildTools) => React.ReactElement<{}>;
}

export interface AnalysisCommandProps {
  appState: T.AppState;
  buildTool?: BuildTools;
  component: T.Component;
}

export default function YamlFileStep(props: YamlFileStepProps) {
  const { alm, children } = props;
  const buildTools = [BuildTools.Maven, BuildTools.Gradle, BuildTools.DotNet, BuildTools.Other];
  const [buildToolSelected, setBuildToolSelected] = React.useState<BuildTools>();

  return (
    <>
      <ol className="list-styled big-spacer-top big-spacer-bottom">
        <li>
          {translate('onboarding.build')}

          <RenderOptions
            checked={buildToolSelected}
            name="language"
            onCheck={value => setBuildToolSelected(value as BuildTools)}
            options={buildTools}
            optionLabelKey="onboarding.build"
          />
        </li>
        {children && children(buildToolSelected)}
      </ol>
      {buildToolSelected !== undefined && (
        <>
          <hr className="huge-spacer-top huge-spacer-bottom" />
          <AllSet alm={alm} />
        </>
      )}
    </>
  );
}
