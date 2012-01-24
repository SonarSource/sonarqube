#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
class RulesController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION
  
  def show
    key=params[:id]
    if key.to_i==0
      parts=key.split(':')
      @rule=Rule.find(:first, :conditions => ['plugin_name=? and plugin_rule_key=?', parts[0], parts[1]])
    else
      @rule=Rule.find(key)
    end
    @page_title=@rule.name
    
    if params[:resource_id]
      resource = Project.find(params[:resource_id])
      @profile = resource.root_project.profile || Profile.default_profile
      @active_rule = @profile.active_by_rule_id(@rule.id)
    end
  end

end
