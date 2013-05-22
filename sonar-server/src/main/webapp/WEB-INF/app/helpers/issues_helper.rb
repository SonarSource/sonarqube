#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
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
module IssuesHelper

  def column_html(filter, column_label, column_tooltip, sort)
    filter_sort = filter.criteria[:sort]
    filter_asc = filter.criteria[:asc] == 'true' ? true : false
    html = h(column_label)
    unless filter.issues_result.maxResultsReached()
      html = link_to_function(h(column_label), "refreshList('#{escape_javascript sort}',#{!filter_asc}, #{filter.criteria[:page]||1})", :title => h(column_tooltip))
      if sort == filter_sort
        html << (filter_asc ? image_tag("asc12.png") : image_tag("desc12.png"))
      end
    end
    html
  end

end
