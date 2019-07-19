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
import { Button, EditButton } from 'sonar-ui-common/components/controls/buttons';
import { translate } from 'sonar-ui-common/helpers/l10n';
import CodeSnippet from '../../../../components/common/CodeSnippet';
import Step from '../../components/Step';
import EditTokenModal from './EditTokenModal';

export interface YourTokenProps {
  component: T.Component;
  currentUser: T.LoggedInUser;
  hasStepAfter?: (hasStepAfter: boolean) => void;
  onContinue: VoidFunction;
  onOpen: VoidFunction;
  open: boolean;
  organization?: string;
  setToken: (token: string) => void;
  stepNumber: number;
  token?: string;
}

export default function EncryptYourTokenStep({
  component,
  currentUser,
  onContinue,
  onOpen,
  open,
  setToken,
  stepNumber,
  token
}: YourTokenProps) {
  const [showModal, toggleModal] = React.useState<boolean>(false);

  const close = () => toggleModal(!showModal);

  const save = (token: string) => {
    setToken(token);
    close();
  };

  const command = `travis encrypt ${token}`;
  const renderCommand = () => (
    <div className="spacer-bottom">
      travis encrypt {token}
      <EditButton className="edit-token spacer-left" onClick={() => toggleModal(true)} />
    </div>
  );

  const renderForm = () => (
    <div className="boxed-group-inner">
      {showModal && (
        <EditTokenModal
          component={component}
          currentUser={currentUser}
          onClose={close}
          onSave={save}
        />
      )}

      <div className="display-flex-space-between">
        <div className="display-inline-block">
          <a
            href="https://docs.travis-ci.com/user/encryption-keys/#usage"
            rel="noopener noreferrer"
            target="_blank">
            {translate('onboarding.analysis.with.travis.encrypt.docs.link.label')}
          </a>
          <br />
          <CodeSnippet isOneLine={true} render={renderCommand} snippet={command} wrap={true} />
        </div>
      </div>

      <div className="big-spacer-top">
        <Button className="js-continue" onClick={onContinue}>
          {translate('continue')}
        </Button>
      </div>
    </div>
  );

  const renderResult = () => null;

  return (
    <Step
      finished={true}
      onOpen={onOpen}
      open={open}
      renderForm={renderForm}
      renderResult={renderResult}
      stepNumber={stepNumber}
      stepTitle={translate('onboarding.analysis.with.travis.encrypt.title')}
    />
  );
}
