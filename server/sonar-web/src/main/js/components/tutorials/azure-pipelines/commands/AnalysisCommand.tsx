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
import { BuildTools, TutorialConfig } from '../../types';
import ClangGCC from './ClangGCC';
import DotNet from './DotNet';
import JavaGradle from './JavaGradle';
import JavaMaven from './JavaMaven';
import Other from './Other';

export interface AnalysisCommandProps {
  config: TutorialConfig;
  projectKey: string;
  projectName: string;
}

export default function AnalysisCommand(props: AnalysisCommandProps) {
  const { config, projectKey, projectName } = props;
  const { buildTool } = config;

  if (!buildTool) {
    return null;
  }

  switch (buildTool) {
    case BuildTools.Maven:
      return <JavaMaven projectKey={projectKey} projectName={projectName} />;

    case BuildTools.Gradle:
      return <JavaGradle projectKey={projectKey} projectName={projectName} />;

    case BuildTools.DotNet:
      return <DotNet projectKey={projectKey} />;

    case BuildTools.Cpp:
    case BuildTools.ObjectiveC:
      return <ClangGCC config={config} projectKey={projectKey} />;

    case BuildTools.Dart:
    case BuildTools.Other:
      return <Other projectKey={projectKey} />;

    default:
      return null;
  }
}
