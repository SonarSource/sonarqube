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
import { translate } from '../../../helpers/l10n';
import { withCLanguageFeature } from '../../hoc/withCLanguageFeature';
import RenderOptions from '../components/RenderOptions';
import { BuildTools } from '../types';

export interface YamlFileStepProps {
  children?: (buildTool: BuildTools) => React.ReactElement<{}>;
  hasCLanguageFeature: boolean;
}

export function YamlFileStep(props: YamlFileStepProps) {
  const { children, hasCLanguageFeature } = props;

  const buildTools = [BuildTools.Maven, BuildTools.Gradle, BuildTools.DotNet];
  if (hasCLanguageFeature) {
    buildTools.push(BuildTools.CFamily);
  }
  buildTools.push(BuildTools.Other);

  const [buildToolSelected, setBuildToolSelected] = React.useState<BuildTools>();

  return (
    <ol className="list-styled big-spacer-top big-spacer-bottom">
      <li className="abs-width-600">
        {translate('onboarding.build')}
        <RenderOptions
          label={translate('onboarding.build')}
          checked={buildToolSelected}
          onCheck={(value) => setBuildToolSelected(value as BuildTools)}
          options={buildTools}
          optionLabelKey="onboarding.build"
        />
      </li>
      {children && buildToolSelected && children(buildToolSelected)}
    </ol>
  );
}

export default withCLanguageFeature(YamlFileStep);
