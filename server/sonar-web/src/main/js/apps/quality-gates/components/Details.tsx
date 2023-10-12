/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import { Spinner } from 'design-system';
import { clone } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { fetchQualityGate } from '../../../api/quality-gates';
import { addGlobalSuccessMessage } from '../../../helpers/globalMessages';
import { translate } from '../../../helpers/l10n';
import { Condition, QualityGate } from '../../../types/types';
import { addCondition, deleteCondition, replaceCondition } from '../utils';
import DetailsContent from './DetailsContent';
import DetailsHeader from './DetailsHeader';

interface Props {
  qualityGateName: string;
  onSetDefault: (qualityGate: QualityGate) => void;
  refreshQualityGates: () => Promise<void>;
}

interface State {
  loading: boolean;
  qualityGate?: QualityGate;
  updatedConditionId?: string;
}

export default class Details extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchDetails();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.qualityGateName !== this.props.qualityGateName) {
      this.fetchDetails();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchDetails = () => {
    const { qualityGateName } = this.props;

    this.setState({ loading: true });
    return fetchQualityGate({ name: qualityGateName }).then(
      (qualityGate) => {
        if (this.mounted) {
          this.setState({ loading: false, qualityGate, updatedConditionId: undefined });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  };

  handleAddCondition = (condition: Condition) => {
    this.setState(({ qualityGate }) => {
      if (!qualityGate) {
        return null;
      }
      addGlobalSuccessMessage(translate('quality_gates.condition_added'));

      const updatedQualityGate = addCondition(clone(qualityGate), condition);
      if (qualityGate.caycStatus !== updatedQualityGate.caycStatus) {
        this.props.refreshQualityGates();
      }

      return {
        qualityGate: updatedQualityGate,
        updatedConditionId: condition.id,
      };
    });
  };

  handleSaveCondition = (newCondition: Condition, oldCondition: Condition) => {
    this.setState(({ qualityGate }) => {
      if (!qualityGate) {
        return null;
      }
      addGlobalSuccessMessage(translate('quality_gates.condition_updated'));
      const updatedQualityGate = replaceCondition(clone(qualityGate), newCondition, oldCondition);
      if (qualityGate.caycStatus !== updatedQualityGate.caycStatus) {
        this.props.refreshQualityGates();
      }
      return {
        qualityGate: updatedQualityGate,
        updatedConditionId: newCondition.id,
      };
    });
  };

  handleRemoveCondition = (condition: Condition) => {
    this.setState(({ qualityGate }) => {
      if (!qualityGate) {
        return null;
      }
      addGlobalSuccessMessage(translate('quality_gates.condition_deleted'));
      const updatedQualityGate = deleteCondition(clone(qualityGate), condition);
      if (qualityGate.caycStatus !== updatedQualityGate.caycStatus) {
        this.props.refreshQualityGates();
      }
      return {
        qualityGate: updatedQualityGate,
        updatedConditionId: undefined,
      };
    });
  };

  handleSetDefault = () => {
    this.setState(({ qualityGate }) => {
      if (!qualityGate) {
        return null;
      }
      this.props.onSetDefault(qualityGate);
      const newQualityGate: QualityGate = {
        ...qualityGate,
        actions: { ...qualityGate.actions, delete: false, setAsDefault: false },
      };
      return { qualityGate: newQualityGate };
    });
  };

  render() {
    const { refreshQualityGates } = this.props;
    const { loading, qualityGate, updatedConditionId } = this.state;

    return (
      <main className="layout-page-main">
        <Spinner loading={loading}>
          {qualityGate && (
            <>
              <Helmet defer={false} title={qualityGate.name} />
              <DetailsHeader
                onSetDefault={this.handleSetDefault}
                qualityGate={qualityGate}
                refreshItem={this.fetchDetails}
                refreshList={refreshQualityGates}
              />
              <DetailsContent
                onAddCondition={this.handleAddCondition}
                onRemoveCondition={this.handleRemoveCondition}
                onSaveCondition={this.handleSaveCondition}
                qualityGate={qualityGate}
                updatedConditionId={updatedConditionId}
              />
            </>
          )}
        </Spinner>
      </main>
    );
  }
}
