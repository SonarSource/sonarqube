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

import {
  ButtonSecondary,
  DropdownMenu,
  DropdownToggler,
  ItemButton,
  PopupPlacement,
  PopupZLevel,
  addGlobalErrorMessage,
  addGlobalSuccessMessage,
} from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { DocLink } from '../../../helpers/doc-links';
import { translate } from '../../../helpers/l10n';
import {
  generateSonarLintUserToken,
  openIssue as openSonarLintIssue,
  probeSonarLintServers,
} from '../../../helpers/sonarlint';
import { Ide } from '../../../types/sonarlint';
import { UserBase } from '../../../types/users';

export interface Props {
  branchName?: string;
  issueKey: string;
  login: UserBase['login'];
  projectKey: string;
  pullRequestID?: string;
}

interface State {
  disabled?: boolean;
  ides: Ide[];
}

const showError = () =>
  addGlobalErrorMessage(
    <FormattedMessage
      id="issues.open_in_ide.failure"
      values={{
        link: (
          <DocumentationLink to={DocLink.SonarLintConnectedMode}>
            {translate('sonarlint-connected-mode-doc')}
          </DocumentationLink>
        ),
      }}
    />,
  );

const showSuccess = () => addGlobalSuccessMessage(translate('issues.open_in_ide.success'));

const DELAY_AFTER_TOKEN_CREATION = 3000;

export function IssueOpenInIdeButton({
  branchName,
  issueKey,
  login,
  projectKey,
  pullRequestID,
}: Readonly<Props>) {
  const [state, setState] = React.useState<State>({ disabled: false, ides: [] });

  const cleanState = () => {
    setState({ ...state, ides: [] });
  };

  const openIssue = async (ide: Ide) => {
    setState({ ...state, disabled: true, ides: [] }); // close the dropdown, disable the button

    let token: { name?: string; token?: string } = {};

    try {
      if (ide.needsToken) {
        token = await generateSonarLintUserToken({ ideName: ide.ideName, login });
      }

      await openSonarLintIssue({
        branchName,
        calledPort: ide.port,
        issueKey,
        projectKey,
        pullRequestID,
        tokenName: token.name,
        tokenValue: token.token,
      });

      showSuccess();
    } catch (e) {
      showError();
    }

    setTimeout(
      () => {
        setState({ ...state, disabled: false });
      },
      ide.needsToken ? DELAY_AFTER_TOKEN_CREATION : 0,
    );
  };

  const onClick = async () => {
    setState({ ...state, ides: [] });

    const ides = (await probeSonarLintServers()) ?? [];

    if (ides.length === 0) {
      showError();
    } else if (ides.length === 1) {
      openIssue(ides[0]);
    } else {
      setState({ ...state, ides });
    }
  };

  return (
    <div>
      <DropdownToggler
        allowResizing
        onRequestClose={cleanState}
        open={state.ides.length > 1}
        overlay={
          <DropdownMenu size="auto">
            {state.ides.map((ide) => {
              const { ideName, description } = ide;

              const label = ideName + (description ? ` - ${description}` : '');

              return (
                <ItemButton
                  key={ide.port}
                  onClick={() => {
                    openIssue(ide);
                  }}
                >
                  {label}
                </ItemButton>
              );
            })}
          </DropdownMenu>
        }
        placement={PopupPlacement.BottomLeft}
        zLevel={PopupZLevel.Global}
      >
        <ButtonSecondary
          className="sw-whitespace-nowrap"
          disabled={state.disabled}
          onClick={onClick}
        >
          {translate('open_in_ide')}
        </ButtonSecondary>
      </DropdownToggler>
    </div>
  );
}
