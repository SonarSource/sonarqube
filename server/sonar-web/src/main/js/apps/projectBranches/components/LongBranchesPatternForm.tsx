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
import SettingForm from './SettingForm';
import { translate } from '../../../helpers/l10n';
import Modal from '../../../components/controls/Modal';

interface Props {
  onChange: () => void;
  onClose: () => void;
  project: string;
  setting: T.SettingValue;
}

export default function LongBranchesPatternForm(props: Props) {
  const header = translate('branches.detection_of_long_living_branches');

  return (
    <Modal contentLabel={header} onRequestClose={props.onClose}>
      <header className="modal-head">
        <h2>{header}</h2>
      </header>
      <SettingForm {...props} />
    </Modal>
  );
}
