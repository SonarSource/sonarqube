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
import { addGlobalSuccessMessage } from 'design-system';
import * as React from 'react';
import {
  associateGateWithProject,
  dissociateGateWithProject,
  fetchQualityGate,
  fetchQualityGates,
  getGateForProject,
  searchProjects,
} from '../../api/quality-gates';
import withComponentContext from '../../app/components/componentContext/withComponentContext';
import handleRequiredAuthorization from '../../app/utils/handleRequiredAuthorization';
import { addGlobalErrorMessageFromAPI } from '../../helpers/globalMessages';
import { translate } from '../../helpers/l10n';
import { Component, Organization, QualityGate } from '../../types/types';
import ProjectQualityGateAppRenderer from './ProjectQualityGateAppRenderer';
import { USE_SYSTEM_DEFAULT } from './constants';
import { withOrganizationContext } from "../organizations/OrganizationContext";

interface Props {
  organization: Organization;
  component: Component;
  onComponentChange: (changes: {}) => void;
}

interface State {
  allQualityGates?: QualityGate[];
  currentQualityGate?: QualityGate;
  loading: boolean;
  selectedQualityGateName: string;
  submitting: boolean;
}

class ProjectQualityGateApp extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    loading: true,
    selectedQualityGateName: USE_SYSTEM_DEFAULT,
    submitting: false,
  };

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

  checkPermissions = () => {
    const { configuration } = this.props.component;
    const hasPermission = configuration && configuration.showQualityGates;
    return !!hasPermission;
  };

  isUsingDefault = async (qualityGate: QualityGate) => {
    const { component } = this.props;

    if (!qualityGate.isDefault) {
      return false;
    }

    // If this is the default Quality Gate, check if it was explicitly
    // selected, or if we're inheriting the system default.
    const selected = await searchProjects({
      organization: component.organization,
      gateName: qualityGate.name,
      query: component.key,
    })
      .then(({ results }) => {
        return Boolean(results.find((r) => r.key === component.key)?.selected);
      })
      .catch(() => false);

    // If it's NOT selected, it means we're following the system default.
    return !selected;
  };

  fetchDetailedQualityGates = async () => {
    const { qualitygates } = await fetchQualityGates({ organization: this.props.organization.kee });
    return Promise.all(
      qualitygates.map(async (qg) => {
        const detailedQp = await fetchQualityGate({ name: qg.name, organization: this.props.organization.kee }).catch(() => qg);
        return { ...detailedQp, ...qg };
      }),
    );
  };

  fetchQualityGates = async () => {
    const { component } = this.props;
    this.setState({ loading: true });

    const [allQualityGates, currentQualityGate] = await Promise.all([
      this.fetchDetailedQualityGates(),
      getGateForProject({ organization: component.organization, project: component.key }),
    ]).catch((error) => {
      addGlobalErrorMessageFromAPI(error);
      return [];
    });

    if (allQualityGates && currentQualityGate) {
      const usingDefault = await this.isUsingDefault(currentQualityGate);

      if (this.mounted) {
        this.setState({
          allQualityGates,
          currentQualityGate,
          selectedQualityGateName: usingDefault ? USE_SYSTEM_DEFAULT : currentQualityGate.name,
          loading: false,
        });
      }
    } else if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  handleSelect = (selectedQualityGateName: string) => {
    this.setState({ selectedQualityGateName });
  };

  handleSubmit = async () => {
    const { component } = this.props;
    const { allQualityGates, currentQualityGate, selectedQualityGateName } = this.state;

    if (allQualityGates === undefined || currentQualityGate === undefined) {
      return;
    }

    this.setState({ submitting: true });

    if (selectedQualityGateName === USE_SYSTEM_DEFAULT) {
      await dissociateGateWithProject({
        organization: component.organization,
        projectKey: component.key,
      }).catch(() => {
        /* noop */
      });
    } else {
      await associateGateWithProject({
        organization: component.organization,
        gateName: selectedQualityGateName,
        projectKey: component.key,
      }).catch(() => {
        /* noop */
      });
    }

    if (this.mounted) {
      addGlobalSuccessMessage(translate('project_quality_gate.successfully_updated'));

      const newGate =
        selectedQualityGateName === USE_SYSTEM_DEFAULT
          ? allQualityGates.find((gate) => gate.isDefault)
          : allQualityGates.find((gate) => gate.name === selectedQualityGateName);

      if (newGate) {
        this.setState({ currentQualityGate: newGate, submitting: false });
        this.props.onComponentChange({ qualityGate: newGate });
      }
    }
  };

  render() {
    if (!this.checkPermissions()) {
      return null;
    }

    const { component } = this.props;

    const { allQualityGates, currentQualityGate, loading, selectedQualityGateName, submitting } =
      this.state;

    return (
      <ProjectQualityGateAppRenderer
        organization={this.props.organization}
        allQualityGates={allQualityGates}
        component={component}
        currentQualityGate={currentQualityGate}
        loading={loading}
        onSubmit={this.handleSubmit}
        onSelect={this.handleSelect}
        selectedQualityGateName={selectedQualityGateName}
        submitting={submitting}
      />
    );
  }
}

export default withComponentContext(withOrganizationContext(ProjectQualityGateApp));
