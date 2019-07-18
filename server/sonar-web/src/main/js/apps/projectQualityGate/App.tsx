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
import Helmet from 'react-helmet';
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  associateGateWithProject,
  dissociateGateWithProject,
  fetchQualityGates,
  getGateForProject
} from '../../api/quality-gates';
import A11ySkipTarget from '../../app/components/a11y/A11ySkipTarget';
import Suggestions from '../../app/components/embed-docs-modal/Suggestions';
import addGlobalSuccessMessage from '../../app/utils/addGlobalSuccessMessage';
import handleRequiredAuthorization from '../../app/utils/handleRequiredAuthorization';
import Form from './Form';
import Header from './Header';

interface Props {
  component: T.Component;
  onComponentChange: (changes: {}) => void;
}

interface State {
  allGates?: T.QualityGate[];
  gate?: T.QualityGate;
  loading: boolean;
}

export default class App extends React.PureComponent<Props> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    if (this.checkPermissions()) {
      this.fetchQualityGates();
    } else {
      handleRequiredAuthorization();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  checkPermissions() {
    const { configuration } = this.props.component;
    const hasPermission = configuration && configuration.showQualityGates;
    return !!hasPermission;
  }

  fetchQualityGates() {
    const { component } = this.props;
    this.setState({ loading: true });
    Promise.all([
      fetchQualityGates({ organization: component.organization }),
      getGateForProject({ organization: component.organization, project: component.key })
    ]).then(
      ([{ qualitygates: allGates }, gate]) => {
        if (this.mounted) {
          this.setState({ allGates, gate, loading: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  }

  handleChangeGate = (oldId?: number, newId?: number) => {
    const { allGates } = this.state;
    if ((!oldId && !newId) || !allGates) {
      return Promise.resolve();
    }

    const { component } = this.props;
    const requestData = {
      gateId: newId ? newId : oldId!,
      organization: component.organization,
      projectKey: component.key
    };
    const request = newId
      ? associateGateWithProject(requestData)
      : dissociateGateWithProject(requestData);

    return request.then(() => {
      if (this.mounted) {
        addGlobalSuccessMessage(translate('project_quality_gate.successfully_updated'));
        if (newId) {
          const newGate = allGates.find(gate => gate.id === newId);
          if (newGate) {
            this.setState({ gate: newGate });
            this.props.onComponentChange({ qualityGate: newGate });
          }
        } else {
          this.setState({ gate: undefined });
        }
      }
    });
  };

  render() {
    if (!this.checkPermissions()) {
      return null;
    }

    const { allGates, gate, loading } = this.state;

    return (
      <div className="page page-limited" id="project-quality-gate">
        <Suggestions suggestions="project_quality_gate" />
        <Helmet title={translate('project_quality_gate.page')} />
        <A11ySkipTarget anchor="qg_main" />
        <Header />
        {loading ? (
          <i className="spinner" />
        ) : (
          allGates && <Form allGates={allGates} gate={gate} onChange={this.handleChangeGate} />
        )}
      </div>
    );
  }
}
