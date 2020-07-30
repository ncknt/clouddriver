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

import com.netflix.spinnaker.accounts.CredentialsRepository;
import java.util.Set;

public class TypedAccountCredentialsProvider<T extends AccountCredentials<?>>
    implements CredentialsProvider<T> {
  private final CredentialsRepository<T> credentialsRepository;

  public TypedAccountCredentialsProvider(CredentialsRepository<T> credentialsRepository) {
    this.credentialsRepository = credentialsRepository;
  }

  @Override
  public Set<T> getAll() {
    return credentialsRepository.getAll();
  }

  @Override
  public AccountCredentials getCredentials(String name) {
    return credentialsRepository.getOne(name);
  }
}
