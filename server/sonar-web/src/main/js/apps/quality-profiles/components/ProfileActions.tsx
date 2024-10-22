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
  ButtonIcon,
  DropdownMenu,
  IconMoreVertical,
  Tooltip,
  TooltipSide,
} from '@sonarsource/echoes-react';
import { some } from 'lodash';
import * as React from 'react';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Router } from '~sonar-aligned/types/router';
import {
  changeProfileParent,
  copyProfile,
  createQualityProfile,
  deleteProfile,
  renameProfile,
  setDefaultProfile,
} from '../../../api/quality-profiles';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { getRulesUrl } from '../../../helpers/urls';
import { PROFILE_PATH } from '../constants';
import { Profile, ProfileActionModals } from '../types';
import { getProfileComparePath, getProfilePath } from '../utils';
import DeleteProfileForm from './DeleteProfileForm';
import ProfileModalForm from './ProfileModalForm';

interface Props {
  isComparable: boolean;
  profile: Profile;
  router: Router;
  updateProfiles: () => Promise<void>;
}

interface State {
  loading: boolean;
  openModal?: ProfileActionModals;
}

class ProfileActions extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    loading: false,
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleCloseModal = () => {
    this.setState({ openModal: undefined });
  };

  handleCopyClick = () => {
    this.setState({ openModal: ProfileActionModals.Copy });
  };

  handleExtendClick = () => {
    this.setState({ openModal: ProfileActionModals.Extend });
  };

  handleRenameClick = () => {
    this.setState({ openModal: ProfileActionModals.Rename });
  };

  handleDeleteClick = () => {
    this.setState({ openModal: ProfileActionModals.Delete });
  };

  handleProfileCopy = async (name: string) => {
    this.setState({ loading: true });

    try {
      await copyProfile(this.props.profile.key, name);
      this.profileActionPerformed(name);
    } catch {
      this.profileActionError();
    }
  };

  handleProfileExtend = async (name: string) => {
    const { profile: parentProfile } = this.props;

    const data = {
      language: parentProfile.language,
      name,
    };

    this.setState({ loading: true });

    try {
      const { profile: newProfile } = await createQualityProfile(data);
      await changeProfileParent(newProfile, parentProfile);
      this.profileActionPerformed(name);
    } catch {
      this.profileActionError();
    }
  };

  handleProfileRename = async (name: string) => {
    this.setState({ loading: true });

    try {
      await renameProfile(this.props.profile.key, name);
      this.profileActionPerformed(name);
    } catch {
      this.profileActionError();
    }
  };

  handleProfileDelete = async () => {
    this.setState({ loading: true });

    try {
      await deleteProfile(this.props.profile);

      if (this.mounted) {
        this.setState({ loading: false, openModal: undefined });
        this.props.router.replace(PROFILE_PATH);
        this.props.updateProfiles();
      }
    } catch {
      this.profileActionError();
    }
  };

  handleSetDefaultClick = () => {
    const { profile } = this.props;
    if (profile.activeRuleCount > 0) {
      setDefaultProfile(profile).then(this.props.updateProfiles, () => {
        /* noop */
      });
    }
  };

  profileActionPerformed = (name: string) => {
    const { profile, router } = this.props;
    if (this.mounted) {
      this.setState({ loading: false, openModal: undefined });
      this.props.updateProfiles().then(
        () => {
          router.push(getProfilePath(name, profile.language));
        },
        () => {
          /* noop */
        },
      );
    }
  };

  profileActionError = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  getQualityProfileBackupUrl = ({ language, name: qualityProfile }: Profile) => {
    const queryParams = Object.entries({ language, qualityProfile })
      .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
      .join('&');
    return `/api/qualityprofiles/backup?${queryParams}`;
  };

  render() {
    const { profile, isComparable } = this.props;
    const { loading, openModal } = this.state;
    const { actions = {} } = profile;

    const backupUrl = `${getBaseUrl()}${this.getQualityProfileBackupUrl(profile)}`;

    const activateMoreUrl = getRulesUrl({
      qprofile: profile.key,
      activation: 'false',
    });

    const hasNoActiveRules = profile.activeRuleCount === 0;
    const hasAnyAction = some([...Object.values(actions), !profile.isBuiltIn, isComparable]);

    if (!hasAnyAction) {
      return null;
    }

    return (
      <>
        <DropdownMenu.Root
          id={`quality-profile-actions-${profile.key}`}
          className="it__quality-profiles__actions-dropdown"
          items={
            <>
              {actions.edit && (
                <DropdownMenu.ItemLink
                  className="it__quality-profiles__activate-more-rules"
                  to={activateMoreUrl}
                >
                  {translate('quality_profiles.activate_more_rules')}
                </DropdownMenu.ItemLink>
              )}

              {!profile.isBuiltIn && (
                <DropdownMenu.ItemLinkDownload
                  download={`${profile.key}.xml`}
                  to={backupUrl}
                  className="it__quality-profiles__backup"
                >
                  {translate('backup_verb')}
                </DropdownMenu.ItemLinkDownload>
              )}

              {isComparable && (
                <DropdownMenu.ItemLink
                  className="it__quality-profiles__compare"
                  to={getProfileComparePath(profile.name, profile.language)}
                >
                  {translate('compare')}
                </DropdownMenu.ItemLink>
              )}

              {actions.copy && (
                <>
                  <Tooltip
                    content={translateWithParameters('quality_profiles.extend_help', profile.name)}
                    side={TooltipSide.Left}
                  >
                    <DropdownMenu.ItemButton
                      className="it__quality-profiles__extend"
                      onClick={this.handleExtendClick}
                    >
                      {translate('extend')}
                    </DropdownMenu.ItemButton>
                  </Tooltip>

                  <Tooltip
                    content={translateWithParameters('quality_profiles.copy_help', profile.name)}
                    side={TooltipSide.Left}
                  >
                    <DropdownMenu.ItemButton
                      className="it__quality-profiles__copy"
                      onClick={this.handleCopyClick}
                    >
                      {translate('copy')}
                    </DropdownMenu.ItemButton>
                  </Tooltip>
                </>
              )}

              {actions.edit && (
                <DropdownMenu.ItemButton
                  className="it__quality-profiles__rename"
                  onClick={this.handleRenameClick}
                >
                  {translate('rename')}
                </DropdownMenu.ItemButton>
              )}

              {actions.setAsDefault &&
                (hasNoActiveRules ? (
                  <Tooltip
                    content={translate('quality_profiles.cannot_set_default_no_rules')}
                    side={TooltipSide.Left}
                  >
                    <DropdownMenu.ItemButton
                      className="it__quality-profiles__set-as-default"
                      onClick={this.handleSetDefaultClick}
                      isDisabled
                    >
                      {translate('set_as_default')}
                    </DropdownMenu.ItemButton>
                  </Tooltip>
                ) : (
                  <DropdownMenu.ItemButton
                    className="it__quality-profiles__set-as-default"
                    onClick={this.handleSetDefaultClick}
                  >
                    {translate('set_as_default')}
                  </DropdownMenu.ItemButton>
                ))}

              {actions.delete && (
                <>
                  <DropdownMenu.Separator />
                  <DropdownMenu.ItemButtonDestructive
                    className="it__quality-profiles__delete"
                    onClick={this.handleDeleteClick}
                  >
                    {translate('delete')}
                  </DropdownMenu.ItemButtonDestructive>
                </>
              )}
            </>
          }
        >
          <ButtonIcon
            Icon={IconMoreVertical}
            className="it__quality-profiles__actions-dropdown-toggle"
            ariaLabel={translateWithParameters(
              'quality_profiles.actions',
              profile.name,
              profile.languageName,
            )}
          />
        </DropdownMenu.Root>

        {openModal === ProfileActionModals.Copy && (
          <ProfileModalForm
            action={openModal}
            loading={loading}
            onClose={this.handleCloseModal}
            onSubmit={this.handleProfileCopy}
            profile={profile}
          />
        )}

        {openModal === ProfileActionModals.Extend && (
          <ProfileModalForm
            action={openModal}
            loading={loading}
            onClose={this.handleCloseModal}
            onSubmit={this.handleProfileExtend}
            profile={profile}
          />
        )}

        {openModal === ProfileActionModals.Rename && (
          <ProfileModalForm
            action={openModal}
            loading={loading}
            onClose={this.handleCloseModal}
            onSubmit={this.handleProfileRename}
            profile={profile}
          />
        )}

        {openModal === ProfileActionModals.Delete && (
          <DeleteProfileForm
            loading={loading}
            onClose={this.handleCloseModal}
            onDelete={this.handleProfileDelete}
            profile={profile}
          />
        )}
      </>
    );
  }
}

export default withRouter(ProfileActions);
