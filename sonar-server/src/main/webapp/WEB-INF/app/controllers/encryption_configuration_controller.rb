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
class EncryptionConfigurationController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION
  before_filter :admin_required
  before_filter :remove_layout

  verify :method => :post, :only => [:generate_secret, :encrypt], :redirect_to => {:action => :index}

  def index
    if java_facade.hasSecretKey()
      render :action => 'index'
    else
      render :action => 'generate_secret_form'
    end
  end

  def generate_secret_form

  end

  def generate_secret
    @secret = java_facade.generateRandomSecretKey()
    render :partial => 'encryption_configuration/generate_secret_key'
  end

  def encrypt
    bad_request('No secret key') unless java_facade.hasSecretKey()
    @encrypted = java_facade.encrypt(params[:text])
    render :partial => 'encryption_configuration/encrypt'
  end

  private

  def remove_layout
    params[:layout]='false'
  end
end
