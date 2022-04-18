/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getHostUrl } from 'sonar-ui-common/helpers/urls';
import InstanceMessage from '../../common/InstanceMessage';

export enum Steps {
  ANALYSIS,
  TOKEN
}

interface Props {
  component: T.Component;
  currentUser: T.LoggedInUser;
}

interface State {
  step: Steps;
  token?: string;
}

export default class ManualTutorial extends React.PureComponent<Props, State> {
  state: State = { step: Steps.TOKEN };

  handleTokenDone = (token: string) => {
    this.setState({ step: Steps.ANALYSIS, token });
  };

  handleTokenOpen = () => {
    this.setState({ step: Steps.TOKEN });
  };

  render() {
    const projectKey = this.props.component.key;
    return (
      <>
        <div className="page-header big-spacer-bottom">
          <h1 className="page-title">{translate('onboarding.project_analysis.header')}</h1>
          <p className="page-description">
            <p>{translate('layout.must_be_configured')} Run analysis on <a href={getHostUrl() + '/project/extension/developer/project?id=' + projectKey}>Project Analysis</a> Page. </p>
          </p>
        </div>
      </>
    );
  }
}
