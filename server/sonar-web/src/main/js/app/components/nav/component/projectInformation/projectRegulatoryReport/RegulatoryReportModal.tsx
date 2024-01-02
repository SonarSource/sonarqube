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
import { translate } from '../../../../../../helpers/l10n';
import { Component } from '../../../../../../types/types';
import Modal from '../../../../../../components/controls/Modal';
import RegulatoryReport from './RegulatoryReport';
import ClickEventBoundary from '../../../../../../components/controls/ClickEventBoundary';
import { BranchLike } from '../../../../../../types/branch-like';

interface Props {
  component: Component;
  branchLike?: BranchLike;
  onClose: () => void;
}

export default function RegulatoryReportModal(props: Props) {
  const { component, branchLike } = props;
  return (
    <Modal contentLabel={translate('regulatory_report.page')} onRequestClose={props.onClose}>
      <ClickEventBoundary>
        <form>
          <RegulatoryReport component={component} branchLike={branchLike} onClose={props.onClose} />
        </form>
      </ClickEventBoundary>
    </Modal>
  );
}
