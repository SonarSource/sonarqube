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
import { Link } from '@sonarsource/echoes-react';
import { Dictionary } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import { DocLink } from '../../../helpers/doc-links';
import { useDocUrl } from '../../../helpers/docs';
import { translate } from '../../../helpers/l10n';
import { Feature } from '../../../types/features';
import { Component } from '../../../types/types';
import { CompilationInfo } from '../components/CompilationInfo';
import CreateYmlFile from '../components/CreateYmlFile';
import { Arch, AutoConfig, BuildTools, TutorialConfig } from '../types';
import { isCFamily } from '../utils';
import { PreambuleYaml } from './PreambuleYaml';
import cFamilyExample from './commands/CFamily';
import dotNetExample from './commands/DotNet';
import gradleExample from './commands/Gradle';
import mavenExample from './commands/Maven';
import othersExample from './commands/Others';

export interface AnalysisCommandProps extends WithAvailableFeaturesProps {
  arch: Arch;
  component: Component;
  config: TutorialConfig;
  mainBranchName: string;
}

export type BuildToolExampleBuilder = (data: {
  arch?: Arch;
  branchesEnabled?: boolean;
  config: TutorialConfig;
  mainBranchName?: string;
  projectKey?: string;
  projectName?: string;
}) => string;

const YamlTemplate: Dictionary<BuildToolExampleBuilder> = {
  [BuildTools.Gradle]: gradleExample,
  [BuildTools.Maven]: mavenExample,
  [BuildTools.DotNet]: dotNetExample,
  [BuildTools.Cpp]: cFamilyExample,
  [BuildTools.ObjectiveC]: cFamilyExample,
  [BuildTools.Other]: othersExample,
};

const showJreWarning = (config: TutorialConfig, arch: Arch) => {
  if (!isCFamily(config.buildTool)) {
    return false;
  }
  if (config.autoConfig === AutoConfig.Automatic) {
    return false;
  }
  return arch === Arch.Arm64;
};

export function AnalysisCommand(props: Readonly<AnalysisCommandProps>) {
  const { config, arch, mainBranchName, component } = props;
  const branchesEnabled = props.hasFeature(Feature.BranchSupport);
  const scannerRequirementsUrl = useDocUrl(DocLink.SonarScannerRequirements);

  if (!config.buildTool) {
    return null;
  }

  const yamlTemplate = YamlTemplate[config.buildTool]({
    arch,
    config,
    branchesEnabled,
    mainBranchName,
    projectKey: component.key,
    projectName: component.name,
  });

  const warning = showJreWarning(config, arch) && (
    <p className="sw-mb-2">
      <FormattedMessage
        defaultMessage={translate('onboarding.analysis.sq_scanner.jre_required_warning')}
        id="onboarding.analysis.sq_scanner.jre_required_warning"
        values={{
          link: (
            <Link to={scannerRequirementsUrl}>
              {translate('onboarding.analysis.sq_scanner.jre_required_warning.link')}
            </Link>
          ),
        }}
      />
    </p>
  );

  return (
    <>
      <PreambuleYaml buildTool={config.buildTool} component={component} />
      <CreateYmlFile
        yamlFileName="bitbucket-pipelines.yml"
        yamlTemplate={yamlTemplate}
        warning={warning}
      />
      {isCFamily(config.buildTool) && <CompilationInfo />}
    </>
  );
}

export default withAvailableFeatures(AnalysisCommand);
