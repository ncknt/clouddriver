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

import com.netflix.spinnaker.accounts.Credentials;
import com.netflix.spinnaker.accounts.CredentialsRepository;
import java.util.*;

public class StaticCredentialsRepository<T extends Credentials>
    implements CredentialsRepository<T> {
  private String type;
  private Map<String, T> credentials = new HashMap<>();

  public StaticCredentialsRepository(String type, Collection<T> allCredentials) {
    this.type = type;
    allCredentials.stream().forEach(c -> credentials.put(c.getName(), c));
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
  public T save(String key, T account) {
    return credentials.put(key, account);
  }

  @Override
  public T update(String key, T account) {
    return credentials.put(key, account);
  }

  @Override
  public void delete(String key) {
    credentials.remove(key);
  }

  @Override
  public String getType() {
    return type;
  }
}
