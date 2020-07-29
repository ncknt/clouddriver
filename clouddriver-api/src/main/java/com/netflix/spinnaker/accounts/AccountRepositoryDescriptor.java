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

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.Builder;

@Builder
public class AccountRepositoryDescriptor<T extends Credentials, U extends Account> {
  private final String type;
  private final AccountParser<U, T> parser;
  private Supplier<List<U>> springAccountSource;
  private Optional<CredentialsLifecycleHandler<T>> eventHandler;
  private Optional<AccountSource<U>> customAccountSource;

  public AccountRepository<T> createRepository() {
    AccountSource<U> source = customAccountSource.orElse(() -> springAccountSource.get());
    CredentialsLifecycleHandler<T> handler = eventHandler.orElse(null);
    MapBackedAccountRepository repository =
        new MapBackedAccountRepository<>(type, source, parser, handler);
    return repository;
  }
}
