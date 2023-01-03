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
import * as React from 'react';
import { ResetButtonLink, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import Modal from 'sonar-ui-common/components/controls/Modal';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';

export interface AlmBindingDefinitionFormModalProps {
  action: 'edit' | 'create';
  canSubmit: () => boolean;
  children: React.ReactNode;
  help?: React.ReactNode;
  isSecondInstance: boolean;
  onCancel: () => void;
  onSubmit: () => void | Promise<void | Response>;
}

export default function AlmBindingDefinitionFormModalRenderer(
  props: AlmBindingDefinitionFormModalProps
) {
  const { action, children, help, isSecondInstance } = props;
  const header = translate('settings.almintegration.form.header', action);

  const handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    props.onSubmit();
  };

  return (
    <Modal
      contentLabel={header}
      onRequestClose={props.onCancel}
      shouldCloseOnOverlayClick={false}
      size="medium">
      <form className="views-form" onSubmit={handleSubmit}>
        <div className="modal-head">
          <h2>{header}</h2>
        </div>

        <div className="modal-body modal-container">
          {isSecondInstance && action === 'create' && (
            <Alert className="big-spacer-bottom" variant="warning">
              {translate('settings.almintegration.form.second_instance_warning')}
            </Alert>
          )}

          <div className="display-flex-start">
            <div className="flex-1">{children}</div>

            {help ? (
              <Alert className="huge-spacer-left flex-1" variant="info">
                {help}
              </Alert>
            ) : (
              <div className="flex-1" />
            )}
          </div>
        </div>

        <div className="modal-foot">
          <SubmitButton disabled={!props.canSubmit()}>
            {translate('settings.almintegration.form.save')}
          </SubmitButton>
          <ResetButtonLink onClick={props.onCancel}>{translate('cancel')}</ResetButtonLink>
        </div>
      </form>
    </Modal>
  );
}
