/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { Helmet } from 'react-helmet-async';
import { fetchQualityGate } from '../../../api/quality-gates';
import addGlobalSuccessMessage from '../../../app/utils/addGlobalSuccessMessage';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { Condition, QualityGate } from '../../../types/types';
import { addCondition, checkIfDefault, deleteCondition, replaceCondition } from '../utils';
import DetailsContent from './DetailsContent';
import DetailsHeader from './DetailsHeader';

interface Props {
  id: string;
  onSetDefault: (qualityGate: QualityGate) => void;
  qualityGates: QualityGate[];
  refreshQualityGates: () => Promise<void>;
}

interface State {
  loading: boolean;
  qualityGate?: QualityGate;
  updatedConditionId?: number;
}

export default class Details extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchDetails();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.id !== this.props.id) {
      this.fetchDetails();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchDetails = () => {
    const { id } = this.props;
    this.setState({ loading: true });
    return fetchQualityGate({ id }).then(
      qualityGate => {
        if (this.mounted) {
          this.setState({ loading: false, qualityGate, updatedConditionId: undefined });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  handleAddCondition = (condition: Condition) => {
    this.setState(({ qualityGate }) => {
      if (!qualityGate) {
        return null;
      }
      addGlobalSuccessMessage(translate('quality_gates.condition_added'));
      return {
        qualityGate: addCondition(qualityGate, condition),
        updatedConditionId: condition.id
      };
    });
  };

  handleSaveCondition = (newCondition: Condition, oldCondition: Condition) => {
    this.setState(({ qualityGate }) => {
      if (!qualityGate) {
        return null;
      }
      addGlobalSuccessMessage(translate('quality_gates.condition_updated'));
      return {
        qualityGate: replaceCondition(qualityGate, newCondition, oldCondition),
        updatedConditionId: newCondition.id
      };
    });
  };

  handleRemoveCondition = (condition: Condition) => {
    this.setState(({ qualityGate }) => {
      if (!qualityGate) {
        return null;
      }
      addGlobalSuccessMessage(translate('quality_gates.condition_deleted'));
      return {
        qualityGate: deleteCondition(qualityGate, condition),
        updatedConditionId: undefined
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
        actions: { ...qualityGate.actions, delete: false, setAsDefault: false }
      };
      return { qualityGate: newQualityGate };
    });
  };

  render() {
    const { refreshQualityGates } = this.props;
    const { loading, qualityGate, updatedConditionId } = this.state;

    return (
      <div className="layout-page-main">
        <DeferredSpinner loading={loading} timeout={200}>
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
                isDefault={checkIfDefault(qualityGate, this.props.qualityGates)}
                onAddCondition={this.handleAddCondition}
                onRemoveCondition={this.handleRemoveCondition}
                onSaveCondition={this.handleSaveCondition}
                qualityGate={qualityGate}
                updatedConditionId={updatedConditionId}
              />
            </>
          )}
        </DeferredSpinner>
      </div>
    );
  }
}
