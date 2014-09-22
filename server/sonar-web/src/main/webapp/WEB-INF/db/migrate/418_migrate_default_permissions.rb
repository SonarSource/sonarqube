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
#

class MigrateDefaultPermissions < ActiveRecord::Migration

  ROOT_QUALIFIERS = {:TRK => 'Projects', :VW => 'Views', :SVW => 'Subviews', :DEV => 'Developers'}

  class Group < ActiveRecord::Base
  end

  class GroupRole < ActiveRecord::Base
  end

  class User < ActiveRecord::Base
  end

  class UserRole < ActiveRecord::Base
  end

  class Property < ActiveRecord::Base
    set_table_name 'properties'
  end

  class PermissionTemplate < ActiveRecord::Base
  end

  class PermissionTemplateUser < ActiveRecord::Base
    set_table_name 'perm_templates_users'
  end

  class PermissionTemplateGroup < ActiveRecord::Base
    set_table_name 'perm_templates_groups'
  end

  class LoadedTemplate < ActiveRecord::Base
  end

  def self.up
    Group.reset_column_information
    GroupRole.reset_column_information
    User.reset_column_information
    UserRole.reset_column_information
    Property.reset_column_information
    PermissionTemplate.reset_column_information
    PermissionTemplateUser.reset_column_information
    PermissionTemplateGroup.reset_column_information

    @is_fresh_install = LoadedTemplate.count == 0

    migrate_existing_default_permissions

  end

  private

  def self.migrate_existing_default_permissions

    ROOT_QUALIFIERS.keys.each do |qualifier|
      existing_properties = {}
      existing_properties["sonar.role.admin.#{qualifier}.defaultGroups"] = Property.find_by_prop_key("sonar.role.admin.#{qualifier}.defaultGroups")
      existing_properties["sonar.role.user.#{qualifier}.defaultGroups"] = Property.find_by_prop_key("sonar.role.user.#{qualifier}.defaultGroups")
      existing_properties["sonar.role.codeviewer.#{qualifier}.defaultGroups"] = Property.find_by_prop_key("sonar.role.codeviewer.#{qualifier}.defaultGroups")
      existing_properties["sonar.role.admin.#{qualifier}.defaultUsers"] = Property.find_by_prop_key("sonar.role.admin.#{qualifier}.defaultUsers")
      existing_properties["sonar.role.user.#{qualifier}.defaultUsers"] = Property.find_by_prop_key("sonar.role.user.#{qualifier}.defaultUsers")
      existing_properties["sonar.role.codeviewer.#{qualifier}.defaultUsers"] = Property.find_by_prop_key("sonar.role.codeviewer.#{qualifier}.defaultUsers")

      configured_values = existing_properties.values.reject {|prop| prop.nil? || prop.text_value.nil?}

      # Existing properties are migrated only when upgrading an existing SonarQube instance
      # Subviews permissions are not migrated since they are not used
      if !@is_fresh_install && qualifier != :SVW && !configured_values.empty?

        template_id = migrate_existing_permissions(qualifier, configured_values)

        # If some of the default properties are missing in DB then fallback to SonarQube defaults
        # This can happen with DevCockpit as the default permissions are not persistent settings
        if configured_values.length < 6
          existing_properties.each_pair do |key, value|
            if value.nil?
              add_default_setting_for_key(key, template_id)
            end
          end
        end
      end

      delete_existing_default_permissions(configured_values)

    end

  end

  def self.migrate_existing_permissions(qualifier, properties)

    qualifier_template = PermissionTemplate.create(
      :name => "Default template for #{ROOT_QUALIFIERS[qualifier]}",
      :kee => "default_template_for_#{ROOT_QUALIFIERS[qualifier].downcase}",
      :description => "This template has been automatically created using the previously configured default permissions for #{ROOT_QUALIFIERS[qualifier]}")

    properties.each do |property|
      key_fields = property.prop_key.split('.')
      value_fields = property.text_value.split(',')
      role = key_fields[2]
      if 'defaultGroups'.eql?(key_fields[4])
        value_fields.each do |group_name|
          if 'Anyone'.eql?(group_name) || !Group.find_by_name(group_name).nil?
            group_id = 'Anyone'.eql?(group_name) ? nil : Group.find_by_name(group_name).id
            PermissionTemplateGroup.create(:group_id => group_id, :permission_reference => role, :template_id => qualifier_template.id)
          end
        end
      else
        value_fields.each do |user_login|
          user = User.find_by_login(user_login)
          unless user.nil?
            PermissionTemplateUser.create(:user_id => user.id, :permission_reference => role, :template_id => qualifier_template.id)
          end
        end
      end
    end

    Property.create(:prop_key => "sonar.permission.template.#{qualifier}.default", :text_value => qualifier_template.kee)

    qualifier_template.id
  end

  def self.delete_existing_default_permissions(properties)
    properties.each do |property|
      Property.delete(property.id) unless property.nil?
    end
  end

  def self.add_default_setting_for_key(key, template_id)

    if key.include?('admin')
      if(key.include?('defaultGroups'))
        # admin -> sonar-administrators
        group_id = Group.find_by_name('sonar-administrators').id
        PermissionTemplateGroup.create(:group_id => group_id, :permission_reference => 'admin', :template_id => template_id)
      end
    elsif key.include?('user')
      if(key.include?('defaultGroups'))
        # user -> sonar-users, Anyone
        group_id = Group.find_by_name('sonar-users').id
        PermissionTemplateGroup.create(:group_id => group_id, :permission_reference => 'user', :template_id => template_id)
        PermissionTemplateGroup.create(:group_id => nil, :permission_reference => 'user', :template_id => template_id)
      end
    elsif key.include?('codeviewer')
      if(key.include?('defaultGroups'))
        # codeviewer -> sonar-users, Anyone
        group_id = Group.find_by_name('sonar-users').id
        PermissionTemplateGroup.create(:group_id => group_id, :permission_reference => 'codeviewer', :template_id => template_id)
        PermissionTemplateGroup.create(:group_id => nil, :permission_reference => 'codeviewer', :template_id => template_id)
      end
    end
  end

end
