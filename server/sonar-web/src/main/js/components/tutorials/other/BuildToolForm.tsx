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
import { ToggleButton } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { withCLanguageFeature } from '../../hoc/withCLanguageFeature';
import GithubCFamilyExampleRepositories from '../components/GithubCFamilyExampleRepositories';
import RenderOptions from '../components/RenderOptions';
import { BuildTools, ManualTutorialConfig, OSs, TutorialModes } from '../types';

interface Props {
  config?: ManualTutorialConfig;
  hasCLanguageFeature: boolean;
  onDone: (config: ManualTutorialConfig) => void;
}

interface State {
  config: ManualTutorialConfig;
}

export class BuildToolForm extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      config: this.props.config || {},
    };
  }

  handleBuildToolChange = (buildTool: BuildTools) => {
    const selectOsByDefault = (buildTool === BuildTools.CFamily ||
      buildTool === BuildTools.Other) && {
      os: OSs.Linux,
    };

    this.setState({ config: { buildTool, ...selectOsByDefault } }, () => {
      this.props.onDone(this.state.config);
    });
  };

  handleOSChange = (os: OSs) => {
    this.setState(
      ({ config }) => ({ config: { buildTool: config.buildTool, os } }),
      () => {
        this.props.onDone(this.state.config);
      },
    );
  };

  render() {
    const { config } = this.state;
    const { hasCLanguageFeature } = this.props;
    const buildTools = [BuildTools.Maven, BuildTools.Gradle, BuildTools.DotNet];
    if (hasCLanguageFeature) {
      buildTools.push(BuildTools.CFamily);
    }
    buildTools.push(BuildTools.Other);

    return (
      <>
        <div>
          <label className="sw-block sw-mb-1">{translate('onboarding.build')}</label>
          <ToggleButton
            label={translate('onboarding.build')}
            onChange={this.handleBuildToolChange}
            options={buildTools.map((tool) => ({
              label: translate('onboarding.build', tool),
              value: tool,
            }))}
            value={config.buildTool}
          />
        </div>

        {(config.buildTool === BuildTools.Other || config.buildTool === BuildTools.CFamily) && (
          <RenderOptions
            label={translate('onboarding.build.other.os')}
            checked={config.os}
            onCheck={this.handleOSChange}
            optionLabelKey="onboarding.build.other.os"
            options={[OSs.Linux, OSs.Windows, OSs.MacOS]}
            titleLabelKey="onboarding.build.other.os"
          />
        )}

        {config.buildTool === BuildTools.CFamily && config.os && (
          <GithubCFamilyExampleRepositories
            className="sw-mt-4 sw-w-abs-600"
            os={config.os}
            ci={TutorialModes.Local}
          />
        )}
      </>
    );
  }
}

export default withCLanguageFeature(BuildToolForm);
