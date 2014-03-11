#
# Sonar, entreprise quality control tool.
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
class Mock::ExampleController < Api::ApiController

  # GET /mock/example/index
  def index
    # declare JSON response with ruby hash or array
    render :json => JSON({'foo' => 'bar', 'an_integer' => 4})
  end

  # GET /mock/example/search
  def search
    # declare JSON response with string
    render :json => <<RESPONSE
      {
        "foo": "bar"
      }
RESPONSE
  end

end
