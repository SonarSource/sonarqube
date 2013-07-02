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

  def column_html(issue_query, issues_result, column_label, column_tooltip, sort)
    filter_sort = issue_query.sort
    filter_asc = issue_query.asc
    html = h(column_label)
    unless issues_result.maxResultsReached()
      html = link_to_function(h(column_label), "refreshList('#{escape_javascript sort}',#{!filter_asc}, #{issue_query.pageIndex||1})", :title => h(column_tooltip))
      if sort == filter_sort
        html << (filter_asc ? image_tag("asc12.png") : image_tag("desc12.png"))
      end
    end
    html
  end

  def issue_filter_star(filter, is_favourite)
    if is_favourite
      style='fav'
      title=message('click_to_remove_from_favourites')
    else
      style='notfav'
      title=message('click_to_add_to_favourites')
    end

    "<a href='#' id='star-#{filter.name.parameterize}' class='issue-filter-star #{style}' filter-id='#{filter.id.to_s}' title='#{title}'></a>"
  end

  def can_be_reassigned_by(user, filter)
    user.has_role?(:admin) && filter.shared
  end

  def severitiy_select_option_tags
    options = ''
    Severity::KEYS.each do |severity|
      selected = (severity == Severity::MAJOR ? 'selected' : '')
      options += "<option #{selected} value='#{ severity }' class='sev_#{ severity } '>#{ message('severity.'+ severity) }</option>"
    end
    options
  end

end
