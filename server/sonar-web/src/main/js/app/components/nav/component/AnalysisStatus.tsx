/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { DeferredSpinner, FlagMessage, Link } from 'design-system';
import * as React from 'react';
import AnalysisWarningsModal from '../../../../components/common/AnalysisWarningsModal';
import { translate } from '../../../../helpers/l10n';
import { Task, TaskStatuses, TaskWarning } from '../../../../types/tasks';
import { Component } from '../../../../types/types';
import { AnalysisErrorModal } from './AnalysisErrorModal';

export interface HeaderMetaProps {
  currentTask?: Task;
  currentTaskOnSameBranch?: boolean;
  component: Component;
  isInProgress?: boolean;
  isPending?: boolean;
  onWarningDismiss: () => void;
  warnings: TaskWarning[];
}

export function AnalysisStatus(props: HeaderMetaProps) {
  const { component, currentTask, currentTaskOnSameBranch, isInProgress, isPending, warnings } =
    props;

  const [modalIsVisible, setDisplayModal] = React.useState(false);
  const openModal = React.useCallback(() => {
    setDisplayModal(true);
  }, [setDisplayModal]);
  const closeModal = React.useCallback(() => {
    setDisplayModal(false);
  }, [setDisplayModal]);

  if (isInProgress || isPending) {
    return (
      <div className="sw-flex sw-items-center">
        <DeferredSpinner timeout={0} />
        <span className="sw-ml-1">
          {isInProgress
            ? translate('project_navigation.analysis_status.in_progress')
            : translate('project_navigation.analysis_status.pending')}
        </span>
      </div>
    );
  }

  if (currentTask?.status === TaskStatuses.Failed) {
    return (
      <>
        <FlagMessage ariaLabel={translate('alert.tooltip.error')} variant="error">
          <span>{translate('project_navigation.analysis_status.failed')}</span>
          <Link
            className="sw-ml-1"
            blurAfterClick={true}
            onClick={openModal}
            preventDefault={true}
            to={{}}
          >
            {translate('project_navigation.analysis_status.details_link')}
          </Link>
        </FlagMessage>
        {modalIsVisible && (
          <AnalysisErrorModal
            component={component}
            currentTask={currentTask}
            currentTaskOnSameBranch={currentTaskOnSameBranch}
            onClose={closeModal}
          />
        )}
      </>
    );
  }

  if (warnings.length > 0) {
    return (
      <>
        <FlagMessage ariaLabel={translate('alert.tooltip.warning')} variant="warning">
          <span>{translate('project_navigation.analysis_status.warnings')}</span>
          <Link
            className="sw-ml-1"
            blurAfterClick={true}
            onClick={openModal}
            preventDefault={true}
            to={{}}
          >
            {translate('project_navigation.analysis_status.details_link')}
          </Link>
        </FlagMessage>
        {modalIsVisible && (
          <AnalysisWarningsModal
            componentKey={component.key}
            onClose={closeModal}
            taskId={currentTask?.id}
            onWarningDismiss={props.onWarningDismiss}
            warnings={warnings}
          />
        )}
      </>
    );
  }

  return null;
}
