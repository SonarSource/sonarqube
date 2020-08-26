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
import { WithRouterProps } from 'react-router';
import { getAlmDefinitionsNoCatch, getProjectAlmBinding } from '../../api/alm-settings';
import { AlmBindingDefinition, AlmKeys, ProjectAlmBindingResponse } from '../../types/alm-settings';
import { withRouter } from '../hoc/withRouter';
import TutorialSelectionRenderer from './TutorialSelectionRenderer';
import { TutorialModes } from './types';

interface Props extends Pick<WithRouterProps, 'router' | 'location'> {
  component: T.Component;
  currentUser: T.LoggedInUser;
}

interface State {
  almBinding?: AlmBindingDefinition;
  forceManual: boolean;
  loading: boolean;
  projectBinding?: ProjectAlmBindingResponse;
}

export class TutorialSelection extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    forceManual: true,
    loading: true
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchAlmBindings();
  }

  fetchAlmBindings = async () => {
    const { component } = this.props;

    const [almDefinitions, projectBinding] = await Promise.all([
      getAlmDefinitionsNoCatch().catch(() => undefined),
      getProjectAlmBinding(component.key).catch(() => undefined)
    ]);

    if (this.mounted) {
      // We only support Bitbucket, GitHub & Gitlab for now.
      if (
        projectBinding === undefined ||
        ![AlmKeys.Bitbucket, AlmKeys.GitHub, AlmKeys.GitLab].includes(projectBinding.alm)
      ) {
        this.setState({ loading: false, forceManual: true });
      } else {
        let almBinding;
        if (almDefinitions !== undefined) {
          const specificDefinitions = almDefinitions[projectBinding.alm] as AlmBindingDefinition[];
          almBinding = specificDefinitions.find(d => d.key === projectBinding.key);
        }
        this.setState({ almBinding, forceManual: false, projectBinding, loading: false });
      }
    }
  };

  handleSelectTutorial = (selectedTutorial: TutorialModes) => {
    const {
      router,
      location: { pathname, query }
    } = this.props;

    router.push({
      pathname,
      query: { ...query, selectedTutorial }
    });
  };

  render() {
    const { component, currentUser, location } = this.props;
    const { almBinding, forceManual, loading, projectBinding } = this.state;

    const selectedTutorial: TutorialModes | undefined = forceManual
      ? TutorialModes.Manual
      : location.query?.selectedTutorial;

    return (
      <TutorialSelectionRenderer
        almBinding={almBinding}
        component={component}
        currentUser={currentUser}
        loading={loading}
        onSelectTutorial={this.handleSelectTutorial}
        projectBinding={projectBinding}
        selectedTutorial={selectedTutorial}
      />
    );
  }
}

export default withRouter(TutorialSelection);
