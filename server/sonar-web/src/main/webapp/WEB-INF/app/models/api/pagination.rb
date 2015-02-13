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
class Api::Pagination

  DEFAULT_PER_PAGE=25

  # entries per page
  attr_accessor :per_page

  # index of selected page. Start with 1.
  attr_accessor :page

  # total count of entries, greater or equal than 0
  attr_accessor :count

  # optional reference to the entries of the selected page
  attr_accessor :page_entries

  def initialize(options={})
    @per_page = (options[:per_page]||DEFAULT_PER_PAGE).to_i
    @page=options[:page].to_i
    @page=1 if @page<1
    @count = options[:count].to_i
  end

  def pages
    if per_page <= 0
      return 0
    end
    p=(count / per_page)
    p+=1 if count % per_page>0
    p
  end

  def offset
    (page-1) * per_page
  end

  # alias
  def limit
    per_page
  end

  def previous?
    page>1
  end

  def next?
    page<pages
  end

  def empty?
    count==0
  end
end
