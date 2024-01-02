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
import ButtonToggle from '../../controls/ButtonToggle';
import { withCLanguageFeature } from '../../hoc/withCLanguageFeature';
import GithubCFamilyExampleRepositories from '../components/GithubCFamilyExampleRepositories';
import RenderOptions from '../components/RenderOptions';
import { BuildTools, ManualTutorialConfig, OSs, TutorialModes } from '../types';

interface Props {
  hasCLanguageFeature: boolean;
  config?: ManualTutorialConfig;
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
    this.setState({ config: { buildTool } }, () => {
      this.props.onDone(this.state.config);
    });
  };

  handleOSChange = (os: OSs) => {
    this.setState(
      ({ config }) => ({ config: { buildTool: config.buildTool, os } }),
      () => {
        this.props.onDone(this.state.config);
      }
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
          <h4 className="spacer-bottom">{translate('onboarding.build')}</h4>
          <ButtonToggle
            label={translate('onboarding.build')}
            onCheck={this.handleBuildToolChange}
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
            className="big-spacer-top abs-width-600"
            os={config.os}
            ci={TutorialModes.Local}
          />
        )}
      </>
    );
  }
}

export default withCLanguageFeature(BuildToolForm);
