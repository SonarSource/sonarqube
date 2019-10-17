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
import { ResetButtonLink, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import SimpleModal from 'sonar-ui-common/components/controls/SimpleModal';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';

export interface AlmPRDecorationFormModalProps {
  canSubmit: () => boolean;
  children: React.ReactNode;
  onCancel: () => void;
  onSubmit: () => void;
  originalKey: string;
}

export default function AlmPRDecorationFormModalRenderer(props: AlmPRDecorationFormModalProps) {
  const { children, originalKey } = props;
  const header = translate('settings.pr_decoration.form.header', originalKey ? 'edit' : 'create');

  return (
    <SimpleModal header={header} onClose={props.onCancel} onSubmit={props.onSubmit} size="medium">
      {({ onCloseClick, onFormSubmit, submitting }) => (
        <form className="views-form" onSubmit={onFormSubmit}>
          <div className="modal-head">
            <h2>{header}</h2>
          </div>

          <div className="modal-body modal-container">{children}</div>

          <div className="modal-foot">
            <DeferredSpinner className="spacer-right" loading={submitting} />
            <SubmitButton disabled={submitting || !props.canSubmit()}>
              {translate('settings.pr_decoration.form.save')}
            </SubmitButton>
            <ResetButtonLink onClick={onCloseClick}>{translate('cancel')}</ResetButtonLink>
          </div>
        </form>
      )}
    </SimpleModal>
  );
}
