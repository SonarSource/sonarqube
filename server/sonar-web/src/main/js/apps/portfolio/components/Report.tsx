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
import SubscriptionContainer from './SubscriptionContainer';
import { getReportStatus, ReportStatus, getReportUrl } from '../../../api/report';
import { translate } from '../../../helpers/l10n';

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

  loadStatus() {
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
  }

  renderHeader = () => <h4>{translate('report.page')}</h4>;

  render() {
    const { component } = this.props;
    const { status, loading } = this.state;

    if (loading) {
      return (
        <div>
          {this.renderHeader()}
          <i className="spinner" />
        </div>
      );
    }

    if (!status) {
      return null;
    }

    return (
      <div>
        {this.renderHeader()}

        {!status.canDownload && (
          <div className="note js-report-cant-download">{translate('report.cant_download')}</div>
        )}

        {status.canDownload && (
          <div className="js-report-can-download">
            {translate('report.can_download')}
            <div className="spacer-top">
              <a
                className="button js-report-download"
                href={getReportUrl(component.key)}
                target="_blank"
                download={component.name + ' - Executive Report.pdf'}>
                {translate('report.print')}
              </a>
            </div>
          </div>
        )}

        {status.canSubscribe && <SubscriptionContainer component={component.key} status={status} />}
      </div>
    );
  }
}
