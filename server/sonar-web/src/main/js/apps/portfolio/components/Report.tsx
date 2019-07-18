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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import Dropdown from 'sonar-ui-common/components/controls/Dropdown';
import DropdownIcon from 'sonar-ui-common/components/icons/DropdownIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getReportStatus, getReportUrl, ReportStatus } from '../../../api/report';
import Subscription from './Subscription';

interface Props {
  component: { key: string; name: string };
}

interface State {
  loading: boolean;
  status?: ReportStatus;
}

export default class Report extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.loadStatus();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadStatus = () => {
    getReportStatus(this.props.component.key).then(
      status => {
        if (this.mounted) {
          this.setState({ status, loading: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  render() {
    const { component } = this.props;
    const { status, loading } = this.state;

    if (loading || !status) {
      return null;
    }

    return status.canSubscribe ? (
      <Dropdown
        overlay={
          <ul className="menu">
            <li>
              <a
                download={component.name + ' - Executive Report.pdf'}
                href={getReportUrl(component.key)}
                target="_blank">
                {translate('report.print')}
              </a>
            </li>
            <li>
              <Subscription
                component={component.key}
                onSubscribe={this.loadStatus}
                status={status}
              />
            </li>
          </ul>
        }
        tagName="li">
        <Button className="dropdown-toggle">
          {translate('portfolio.pdf_report')}
          <DropdownIcon className="spacer-left icon-half-transparent" />
        </Button>
      </Dropdown>
    ) : (
      <a
        className="button"
        download={component.name + ' - Executive Report.pdf'}
        href={getReportUrl(component.key)}
        target="_blank">
        {translate('report.print')}
      </a>
    );
  }
}
