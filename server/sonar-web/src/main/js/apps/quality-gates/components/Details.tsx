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
import { connect } from 'react-redux';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { fetchQualityGate } from '../../../api/quality-gates';
import { fetchMetrics } from '../../../store/rootActions';
import { getMetrics, Store } from '../../../store/rootReducer';
import { addCondition, checkIfDefault, deleteCondition, replaceCondition } from '../utils';
import DetailsContent from './DetailsContent';
import DetailsHeader from './DetailsHeader';

interface OwnProps {
  id: string;
  onSetDefault: (qualityGate: T.QualityGate) => void;
  organization?: string;
  qualityGates: T.QualityGate[];
  refreshQualityGates: () => Promise<void>;
}

interface StateToProps {
  metrics: T.Dict<T.Metric>;
}

interface DispatchToProps {
  fetchMetrics: () => void;
}

type Props = StateToProps & DispatchToProps & OwnProps;

interface State {
  loading: boolean;
  qualityGate?: T.QualityGate;
}

export class Details extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.props.fetchMetrics();
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
    const { id, organization } = this.props;
    this.setState({ loading: true });
    return fetchQualityGate({ id, organization }).then(
      qualityGate => {
        if (this.mounted) {
          this.setState({ loading: false, qualityGate });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  handleAddCondition = (condition: T.Condition) => {
    this.setState(({ qualityGate }) => {
      if (!qualityGate) {
        return null;
      }
      return { qualityGate: addCondition(qualityGate, condition) };
    });
  };

  handleSaveCondition = (newCondition: T.Condition, oldCondition: T.Condition) => {
    this.setState(({ qualityGate }) => {
      if (!qualityGate) {
        return null;
      }
      return { qualityGate: replaceCondition(qualityGate, newCondition, oldCondition) };
    });
  };

  handleRemoveCondition = (condition: T.Condition) => {
    this.setState(({ qualityGate }) => {
      if (!qualityGate) {
        return null;
      }
      return { qualityGate: deleteCondition(qualityGate, condition) };
    });
  };

  handleSetDefault = () => {
    this.setState(({ qualityGate }) => {
      if (!qualityGate) {
        return null;
      }
      this.props.onSetDefault(qualityGate);
      const newQualityGate: T.QualityGate = {
        ...qualityGate,
        actions: { ...qualityGate.actions, delete: false, setAsDefault: false }
      };
      return { qualityGate: newQualityGate };
    });
  };

  render() {
    const { organization, metrics, refreshQualityGates } = this.props;
    const { loading, qualityGate } = this.state;

    return (
      <div className="layout-page-main">
        <DeferredSpinner loading={loading} timeout={200}>
          {qualityGate && (
            <>
              <Helmet title={qualityGate.name} />
              <DetailsHeader
                onSetDefault={this.handleSetDefault}
                organization={organization}
                qualityGate={qualityGate}
                refreshItem={this.fetchDetails}
                refreshList={refreshQualityGates}
              />
              <DetailsContent
                isDefault={checkIfDefault(qualityGate, this.props.qualityGates)}
                metrics={metrics}
                onAddCondition={this.handleAddCondition}
                onRemoveCondition={this.handleRemoveCondition}
                onSaveCondition={this.handleSaveCondition}
                organization={organization}
                qualityGate={qualityGate}
              />
            </>
          )}
        </DeferredSpinner>
      </div>
    );
  }
}

const mapDispatchToProps: DispatchToProps = { fetchMetrics };

const mapStateToProps = (state: Store): StateToProps => ({
  metrics: getMetrics(state)
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Details);
