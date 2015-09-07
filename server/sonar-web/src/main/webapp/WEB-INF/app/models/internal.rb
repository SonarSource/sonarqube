#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

# All the Java components that are not published to public plugin API.
# Must NOT be used by plugins. Forward-compatibility is NOT guaranteed.
class Internal

  def self.issues
    component(Java::OrgSonarServerIssue::InternalRubyIssueService.java_class)
  end

  def self.text
    component(Java::OrgSonarServerText::RubyTextService.java_class)
  end

  def self.users_api
    component(Java::OrgSonarApiUser::RubyUserService.java_class)
  end

  def self.component_api
    component(Java::OrgSonarApiComponent::RubyComponentService.java_class)
  end

  # TODO to delete
  def self.permissions
    component(Java::OrgSonarServerPermission::PermissionService.java_class)
  end

  def self.permission_templates
    component(Java::OrgSonarServerPermission::PermissionTemplateService.java_class)
  end

  def self.debt
    component(Java::OrgSonarServerDebt::DebtModelService.java_class)
  end

  def self.group_membership
    component(Java::OrgSonarServerUser::GroupMembershipService.java_class)
  end

  def self.quality_profiles
    component(Java::OrgSonarServerQualityprofile::QProfiles.java_class)
  end

  def self.qprofile_service
    component(Java::OrgSonarServerQualityprofile::QProfileService.java_class)
  end

  def self.qprofile_loader
    component(Java::OrgSonarServerQualityprofile::QProfileLoader.java_class)
  end

  def self.qprofile_exporters
    component(Java::OrgSonarServerQualityprofile::QProfileExporters.java_class)
  end

  def self.quality_gates
    component(Java::OrgSonarServerQualitygate::QualityGates.java_class)
  end

  def self.rules
    component(Java::OrgSonarServerRule::RubyRuleService.java_class)
  end

  def self.durations
    component(Java::OrgSonarApiUtils::Durations.java_class)
  end

  def self.i18n
    component(Java::OrgSonarServerUi::JRubyI18n.java_class)
  end

  def self.component(component_java_class)
    Java::OrgSonarServerPlatform::Platform.component(component_java_class)
  end

end
