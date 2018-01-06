/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import React from 'react';
import NewProjectForm from './NewProjectForm';
import RadioToggle from '../../../components/controls/RadioToggle';
import { translate } from '../../../helpers/l10n';

/*::
type Props = {|
  onDone: (result: Result) => void,
  onReset: () => void,
  organization?: string,
  sonarCloud: boolean
|};
*/

/*::
type State = {
  language?: string,
  javaBuild?: string,
  cFamilyCompiler?: string,
  os?: string,
  projectKey?: string
};
*/

/*::
export type Result = State; */

export default class LanguageStep extends React.PureComponent {
  /*:: props: Props; */

  static defaultProps = { sonarCloud: false };

  state /*: State */ = {};

  isConfigured = () => {
    const { language, javaBuild, cFamilyCompiler, os, projectKey } = this.state;
    const isJavaConfigured = language === 'java' && javaBuild != null;
    const isDotNetConfigured = language === 'dotnet' && projectKey != null;
    const isCFamilyConfigured =
      language === 'c-family' && (cFamilyCompiler === 'msvc' || os != null) && projectKey != null;
    const isOtherConfigured = language === 'other' && projectKey != null;

    return isJavaConfigured || isDotNetConfigured || isCFamilyConfigured || isOtherConfigured;
  };

  handleChange = () => {
    if (this.isConfigured()) {
      this.props.onDone(this.state);
    } else {
      this.props.onReset();
    }
  };

  handleLanguageChange = (language /*: string */) => {
    this.setState({ language }, this.handleChange);
  };

  handleJavaBuildChange = (javaBuild /*: string */) => {
    this.setState({ javaBuild }, this.handleChange);
  };

  handleCFamilyCompilerChange = (cFamilyCompiler /*: string */) => {
    this.setState({ cFamilyCompiler }, this.handleChange);
  };

  handleOSChange = (os /*: string */) => {
    this.setState({ os }, this.handleChange);
  };

  handleProjectKeyDone = (projectKey /*: string */) => {
    this.setState({ projectKey }, this.handleChange);
  };

  handleProjectKeyDelete = () => {
    this.setState({ projectKey: undefined }, this.handleChange);
  };

  renderJavaBuild = () => (
    <div className="big-spacer-top">
      <h4 className="spacer-bottom">{translate('onboarding.language.java.build_technology')}</h4>
      <RadioToggle
        name="java-build"
        onCheck={this.handleJavaBuildChange}
        options={['maven', 'gradle'].map(build => ({
          label: translate('onboarding.language.java.build_technology', build),
          value: build
        }))}
        value={this.state.javaBuild}
      />
    </div>
  );

  renderCFamilyCompiler = () => (
    <div className="big-spacer-top">
      <h4 className="spacer-bottom">{translate('onboarding.language.c-family.compiler')}</h4>
      <RadioToggle
        name="c-family-compiler"
        onCheck={this.handleCFamilyCompilerChange}
        options={['msvc', 'clang-gcc'].map(compiler => ({
          label: translate('onboarding.language.c-family.compiler', compiler),
          value: compiler
        }))}
        value={this.state.cFamilyCompiler}
      />
    </div>
  );

  renderOS = () => (
    <div className="big-spacer-top">
      <h4 className="spacer-bottom">{translate('onboarding.language.os')}</h4>
      <RadioToggle
        name="os"
        onCheck={this.handleOSChange}
        options={['linux', 'win', 'mac'].map(os => ({
          label: translate('onboarding.language.os', os),
          value: os
        }))}
        value={this.state.os}
      />
    </div>
  );

  renderProjectKey = () => (
    <NewProjectForm
      onDelete={this.handleProjectKeyDelete}
      onDone={this.handleProjectKeyDone}
      organization={this.props.organization}
      projectKey={this.state.projectKey}
    />
  );

  render() {
    const shouldAskProjectKey =
      this.state.language === 'dotnet' ||
      (this.state.language === 'c-family' &&
        (this.state.cFamilyCompiler === 'msvc' ||
          (this.state.cFamilyCompiler === 'clang-gcc' && this.state.os != null))) ||
      (this.state.language === 'other' && this.state.os !== undefined);

    const languages = this.props.sonarCloud
      ? ['java', 'dotnet', 'c-family', 'other']
      : ['java', 'dotnet', 'other'];

    return (
      <div>
        <div>
          <h4 className="spacer-bottom">{translate('onboarding.language')}</h4>
          <RadioToggle
            name="language"
            onCheck={this.handleLanguageChange}
            options={languages.map(language => ({
              label: translate('onboarding.language', language),
              value: language
            }))}
            value={this.state.language}
          />
        </div>
        {this.state.language === 'java' && this.renderJavaBuild()}
        {this.state.language === 'c-family' && this.renderCFamilyCompiler()}
        {((this.state.language === 'c-family' && this.state.cFamilyCompiler === 'clang-gcc') ||
          this.state.language === 'other') &&
          this.renderOS()}
        {shouldAskProjectKey && this.renderProjectKey()}
      </div>
    );
  }
}
