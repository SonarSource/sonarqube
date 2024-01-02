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
import { NumberedListItem } from 'design-system/lib';
import * as React from 'react';
import { translate } from '../../../../helpers/l10n';
import { Component } from '../../../../types/types';
import RenderOptions from '../../components/RenderOptions';
import { OSs } from '../../types';
import { LanguageProps } from '../JenkinsStep';
import DotNetCore from './DotNetCore';
import DotNetFramework from './DotNetFramework';

export interface DotNetCoreFrameworkProps {
  component: Component;
  os: OSDotNet;
}

export type OSDotNet = OSs.Linux | OSs.Windows;

const DotNetFlavor = { win_core: DotNetCore, win_msbuild: DotNetFramework, linux_core: DotNetCore };
const DotOS: { [key in keyof typeof DotNetFlavor]: OSDotNet } = {
  win_core: OSs.Windows,
  win_msbuild: OSs.Windows,
  linux_core: OSs.Linux,
};

export default function DotNet(props: LanguageProps) {
  const { component, onDone } = props;
  const [flavorComponent, setFlavorComponent] =
    React.useState<keyof typeof DotNetFlavor>('win_core');
  const DotNetTutorial = flavorComponent && DotNetFlavor[flavorComponent];

  React.useEffect(() => {
    onDone(flavorComponent !== undefined);
  }, [flavorComponent, onDone]);

  return (
    <>
      <NumberedListItem>
        {translate('onboarding.tutorial.with.jenkins.jenkinsfile.dotnet.build_agent')}
        <RenderOptions
          label={translate('onboarding.tutorial.with.jenkins.jenkinsfile.dotnet.build_agent')}
          checked={flavorComponent}
          optionLabelKey="onboarding.build.dotnet"
          onCheck={(value) => setFlavorComponent(value as keyof typeof DotNetFlavor)}
          options={Object.keys(DotNetFlavor)}
        />
      </NumberedListItem>
      {DotNetTutorial && flavorComponent && (
        <DotNetTutorial component={component} os={DotOS[flavorComponent]} />
      )}
    </>
  );
}
