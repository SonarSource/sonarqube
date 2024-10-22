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

import { Component } from '../../../../types/types';
import { Arch, AutoConfig, BuildTools, OSs, TutorialConfig } from '../../types';
import ClangGCCCustom from './ClangGCCCommand';
import DotNet from './DotNet';
import JavaGradle from './JavaGradle';
import JavaMaven from './JavaMaven';
import Other from './Other';

export interface AnalysisCommandProps {
  arch: Arch;
  baseUrl: string;
  component: Component;
  config: TutorialConfig;
  isLocal: boolean;
  os: OSs;
  token?: string;
}

export default function AnalysisCommand(props: Readonly<AnalysisCommandProps>) {
  const { config, os, arch, component, baseUrl, isLocal, token } = props;

  if (typeof token === 'undefined') {
    return null;
  }

  switch (config.buildTool) {
    case BuildTools.Maven:
      return <JavaMaven baseUrl={baseUrl} component={component} token={token} />;

    case BuildTools.Gradle:
      return <JavaGradle baseUrl={baseUrl} component={component} token={token} />;

    case BuildTools.DotNet:
      return <DotNet baseUrl={baseUrl} component={component} token={token} />;

    case BuildTools.Dart:
    case BuildTools.Other:
      return (
        <Other
          arch={arch}
          baseUrl={baseUrl}
          os={os}
          component={component}
          isLocal={isLocal}
          token={token}
        />
      );

    case BuildTools.Cpp:
    case BuildTools.ObjectiveC:
      if (config.buildTool === BuildTools.Cpp && config.autoConfig === AutoConfig.Automatic) {
        return (
          <Other
            arch={arch}
            os={os}
            baseUrl={baseUrl}
            component={component}
            isLocal={isLocal}
            token={token}
          />
        );
      }
      return (
        <ClangGCCCustom
          os={os}
          arch={arch}
          baseUrl={baseUrl}
          component={component}
          isLocal={isLocal}
          token={token}
        />
      );

    default:
      return null;
  }
}
