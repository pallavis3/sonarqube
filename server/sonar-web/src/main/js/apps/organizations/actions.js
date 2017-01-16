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
import { getOrganization } from '../../api/organizations';
import { onFail } from '../../store/rootActions';
import { receiveOrganizations } from '../../store/organizations/duck';

export const fetchOrganization = (key: string): void => (dispatch: Function): void => {
  /* eslint-disable no-console */
  const onFulfilled = (organization: null | {}) => {
    if (organization) {
      dispatch(receiveOrganizations([organization]));
    } else {
      onFail(dispatch)();
    }
  };

  getOrganization(key).then(onFulfilled, onFail(dispatch));
};
