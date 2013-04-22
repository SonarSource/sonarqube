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
class ColumnsController < ApplicationController
  before_filter :admin_required

  before_filter :init
  
  def add
    column = @components_configuration.find_available_column(@column_id)
    @components_configuration.add_column(column)
    redirect_to :controller => 'components', :action => 'index', :configuring => 'true', :id => params[:rid]
  end
  
  def delete
    column = @components_configuration.find_selected_column(@column_id)
    @components_configuration.remove_column(column)
    if column.sort_default?
      @components_configuration.set_column_sort_default(Sonar::ColumnsView::TYPE_PROJECT)
    end
    redirect_to :controller => 'components', :action => 'index', :configuring => 'true', :id => params[:rid]
  end

  def toggle_treemap
    @components_configuration.toggle_treemap_enabled
    redirect_to :controller => 'components', :action => 'index', :configuring => 'true', :id => params[:rid]
  end
  
  def left
    column = @components_configuration.find_selected_column(@column_id)
    @components_configuration.move_column(column, "left")
    redirect_to :controller => 'components', :action => 'index', :configuring => 'true', :id => params[:rid]
  end
    
  def right
    column = @components_configuration.find_selected_column(@column_id)
    @components_configuration.move_column(column, "right")
    redirect_to :controller => 'components', :action => 'index', :configuring => 'true', :id => params[:rid]
  end  
  
  def default_sorting
    @components_configuration.set_column_sort_default(@column_id)
    redirect_to :controller => 'components', :action => 'index', :configuring => 'true', :id => params[:rid]
  end  
  

  private
  
  def init
    @components_configuration = Sonar::ComponentsConfiguration.new
    @column_id = params[:id]
  end


end
