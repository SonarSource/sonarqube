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

import { noop } from 'lodash';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import Step from '../components/Step';
import { Arch, OSs, TutorialConfig } from '../types';
import BuildToolForm from './BuildToolForm';
import AnalysisCommand from './commands/AnalysisCommand';

interface Props {
  baseUrl: string;
  component: Component;
  isLocal: boolean;
  open: boolean;
  stepNumber: number;
  token?: string;
}

export default function ProjectAnalysisStep(props: Readonly<Props>) {
  const { component, open, stepNumber, baseUrl, isLocal, token } = props;

  const [config, setConfig] = React.useState<TutorialConfig>({});
  const [os, setOs] = React.useState<OSs>(OSs.Linux);
  const [arch, setArch] = React.useState<Arch>(Arch.X86_64);

  function renderForm() {
    return (
      <div className="sw-pb-4">
        <BuildToolForm
          config={config}
          isLocal={isLocal}
          setConfig={setConfig}
          os={os}
          setOs={setOs}
          arch={arch}
          setArch={setArch}
        />

        {config && (
          <div className="sw-mt-4">
            <AnalysisCommand
              config={config}
              os={os}
              arch={arch}
              component={component}
              baseUrl={baseUrl}
              isLocal={isLocal}
              token={token}
            />
          </div>
        )}
      </div>
    );
  }

  return (
    <Step
      finished={false}
      onOpen={noop}
      open={open}
      renderForm={renderForm}
      stepNumber={stepNumber}
      stepTitle={translate('onboarding.analysis.header')}
    />
  );
}
