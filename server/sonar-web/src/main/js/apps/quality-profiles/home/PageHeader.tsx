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
import { ButtonPrimary, ButtonSecondary, FlagMessage, Link } from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { useLocation, useRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Actions } from '../../../api/quality-profiles';
import { useDocUrl } from '../../../helpers/docs';
import { translate } from '../../../helpers/l10n';
import { Profile } from '../types';
import { getProfilePath } from '../utils';
import CreateProfileForm from './CreateProfileForm';
import RestoreProfileForm from './RestoreProfileForm';

interface Props {
  actions: Actions;
  languages: Array<{ key: string; name: string }>;
  profiles: Profile[];
  updateProfiles: () => Promise<void>;
}

export default function PageHeader(props: Readonly<Props>) {
  const { actions, languages, profiles } = props;
  const intl = useIntl();
  const location = useLocation();
  const router = useRouter();
  const docUrl = useDocUrl();

  const [modal, setModal] = React.useState<'' | 'createProfile' | 'restoreProfile'>('');

  const handleCreate = (profile: Profile) => {
    props.updateProfiles().then(
      () => {
        router.push(getProfilePath(profile.name, profile.language));
      },
      () => {},
    );
  };

  const closeModal = () => setModal('');

  return (
    <header className="sw-grid sw-grid-cols-3 sw-gap-12">
      <div className="sw-col-span-2">
        <h1 className="sw-heading-lg sw-mb-4">{translate('quality_profiles.page')}</h1>
        <div className="sw-body-sm">
          {intl.formatMessage({ id: 'quality_profiles.intro' })}

          <Link className="sw-ml-2" to={docUrl('/instance-administration/quality-profiles/')}>
            {intl.formatMessage({ id: 'learn_more' })}
          </Link>
        </div>
      </div>
      {actions.create && (
        <div className="sw-flex sw-flex-col sw-items-end">
          <div>
            <ButtonPrimary
              disabled={languages.length === 0}
              id="quality-profiles-create"
              onClick={() => setModal('createProfile')}
            >
              {intl.formatMessage({ id: 'create' })}
            </ButtonPrimary>
            <ButtonSecondary
              className="sw-ml-2"
              id="quality-profiles-restore"
              onClick={() => setModal('restoreProfile')}
            >
              {intl.formatMessage({ id: 'restore' })}
            </ButtonSecondary>
          </div>
          {languages.length === 0 && (
            <FlagMessage className="sw-mt-2" variant="warning">
              {intl.formatMessage({ id: 'quality_profiles.no_languages_available' })}
            </FlagMessage>
          )}
        </div>
      )}

      {modal === 'restoreProfile' && (
        <RestoreProfileForm onClose={closeModal} onRestore={props.updateProfiles} />
      )}

      {modal === 'createProfile' && (
        <CreateProfileForm
          languages={languages}
          location={location}
          onClose={closeModal}
          onCreate={handleCreate}
          profiles={profiles}
        />
      )}
    </header>
  );
}
