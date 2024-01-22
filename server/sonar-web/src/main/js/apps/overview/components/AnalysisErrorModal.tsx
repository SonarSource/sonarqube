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
import Modal from '../../../components/controls/Modal';
import { ResetButtonLink } from '../../../components/controls/buttons';
import { hasMessage, translate } from '../../../helpers/l10n';
import { Task } from '../../../types/tasks';
import { Component } from '../../../types/types';
import { AnalysisErrorMessage } from './AnalysisErrorMessage';
import { AnalysisLicenseError } from './AnalysisLicenseError';

interface Props {
  component: Component;
  currentTask: Task;
  onClose: () => void;
}

export function AnalysisErrorModal(props: Props) {
  const { component, currentTask } = props;

  const header = translate('error');

  const licenseError =
    currentTask.errorType &&
    hasMessage('license.component_navigation.button', currentTask.errorType);

  return (
    <Modal contentLabel={header} onRequestClose={props.onClose}>
      <header className="modal-head">
        <h2>{header}</h2>
      </header>

      <div className="modal-body modal-container">
        {licenseError ? (
          <AnalysisLicenseError currentTask={currentTask} />
        ) : (
          <AnalysisErrorMessage
            component={component}
            currentTask={currentTask}
            onLeave={props.onClose}
          />
        )}
      </div>

      <footer className="modal-foot">
        <ResetButtonLink onClick={props.onClose}>{translate('close')}</ResetButtonLink>
      </footer>
    </Modal>
  );
}
