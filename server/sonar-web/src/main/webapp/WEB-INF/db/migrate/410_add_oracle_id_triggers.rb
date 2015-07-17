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

#
# Sonar 3.7
# SONAR-4178
class AddOracleIdTriggers < ActiveRecord::Migration

  def self.up
    if dialect()=='oracle'
      create_id_trigger('action_plans')
      create_id_trigger('active_dashboards')
      create_id_trigger('active_rule_changes')
      create_id_trigger('active_rule_notes')
      create_id_trigger('active_rule_param_changes')
      create_id_trigger('active_rule_parameters')
      create_id_trigger('active_rules')
      create_id_trigger('authors')
      create_id_trigger('characteristic_properties')
      create_id_trigger('characteristics')
      create_id_trigger('dashboards')
      create_id_trigger('dependencies')
      create_id_trigger('duplications_index')
      create_id_trigger('events')
      create_id_trigger('graphs')
      create_id_trigger('group_roles')
      create_id_trigger('groups')
      create_id_trigger('issue_changes')
      create_id_trigger('issues')
      create_id_trigger('loaded_templates')
      create_id_trigger('manual_measures')
      create_id_trigger('measure_data')
      create_id_trigger('measure_filter_favourites')
      create_id_trigger('measure_filters')
      create_id_trigger('metrics')
      create_id_trigger('notifications')
      create_id_trigger('project_links')
      create_id_trigger('project_measures')
      create_id_trigger('projects')
      create_id_trigger('properties')
      create_id_trigger('quality_models')
      create_id_trigger('resource_index')
      create_id_trigger('rule_notes')
      create_id_trigger('rules')
      create_id_trigger('rules_parameters')
      create_id_trigger('rules_profiles')
      create_id_trigger('semaphores')
      create_id_trigger('snapshot_data')
      create_id_trigger('snapshot_sources')
      create_id_trigger('snapshots')
      create_id_trigger('user_roles')
      create_id_trigger('users')
      create_id_trigger('widget_properties')
      create_id_trigger('widgets')
    end
  end

end

