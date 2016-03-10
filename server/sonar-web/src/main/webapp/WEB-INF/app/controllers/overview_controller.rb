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
class OverviewController < ApplicationController
  before_filter :init_resource_for_user_role

  SECTION=Navigation::SECTION_RESOURCE

  def index
    if Project.root_qualifiers.include?('VW') && (@resource.qualifier == 'VW' || @resource.qualifier == 'SVW')
      return redirect_to(url_for({:controller => 'governance'}) + '?id=' + url_encode(params[:id]))
    end
  end

end
