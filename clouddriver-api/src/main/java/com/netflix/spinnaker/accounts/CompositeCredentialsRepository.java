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

package com.netflix.spinnaker.accounts;

import java.util.*;
import java.util.stream.Collectors;

public class CompositeCredentialsRepository<T extends Credentials> {
  private Map<String, CredentialsRepository<? extends T>> allRepositories;

  public CompositeCredentialsRepository(List<CredentialsRepository<? extends T>> repositories) {
    this.allRepositories = new HashMap<>();
    repositories.forEach(this::registerRepository);
  }

  public void registerRepository(CredentialsRepository<? extends T> repository) {
    this.allRepositories.put(repository.getType(), repository);
  }

  public T getCredentials(String accountName, String type) {
    if (accountName == null || accountName.equals("")) {
      throw new IllegalArgumentException(
          "An artifact account must be supplied to download this artifact: " + accountName);
    }

    CredentialsRepository<? extends T> repository = allRepositories.get(type);
    if (repository == null) {
      throw new IllegalArgumentException("No artifact type '" + type + "' registered");
    }

    T account = repository.getOne(accountName);
    if (account == null) {
      throw new IllegalArgumentException(
          "Artifact credentials '"
              + accountName
              + "' cannot handle artifacts of type '"
              + type
              + "'");
    }

    return account;
  }

  public List<T> getAllCredentials() {
    return Collections.unmodifiableList(
        allRepositories.values().stream()
            .map(CredentialsRepository::getAll)
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));
  }
}
