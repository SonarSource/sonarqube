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
class FavouritesController < ApplicationController

  before_filter :login_required

  def toggle
    favourite_id=params[:id]
    if current_user.favourite?(favourite_id)
      current_user.delete_favourite(favourite_id)
      @style='notfav'
      @tooltip='Click to add to favourites'
    else
      current_user.add_favourite(favourite_id)
      @style='fav'
      @tooltip='Click to delete from favourites'
    end

    star_id=params[:elt]
    render :update do |page|
      page.element.removeClassName(star_id, 'notfav')
      page.element.removeClassName(star_id, 'fav')
      page.element.addClassName(star_id, @style)
      page.element.writeAttribute(star_id, 'alt', @tooltip)
      page.element.writeAttribute(star_id, 'title', @tooltip)
    end 
  end

end
