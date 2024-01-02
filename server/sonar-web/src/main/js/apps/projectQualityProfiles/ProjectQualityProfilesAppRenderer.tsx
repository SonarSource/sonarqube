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
import { groupBy, orderBy } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { Profile } from '../../api/quality-profiles';
import A11ySkipTarget from '../../components/a11y/A11ySkipTarget';
import Link from '../../components/common/Link';
import { Button } from '../../components/controls/buttons';
import HelpTooltip from '../../components/controls/HelpTooltip';
import Suggestions from '../../components/embed-docs-modal/Suggestions';
import EditIcon from '../../components/icons/EditIcon';
import PlusCircleIcon from '../../components/icons/PlusCircleIcon';
import { translate } from '../../helpers/l10n';
import { getRulesUrl } from '../../helpers/urls';
import { Component } from '../../types/types';
import BuiltInQualityProfileBadge from '../quality-profiles/components/BuiltInQualityProfileBadge';
import AddLanguageModal from './components/AddLanguageModal';
import SetQualityProfileModal from './components/SetQualityProfileModal';
import { ProjectProfile } from './types';

export interface ProjectQualityProfilesAppRendererProps {
  allProfiles?: Profile[];
  component: Component;
  loading: boolean;
  onAddLanguage: (key: string) => Promise<void>;
  onCloseModal: () => void;
  onOpenAddLanguageModal: () => void;
  onOpenSetProfileModal: (projectProfile: ProjectProfile) => void;
  onSetProfile: (newKey: string | undefined, oldKey: string) => Promise<void>;
  projectProfiles?: ProjectProfile[];
  showAddLanguageModal?: boolean;
  showProjectProfileInModal?: ProjectProfile;
}

export default function ProjectQualityProfilesAppRenderer(
  props: ProjectQualityProfilesAppRendererProps
) {
  const {
    allProfiles,
    component,
    loading,
    showProjectProfileInModal,
    projectProfiles,
    showAddLanguageModal,
  } = props;

  const profilesByLanguage = groupBy(allProfiles, 'language');
  const orderedProfiles = orderBy(projectProfiles, (p) => p.profile.languageName);

  return (
    <div className="page page-limited" id="project-quality-profiles">
      <Suggestions suggestions="project_quality_profiles" />
      <Helmet defer={false} title={translate('project_quality_profiles.page')} />
      <A11ySkipTarget anchor="profiles_main" />

      <header className="page-header">
        <div className="page-title display-flex-center">
          <h1>{translate('project_quality_profiles.page')} </h1>
          <HelpTooltip
            className="spacer-left"
            overlay={
              <div className="big-padded-top big-padded-bottom">
                {translate('quality_profiles.list.projects.help')}
              </div>
            }
          />
        </div>
      </header>

      <div className="boxed-group">
        <h2 className="boxed-group-header">{translate('project_quality_profile.subtitle')}</h2>

        <div className="boxed-group-inner">
          <p className="big-spacer-bottom">
            {translate('project_quality_profiles.page.description')}
          </p>

          {loading && <i className="spinner spacer-left" />}

          {!loading && orderedProfiles.length > 0 && (
            <table className="data zebra">
              <thead>
                <tr>
                  <th>{translate('language')}</th>
                  <th className="thin nowrap">{translate('project_quality_profile.current')}</th>
                  <th className="thin nowrap text-right">
                    {translate('coding_rules.filters.activation.active_rules')}
                  </th>
                  <th aria-label={translate('actions')} />
                </tr>
              </thead>
              <tbody>
                {orderedProfiles.map((projectProfile) => {
                  const { profile, selected } = projectProfile;
                  return (
                    <tr key={profile.language}>
                      <td>{profile.languageName}</td>
                      <td className="thin nowrap">
                        <span className="display-inline-flex-center">
                          {!selected && profile.isDefault ? (
                            <em>{translate('project_quality_profile.instance_default')}</em>
                          ) : (
                            <>
                              {profile.name}
                              {profile.isBuiltIn && (
                                <BuiltInQualityProfileBadge className="spacer-left" />
                              )}
                            </>
                          )}
                        </span>
                      </td>
                      <td className="nowrap text-right">
                        <Link to={getRulesUrl({ activation: 'true', qprofile: profile.key })}>
                          {profile.activeRuleCount}
                        </Link>
                      </td>
                      <td className="text-right">
                        <Button
                          onClick={() => {
                            props.onOpenSetProfileModal(projectProfile);
                          }}
                        >
                          <EditIcon className="spacer-right" />
                          {translate('project_quality_profile.change_profile')}
                        </Button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}

          <div className="big-spacer-top">
            <h2>{translate('project_quality_profile.add_language.title')}</h2>

            <p className="spacer-top big-spacer-bottom">
              {translate('project_quality_profile.add_language.description')}
            </p>

            <Button disabled={loading} onClick={props.onOpenAddLanguageModal}>
              <PlusCircleIcon className="little-spacer-right" />
              {translate('project_quality_profile.add_language.action')}
            </Button>
          </div>

          {showProjectProfileInModal && (
            <SetQualityProfileModal
              availableProfiles={profilesByLanguage[showProjectProfileInModal.profile.language]}
              component={component}
              currentProfile={showProjectProfileInModal.profile}
              onClose={props.onCloseModal}
              onSubmit={props.onSetProfile}
              usesDefault={!showProjectProfileInModal.selected}
            />
          )}

          {showAddLanguageModal && projectProfiles && (
            <AddLanguageModal
              profilesByLanguage={profilesByLanguage}
              onClose={props.onCloseModal}
              onSubmit={props.onAddLanguage}
              unavailableLanguages={projectProfiles.map((p) => p.profile.language)}
            />
          )}
        </div>
      </div>
    </div>
  );
}
