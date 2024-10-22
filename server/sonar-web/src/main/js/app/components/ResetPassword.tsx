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

import { Helmet } from 'react-helmet-async';
import {
  FlagMessage,
  LargeCenteredLayout,
  PageContentFontWrapper,
  SubHeading,
  Title,
} from '~design-system';
import ResetPasswordForm from '../../components/common/ResetPasswordForm';
import { whenLoggedIn } from '../../components/hoc/whenLoggedIn';
import { translate } from '../../helpers/l10n';
import { getBaseUrl } from '../../helpers/system';
import { LoggedInUser } from '../../types/users';

export interface ResetPasswordProps {
  currentUser: LoggedInUser;
}

export function ResetPassword({ currentUser }: Readonly<ResetPasswordProps>) {
  return (
    <LargeCenteredLayout className="sw-h-screen sw-pt-10">
      <PageContentFontWrapper className="sw-typo-default">
        <Helmet defer={false} title={translate('my_account.reset_password.page')} />
        <div className="sw-flex sw-justify-center">
          <div>
            <Title>{translate('my_account.reset_password')}</Title>
            <FlagMessage variant="warning" className="sw-mb-4">
              {translate('my_account.reset_password.explain')}
            </FlagMessage>
            <SubHeading>{translate('my_profile.password.title')}</SubHeading>
            <ResetPasswordForm
              user={currentUser}
              onPasswordChange={() => {
                // Force a refresh for the backend to handle additional redirects.
                window.location.href = `${getBaseUrl()}/`;
              }}
            />
          </div>
        </div>
      </PageContentFontWrapper>
    </LargeCenteredLayout>
  );
}

export default whenLoggedIn(ResetPassword);
