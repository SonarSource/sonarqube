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
class Api::Pagination

  DEFAULT_PER_PAGE=25
  attr_accessor :per_page, :page, :results

  def initialize(options={})
    @per_page=options[:per_page]||DEFAULT_PER_PAGE
    @page=options[:page].to_i
    @page=1 if @page<1
    @results = options[:results].to_i
  end

  def pages
    @pages ||=
        begin
          p=(@results / @per_page)
          p+=1 if @results % @per_page > 0
          p
        end
  end

  def offset
    (page-1) * per_page
  end

  # alias
  def limit
    per_page
  end

  # inclusive index
  #def to_index
  #  [results-1, (page * per_page)-1].min
  #end

  def previous?
    page>1
  end

  def next?
    page<pages
  end
end
