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
import { CompilationInfo } from '../../components/CompilationInfo';
import { OSs } from '../../types';
import DownloadBuildWrapper from './DownloadBuildWrapper';
import DownloadScanner from './DownloadScanner';
import ExecBuildWrapper from './ExecBuildWrapper';
import ExecScanner from './ExecScanner';

export interface ClangGCCCustomProps {
  component: Component;
  baseUrl: string;
  isLocal: boolean;
  os: OSs;
  token: string;
}

export default function ClangGCCCustom(props: ClangGCCCustomProps) {
  const { os, baseUrl, component, isLocal, token } = props;

  return (
    <div>
      <DownloadBuildWrapper isLocal={isLocal} baseUrl={baseUrl} os={os} />
      <DownloadScanner isLocal={isLocal} os={os} token={token} />
      <ExecBuildWrapper os={os} />
      <CompilationInfo />
      <ExecScanner
        baseUrl={baseUrl}
        isLocal={isLocal}
        component={component}
        os={os}
        token={token}
        cfamily={true}
      />
    </div>
  );
}
