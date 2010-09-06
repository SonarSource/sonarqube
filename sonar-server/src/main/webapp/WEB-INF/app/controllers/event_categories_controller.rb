#
# Sonar, entreprise quality control tool.
# Copyright (C) 2009 SonarSource SA
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
class EventCategoriesController < ApplicationController

  before_filter :admin_required

  verify :method => :post, :only => [  :save, :delete ], :redirect_to => { :action => :index }

  SECTION=Navigation::SECTION_CONFIGURATION

  def index
     @categories=EventCategory.categories(true).sort
     if params['name']
      @category=EventCategory.category(params['name'])
    else
      @category=EventCategory.new()
    end
  end

  def save
    category=EventCategory.new(params['name'], params['description'])

    if params[:previous_name]!=category.name
      errors=category.rename(params[:previous_name])
    else
      errors=category.save
    end

    if errors.empty?
      flash[:notice]='Category saved.'
    else
      flash[:error]=errors.join('<br/>')
    end
    redirect_to :action => 'index'
  end

  def delete
    if params['name']
      category=EventCategory.category(params['name'])
      if category
        category.delete
        flash[:notice]='Category deleted.'
      end
    end
    redirect_to :action => 'index'
  end

end
