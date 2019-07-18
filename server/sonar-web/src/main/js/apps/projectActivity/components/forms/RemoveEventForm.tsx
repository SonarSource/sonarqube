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
import ConfirmModal from 'sonar-ui-common/components/controls/ConfirmModal';
import { translate } from 'sonar-ui-common/helpers/l10n';

interface Props {
  analysis: string;
  deleteEvent: (analysis: string, event: string) => Promise<void>;
  event: T.AnalysisEvent;
  header: string;
  removeEventQuestion: string;
  onClose: () => void;
}

export default class RemoveEventForm extends React.PureComponent<Props> {
  handleSubmit = () => {
    return this.props.deleteEvent(this.props.analysis, this.props.event.key);
  };

  render() {
    return (
      <ConfirmModal
        confirmButtonText={translate('delete')}
        header={this.props.header}
        isDestructive={true}
        onClose={this.props.onClose}
        onConfirm={this.handleSubmit}>
        {translate(this.props.removeEventQuestion)}
      </ConfirmModal>
    );
  }
}
