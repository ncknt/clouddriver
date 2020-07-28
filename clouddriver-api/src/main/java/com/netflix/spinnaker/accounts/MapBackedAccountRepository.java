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
import lombok.Getter;

public class MapBackedAccountRepository<P extends Account, T extends Credentials>
    implements ReloadableAccountRepository<T> {
  protected Map<String, T> accounts = new HashMap<>();
  protected AccountSource<P> accountSource;
  protected CredentialsLifecycleHandler<T> eventHandler;
  protected AccountParser<P, T> parser;
  @Getter protected String type;

  public MapBackedAccountRepository(
      String type,
      AccountSource<P> source,
      AccountParser<P, T> parser,
      CredentialsLifecycleHandler<T> eventHandler) {
    this(type, parser, eventHandler);
    this.accountSource = source;
  }

  public MapBackedAccountRepository(
      String type,
      Collection<P> props,
      AccountParser<P, T> parser,
      CredentialsLifecycleHandler<T> eventHandler) {
    this(type, parser, eventHandler);
    this.parse(props);
  }

  protected MapBackedAccountRepository(
      String type, AccountParser<P, T> parser, CredentialsLifecycleHandler<T> eventHandler) {
    this.type = type;
    this.eventHandler = eventHandler;
    this.parser = parser;
  }

  public void load() {
    if (accountSource != null) {
      this.parse(accountSource.getAccounts());
    }
  }

  protected void parse(Collection<P> accountProps) {
    Set<String> latestAccountNames =
        accountProps.stream().map(P::getName).collect(Collectors.toSet());

    // deleted accounts
    accounts.keySet().stream()
        .filter(name -> !latestAccountNames.contains(name))
        .forEach(this::delete);

    // modified accounts
    accountProps.stream()
        .filter(p -> this.accounts.containsKey(p.getName()))
        .map(parser::parse)
        .filter(Objects::nonNull)
        .forEach(a -> update(a.getName(), a));

    // new accounts
    accountProps.stream()
        .filter(p -> !this.accounts.containsKey(p.getName()))
        .map(parser::parse)
        .filter(Objects::nonNull)
        .forEach(a -> save(a.getName(), a));
  }

  @Override
  public T getOne(String key) {
    return accounts.get(key);
  }

  @Override
  public Set<T> getAll() {
    return new HashSet<>(accounts.values());
  }

  @Override
  public T save(String key, T account) {
    if (eventHandler != null) {
      eventHandler.accountAdded(account);
    }
    return accounts.put(key, account);
  }

  @Override
  public T update(String key, T account) {
    if (eventHandler != null) {
      eventHandler.accountUpdated(account);
    }
    return accounts.put(key, account);
  }

  @Override
  public void delete(String key) {
    T account = accounts.remove(key);
    if (account != null && eventHandler != null) {
      eventHandler.accountDeleted(account);
    }
  }
}
