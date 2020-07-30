/*
 * Copyright 2020 Armory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.security;

import java.util.Set;

public interface CredentialsProvider<T extends AccountCredentials<?>> {

  /**
   * Returns all of the accounts known to the repository of this provider.
   *
   * @return a set of account names
   */
  Set<T> getAll();

  /**
   * Returns a specific {@link AccountCredentials} object a specified name
   *
   * @param name the name of the account
   * @return account credentials object
   */
  AccountCredentials getCredentials(String name);
}
