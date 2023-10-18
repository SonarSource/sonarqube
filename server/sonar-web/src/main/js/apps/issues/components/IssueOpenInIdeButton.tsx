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

import {
  ButtonSecondary,
  DropdownMenu,
  DropdownToggler,
  ItemButton,
  PopupPlacement,
  PopupZLevel,
} from 'design-system';
import * as React from 'react';
import { addGlobalErrorMessage, addGlobalSuccessMessage } from '../../../helpers/globalMessages';
import { translate } from '../../../helpers/l10n';
import { openIssue as openSonarLintIssue, probeSonarLintServers } from '../../../helpers/sonarlint';
import { Ide } from '../../../types/sonarlint';

export interface Props {
  issueKey: string;
  projectKey: string;
}

interface State {
  ides: Ide[];
  mounted: boolean;
}

const showError = () => addGlobalErrorMessage(translate('issues.open_in_ide.failure'));

const showSuccess = () => addGlobalSuccessMessage(translate('issues.open_in_ide.success'));

export function IssueOpenInIdeButton({ issueKey, projectKey }: Readonly<Props>) {
  const [state, setState] = React.useState<State>({ ides: [], mounted: false });

  React.useEffect(() => {
    setState({ ...state, mounted: true });

    return () => {
      setState({ ...state, mounted: false });
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const cleanState = () => {
    if (state.mounted) {
      setState({ ...state, ides: [] });
    }
  };

  const openIssue = (ide: Ide) => {
    setState({ ...state, ides: [] });

    return openSonarLintIssue(ide.port, projectKey, issueKey)
      .then(showSuccess)
      .catch(showError)
      .finally(cleanState);
  };

  const onClick = async () => {
    setState({ ...state, ides: [] });

    const ides = (await probeSonarLintServers()) ?? [];

    if (ides.length === 0) {
      showError();
    } else if (ides.length === 1) {
      openIssue(ides[0]);
    } else if (state.mounted) {
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
        <ButtonSecondary className="sw-whitespace-nowrap" onClick={onClick}>
          {translate('open_in_ide')}
        </ButtonSecondary>
      </DropdownToggler>
    </div>
  );
}
