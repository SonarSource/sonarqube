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
import * as React from 'react';
import * as PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import DetailsHeader from './DetailsHeader';
import DetailsContent from './DetailsContent';
import { getMetrics } from '../../../store/rootReducer';
import { fetchMetrics } from '../../../store/rootActions';
import { fetchQualityGate } from '../../../api/quality-gates';
import { Metric, QualityGate, Condition } from '../../../app/types';
import { checkIfDefault, addCondition, replaceCondition, deleteCondition } from '../utils';

interface OwnProps {
  onSetDefault: (qualityGate: QualityGate) => void;
  organization?: string;
  params: { id: number };
  qualityGates: QualityGate[];
  refreshQualityGates: () => Promise<void>;
}

interface StateToProps {
  metrics: { [key: string]: Metric };
}

interface DispatchToProps {
  fetchMetrics: () => void;
}

type Props = StateToProps & DispatchToProps & OwnProps;

interface State {
  loading: boolean;
  qualityGate?: QualityGate;
}

export class DetailsApp extends React.PureComponent<Props, State> {
  mounted = false;

  static contextTypes = {
    router: PropTypes.object.isRequired
  };

  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.props.fetchMetrics();
    this.fetchDetails();
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.params.id !== this.props.params.id) {
      this.setState({ loading: true });
      this.fetchDetails(nextProps);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchDetails = ({ organization, params } = this.props) => {
    return fetchQualityGate({ id: params.id, organization }).then(
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

  handleAddCondition = (metric: string) => {
    this.setState(({ qualityGate }) => {
      if (!qualityGate) {
        return undefined;
      }
      return { qualityGate: addCondition(qualityGate, metric) };
    });
  };

  handleSaveCondition = (newCondition: Condition, oldCondition: Condition) => {
    this.setState(({ qualityGate }) => {
      if (!qualityGate) {
        return undefined;
      }
      return { qualityGate: replaceCondition(qualityGate, newCondition, oldCondition) };
    });
  };

  handleRemoveCondition = (condition: Condition) => {
    this.setState(({ qualityGate }) => {
      if (!qualityGate) {
        return undefined;
      }
      return { qualityGate: deleteCondition(qualityGate, condition) };
    });
  };

  handleSetDefault = () => {
    this.setState(({ qualityGate }) => {
      if (!qualityGate) {
        return undefined;
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
    const { organization, metrics, refreshQualityGates } = this.props;
    const { qualityGate } = this.state;

    if (!qualityGate) {
      return null;
    }

    return (
      <>
        <Helmet title={qualityGate.name} />
        <div className="layout-page-main">
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
        </div>
      </>
    );
  }
}

const mapDispatchToProps: DispatchToProps = { fetchMetrics };

const mapStateToProps = (state: any): StateToProps => ({
  metrics: getMetrics(state)
});

export default connect<StateToProps, DispatchToProps, OwnProps>(
  mapStateToProps,
  mapDispatchToProps
)(DetailsApp);
