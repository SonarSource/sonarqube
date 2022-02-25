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
import { connect } from 'react-redux';
import { getGlobalSettingValue, Store } from '../../../store/rootReducer';
import { AdminPageExtension } from '../../../types/extension';
import { Extension } from '../../../types/types';
import { fetchValues } from '../../settings/store/actions';
import '../style.css';
import { HousekeepingPolicy, RangeOption } from '../utils';
import AuditAppRenderer from './AuditAppRenderer';

interface Props {
  auditHousekeepingPolicy: HousekeepingPolicy;
  fetchValues: typeof fetchValues;
  adminPages: Extension[];
}

interface State {
  dateRange?: { from?: Date; to?: Date };
  hasGovernanceExtension?: boolean;
  downloadStarted: boolean;
  selection: RangeOption;
}

export class AuditApp extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    const hasGovernanceExtension = Boolean(
      props.adminPages?.find(e => e.key === AdminPageExtension.GovernanceConsole)
    );
    this.state = {
      downloadStarted: false,
      selection: RangeOption.Today,
      hasGovernanceExtension
    };
  }

  componentDidMount() {
    const { hasGovernanceExtension } = this.state;

    if (hasGovernanceExtension) {
      this.props.fetchValues(['sonar.dbcleaner.auditHousekeeping']);
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.adminPages !== this.props.adminPages) {
      const hasGovernanceExtension = Boolean(
        this.props.adminPages?.find(e => e.key === AdminPageExtension.GovernanceConsole)
      );
      this.setState({
        hasGovernanceExtension
      });
    }
  }

  handleDateSelection = (dateRange: { from?: Date; to?: Date }) =>
    this.setState({ dateRange, downloadStarted: false, selection: RangeOption.Custom });

  handleOptionSelection = (selection: RangeOption) =>
    this.setState({ dateRange: undefined, downloadStarted: false, selection });

  handleStartDownload = () => {
    setTimeout(() => {
      this.setState({ downloadStarted: true });
    }, 0);
  };

  render() {
    const { hasGovernanceExtension, ...auditAppRendererProps } = this.state;
    const { auditHousekeepingPolicy } = this.props;

    if (!hasGovernanceExtension) {
      return null;
    }

    return (
      <AuditAppRenderer
        handleDateSelection={this.handleDateSelection}
        handleOptionSelection={this.handleOptionSelection}
        handleStartDownload={this.handleStartDownload}
        housekeepingPolicy={auditHousekeepingPolicy || HousekeepingPolicy.Monthly}
        {...auditAppRendererProps}
      />
    );
  }
}

const mapDispatchToProps = { fetchValues };

const mapStateToProps = (state: Store) => {
  const settingValue = getGlobalSettingValue(state, 'sonar.dbcleaner.auditHousekeeping');
  return {
    auditHousekeepingPolicy: settingValue?.value as HousekeepingPolicy
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(AuditApp);
