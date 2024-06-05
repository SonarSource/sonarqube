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
import { Component } from '../../../../types/types';
import { BuildTools, ManualTutorialConfig } from '../../types';
import ClangGCCCustom from './ClangGCCCommand';
import DotNet from './DotNet';
import JavaGradle from './JavaGradle';
import JavaMaven from './JavaMaven';
import Other from './Other';

export interface AnalysisCommandProps {
  baseUrl: string;
  component: Component;
  isLocal: boolean;
  languageConfig: ManualTutorialConfig;
  token?: string;
}

export default function AnalysisCommand(props: AnalysisCommandProps) {
  const { component, baseUrl, isLocal, languageConfig, token } = props;

  if (!token) {
    return null;
  }

  switch (languageConfig.buildTool) {
    case BuildTools.Maven:
      return <JavaMaven baseUrl={baseUrl} component={component} token={token} />;

    case BuildTools.Gradle:
      return <JavaGradle baseUrl={baseUrl} component={component} token={token} />;

    case BuildTools.DotNet:
      return <DotNet baseUrl={baseUrl} component={component} token={token} />;

    case BuildTools.CFamily:
      return languageConfig.os !== undefined ? (
        <ClangGCCCustom
          os={languageConfig.os}
          baseUrl={baseUrl}
          component={component}
          isLocal={isLocal}
          token={token}
        />
      ) : null;

    case BuildTools.Other:
      return languageConfig.os !== undefined ? (
        <Other
          baseUrl={baseUrl}
          os={languageConfig.os}
          component={component}
          isLocal={isLocal}
          token={token}
        />
      ) : null;

    default:
      return null;
  }
}
