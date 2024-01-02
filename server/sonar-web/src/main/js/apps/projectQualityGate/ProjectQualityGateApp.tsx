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
import { addGlobalSuccessMessage } from '../../helpers/globalMessages';
import { translate } from '../../helpers/l10n';
import { Component, QualityGate } from '../../types/types';
import { USE_SYSTEM_DEFAULT } from './constants';
import ProjectQualityGateAppRenderer from './ProjectQualityGateAppRenderer';

interface Props {
  component: Component;
  onComponentChange: (changes: {}) => void;
}

interface State {
  allQualityGates?: QualityGate[];
  currentQualityGate?: QualityGate;
  loading: boolean;
  selectedQualityGateId: string;
  submitting: boolean;
}

export class ProjectQualityGateApp extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    loading: true,
    selectedQualityGateId: USE_SYSTEM_DEFAULT,
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
    const { qualitygates } = await fetchQualityGates();
    return Promise.all(
      qualitygates.map(async (qg) => {
        const detailedQp = await fetchQualityGate({ id: qg.id }).catch(() => qg);
        return { ...detailedQp, ...qg };
      })
    );
  };

  fetchQualityGates = async () => {
    const { component } = this.props;
    this.setState({ loading: true });

    const [allQualityGates, currentQualityGate] = await Promise.all([
      this.fetchDetailedQualityGates(),
      getGateForProject({ project: component.key }),
    ]).catch(() => []);

    if (allQualityGates && currentQualityGate) {
      const usingDefault = await this.isUsingDefault(currentQualityGate);

      if (this.mounted) {
        this.setState({
          allQualityGates,
          currentQualityGate,
          selectedQualityGateId: usingDefault ? USE_SYSTEM_DEFAULT : currentQualityGate.id,
          loading: false,
        });
      }
    } else if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  handleSelect = (selectedQualityGateId: string) => {
    this.setState({ selectedQualityGateId });
  };

  handleSubmit = async () => {
    const { component } = this.props;
    const { allQualityGates, currentQualityGate, selectedQualityGateId } = this.state;

    if (allQualityGates === undefined || currentQualityGate === undefined) {
      return;
    }

    this.setState({ submitting: true });

    if (selectedQualityGateId === USE_SYSTEM_DEFAULT) {
      await dissociateGateWithProject({
        gateId: currentQualityGate.id,
        projectKey: component.key,
      }).catch(() => {
        /* noop */
      });
    } else {
      await associateGateWithProject({
        gateId: selectedQualityGateId,
        projectKey: component.key,
      }).catch(() => {
        /* noop */
      });
    }

    if (this.mounted) {
      addGlobalSuccessMessage(translate('project_quality_gate.successfully_updated'));

      const newGate =
        selectedQualityGateId === USE_SYSTEM_DEFAULT
          ? allQualityGates.find((gate) => gate.isDefault)
          : allQualityGates.find((gate) => gate.id === selectedQualityGateId);

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

    const { allQualityGates, currentQualityGate, loading, selectedQualityGateId, submitting } =
      this.state;

    return (
      <ProjectQualityGateAppRenderer
        allQualityGates={allQualityGates}
        currentQualityGate={currentQualityGate}
        loading={loading}
        onSubmit={this.handleSubmit}
        onSelect={this.handleSelect}
        selectedQualityGateId={selectedQualityGateId}
        submitting={submitting}
      />
    );
  }
}

export default withComponentContext(ProjectQualityGateApp);
