/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import RadioToggle from 'sonar-ui-common/components/controls/RadioToggle';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { isSonarCloud } from '../../../helpers/system';
import { isLanguageConfigured, LanguageConfig } from '../utils';
import NewProjectForm from './NewProjectForm';
import { RenderOptions } from './RenderOptions';

interface Props {
  component?: T.Component;
  config?: LanguageConfig;
  onDone: (config: LanguageConfig) => void;
  onReset: VoidFunction;
  organization?: string;
}

type State = LanguageConfig;

export interface RenderOSProps {
  os: string | undefined;
  setOS: (os: string) => void;
}
export function RenderOS(props: RenderOSProps) {
  return (
    <RenderOptions
      checked={props.os}
      name="os"
      onCheck={props.setOS}
      optionLabelKey="onboarding.language.os"
      options={['linux', 'win', 'mac']}
      titleLabelKey="onboarding.language.os"
    />
  );
}

export default class LanguageForm extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      ...(this.props.config || {}),
      projectKey: props.component ? props.component.key : undefined
    };
  }

  handleChange = () => {
    if (isLanguageConfigured(this.state)) {
      this.props.onDone(this.state);
    } else {
      this.props.onReset();
    }
  };

  handleLanguageChange = (language: string) => {
    this.setState({ language }, this.handleChange);
  };

  handleJavaBuildChange = (javaBuild: string) => {
    this.setState({ javaBuild }, this.handleChange);
  };

  handleCFamilyCompilerChange = (cFamilyCompiler: string) => {
    this.setState({ cFamilyCompiler }, this.handleChange);
  };

  handleOSChange = (os: string) => {
    this.setState({ os }, this.handleChange);
  };

  handleProjectKeyDone = (projectKey: string) => {
    this.setState({ projectKey }, this.handleChange);
  };

  handleProjectKeyDelete = () => {
    this.setState({ projectKey: undefined }, this.handleChange);
  };

  renderJavaBuild = () => (
    <RenderOptions
      checked={this.state.javaBuild}
      name="java-build"
      onCheck={this.handleJavaBuildChange}
      optionLabelKey="onboarding.language.java.build_technology"
      options={['maven', 'gradle']}
      titleLabelKey="onboarding.language.java.build_technology"
    />
  );

  renderCFamilyCompiler = () => (
    <RenderOptions
      checked={this.state.cFamilyCompiler}
      name="c-family-compiler"
      onCheck={this.handleCFamilyCompilerChange}
      optionLabelKey="onboarding.language.c-family.compiler"
      options={['msvc', 'clang-gcc']}
      titleLabelKey="onboarding.language.c-family.compiler"
    />
  );

  renderProjectKey = () => {
    const { cFamilyCompiler, language, os } = this.state;
    const needProjectKey =
      language === 'dotnet' ||
      (language === 'c-family' &&
        (cFamilyCompiler === 'msvc' || (cFamilyCompiler === 'clang-gcc' && os !== undefined))) ||
      (language === 'other' && os !== undefined);

    if (!needProjectKey || this.props.component) {
      return null;
    }

    return (
      <NewProjectForm
        onDelete={this.handleProjectKeyDelete}
        onDone={this.handleProjectKeyDone}
        organization={this.props.organization}
        projectKey={this.state.projectKey}
      />
    );
  };

  render() {
    const { cFamilyCompiler, language } = this.state;
    const languages = isSonarCloud()
      ? ['java', 'dotnet', 'c-family', 'other']
      : ['java', 'dotnet', 'other'];

    return (
      <>
        <div>
          <h4 className="spacer-bottom">{translate('onboarding.language')}</h4>
          <RadioToggle
            name="language"
            onCheck={this.handleLanguageChange}
            options={languages.map(language => ({
              label: translate('onboarding.language', language),
              value: language
            }))}
            value={language}
          />
        </div>
        {language === 'java' && this.renderJavaBuild()}
        {language === 'c-family' && this.renderCFamilyCompiler()}
        {((language === 'c-family' && cFamilyCompiler === 'clang-gcc') || language === 'other') && (
          <RenderOS os={this.state.os} setOS={this.handleOSChange} />
        )}
        {this.renderProjectKey()}
      </>
    );
  }
}
