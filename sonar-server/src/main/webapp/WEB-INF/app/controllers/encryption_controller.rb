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
class EncryptionController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION
  before_filter :admin_required
  verify :method => :post, :only => [:generate_secret, :encrypt], :redirect_to => {:action => :index}

  def index
    @has_secret_key=java_facade.hasSecretKey()
  end

  def generate_secret
    begin
      @secret=java_facade.generateRandomSecretKey()
    rescue Exception => e
      flash[:error]=e.message
      redirect_to :action => :index
    end
  end

  def encrypt
    bad_request('No secret key') unless java_facade.hasSecretKey()
    @encrypted=java_facade.encrypt(params[:text])
    render :action => 'encrypt', :layout => false
  end

  private


end
