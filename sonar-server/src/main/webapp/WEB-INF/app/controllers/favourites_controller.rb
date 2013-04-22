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
class FavouritesController < ApplicationController

  before_filter :login_required

  def toggle
    favourite_id=params[:id]

    if current_user.favourite?(favourite_id)
      current_user.delete_favourite(favourite_id)
      css='notfav'
      title=message('click_to_add_to_favourites')
    else
      current_user.add_favourite(favourite_id)
      css='fav'
      title=message('click_to_remove_from_favourites')
    end

    render :json => {:css => css, :title => title}, :status => 200
  end

end
