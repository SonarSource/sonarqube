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
module IssuesHelper

  def issue_filter_star(filter, is_favourite)
    if is_favourite
      style='icon-favorite'
      title=message('click_to_remove_from_favorites')
    else
      style='icon-not-favorite'
      title=message('click_to_add_to_favorites')
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
