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
import { LightLabel, PageContentFontWrapper, Title } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import { LoggedInUser } from '../../../types/users';
import {getHostUrl} from "../../../helpers/urls";

export enum Steps {
  ANALYSIS,
  TOKEN,
}

interface Props {
  baseUrl: string;
  component: Component;
  currentUser: LoggedInUser;
  isLocal?: boolean;
}

interface State {
  step: Steps;
  token?: string;
}

export default class OtherTutorial extends React.PureComponent<Props, State> {
  state: State = { step: Steps.TOKEN };

  render() {
    const { component } = this.props;
    const projectKey = component.key;

    return (
      <PageContentFontWrapper className="sw-typo-default">
        <div className="sw-mb-4">
          <Title>{translate('onboarding.project_analysis.header')} </Title>
          <LightLabel>
            {translate('layout.must_be_configured')} Run analysis on <a href={getHostUrl() + '/project/extension/developer/project?id=' + projectKey}>Project Analysis</a> Page.
          </LightLabel>
        </div>
      </PageContentFontWrapper>
    );
  }
}
