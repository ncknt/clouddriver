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

public class MapBackedCredentialsRepository<P extends Account, T extends Credentials>
    implements ReloadableCredentialsRepository<T> {
  protected Map<String, T> credentials = new HashMap<>();
  protected AccountSource<P> accountSource;
  protected CredentialsLifecycleHandler<T> eventHandler;
  protected AccountParser<P, T> parser;
  @Getter protected String type;

  // New empty and static
  public MapBackedCredentialsRepository(String type) {
    this.type = type;
    this.accountSource = () -> Collections.emptyList();
    this.parser = null;
    this.eventHandler = null;
  }

  public MapBackedCredentialsRepository(
      String type,
      AccountSource<P> source,
      AccountParser<P, T> parser,
      CredentialsLifecycleHandler<T> eventHandler) {
    this.type = type;
    this.eventHandler = eventHandler;
    this.parser = parser;
    this.accountSource = source;
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
    credentials.keySet().stream()
        .filter(name -> !latestAccountNames.contains(name))
        .forEach(this::delete);

    // modified accounts
    accountProps.stream()
        .filter(p -> this.credentials.containsKey(p.getName()))
        .map(parser::parse)
        .filter(Objects::nonNull)
        .forEach(a -> update(a.getName(), a));

    // new accounts
    accountProps.stream()
        .filter(p -> !this.credentials.containsKey(p.getName()))
        .map(parser::parse)
        .filter(Objects::nonNull)
        .forEach(a -> save(a.getName(), a));
  }

  @Override
  public T getOne(String key) {
    return credentials.get(key);
  }

  @Override
  public Set<T> getAll() {
    return new HashSet<>(credentials.values());
  }

  @Override
  public T save(String key, T credentials) {
    if (eventHandler != null) {
      eventHandler.credentialsAdded(credentials);
    }
    return this.credentials.put(key, credentials);
  }

  @Override
  public T update(String key, T credentials) {
    if (eventHandler != null) {
      eventHandler.credentialsUpdated(credentials);
    }
    return this.credentials.put(key, credentials);
  }

  @Override
  public void delete(String key) {
    T credentials = this.credentials.remove(key);
    if (credentials != null && eventHandler != null) {
      eventHandler.credentialsDeleted(credentials);
    }
  }
}
