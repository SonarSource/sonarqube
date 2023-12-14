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
import React, { useState } from 'react';
import Modal from '../../components/controls/Modal';
import Radio from '../../components/controls/Radio';
import { Button, ResetButtonLink } from '../../components/controls/buttons';
import { Alert } from '../../components/ui/Alert';
import { translate } from '../../helpers/l10n';
import { useGithubProvisioningEnabledQuery } from '../../queries/identity-provider/github';
import { Visibility } from '../../types/component';

export interface Props {
  defaultVisibility: Visibility;
  onClose: () => void;
  onConfirm: (visiblity: Visibility) => void;
}

export default function ChangeDefaultVisibilityForm(props: Props) {
  const [visibility, setVisibility] = useState(props.defaultVisibility);
  const { data: githubProbivisioningEnabled } = useGithubProvisioningEnabledQuery();

  const handleConfirmClick = () => {
    props.onConfirm(visibility);
    props.onClose();
  };

  const handleVisibilityChange = (visibility: Visibility) => {
    setVisibility(visibility);
  };

  const header = translate('settings.projects.change_visibility_form.header');

  return (
    <Modal contentLabel={header} onRequestClose={props.onClose}>
      <header className="modal-head">
        <h2>{header}</h2>
      </header>

      <div className="modal-body">
        {Object.values(Visibility).map((visibilityValue) => (
          <div className="big-spacer-bottom" key={visibilityValue}>
            <Radio
              value={visibilityValue}
              checked={visibility === visibilityValue}
              onCheck={handleVisibilityChange}
            >
              <div>
                {translate('visibility', visibilityValue)}
                <p className="text-muted spacer-top">
                  {translate('visibility', visibilityValue, 'description.short')}
                </p>
              </div>
            </Radio>
          </div>
        ))}

        <Alert variant="warning">
          {translate(
            `settings.projects.change_visibility_form.warning${
              githubProbivisioningEnabled ? '.github' : ''
            }`,
          )}
        </Alert>
      </div>

      <footer className="modal-foot">
        <Button className="js-confirm" type="submit" onClick={handleConfirmClick}>
          {translate('settings.projects.change_visibility_form.submit')}
        </Button>
        <ResetButtonLink className="js-modal-close" onClick={props.onClose}>
          {translate('cancel')}
        </ResetButtonLink>
      </footer>
    </Modal>
  );
}
