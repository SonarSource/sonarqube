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
class AllProjectsController < ApplicationController

  SECTION=Navigation::SECTION_HOME
  
  def index
    require_parameters :qualifier
    @qualifier = params[:qualifier]
    bad_request("The 'qualifier' parameter is not valid. It must reference a root type.") unless Project.root_qualifiers.include?(@qualifier)
    
    
    @filter = MeasureFilter.new
    @filter.criteria = params.merge({'qualifiers' => [@qualifier], :cols => ['name', 'description', 'links'], :sort => 'name'})
    @filter.enable_default_display
    @filter.execute(self, :user => current_user)
  end

end