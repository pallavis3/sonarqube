/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
// @flow
import React from 'react';
import { Link, IndexLink } from 'react-router';
import type { Organization } from '../../../store/organizations/duck';

export default class OrganizationNavigation extends React.Component {
  props: {
    organization: Organization
  };

  render () {
    const { organization } = this.props;

    return (
        <nav className="navbar navbar-context page-container" id="context-navigation">
          <div className="navbar-context-inner">
            <div className="container">
              <ul className="nav navbar-nav nav-crumbs">
                <li>
                  <Link to={`/organizations/${organization.key}`}>
                    {organization.name}
                  </Link>
                </li>
              </ul>

              <ul className="nav navbar-nav nav-tabs">
                <li>
                  <IndexLink to={`/organizations/${organization.key}`} activeClassName="active">
                    <i className="icon-home"/>
                  </IndexLink>
                </li>
              </ul>
            </div>
          </div>
        </nav>
    );
  }
}
