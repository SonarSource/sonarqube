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
module MeasuresHelper

  def list_column_title(filter, column, url_params)
    if column.sort?
      html = link_to(h(column.display_name), url_params.merge({:controller => 'measures', :action => 'search', :asc => (!filter.sort_asc?).to_s, :sort => column.key}))
    else
      html=h(column.display_name)
    end
    #if column.variation
    #  html="<img src='#{ApplicationController.root_context}/images/trend-up.png'></img> #{html}"
    #end

    if filter.sort_key==column.key
      html << (filter.sort_asc? ? image_tag("asc12.png") : image_tag("desc12.png"))
    end
    "<th class='#{column.align}'>#{html}</th>"
  end

end
