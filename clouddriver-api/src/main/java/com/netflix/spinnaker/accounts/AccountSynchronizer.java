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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccountSynchronizer {
  @Autowired private List<Reloader> reloaders = new ArrayList<>();
  private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

  @PostConstruct
  public void start() {
    reloaders.stream()
        .filter(r -> r.getFrequencyMs() > 0)
        .forEach(r -> executor.schedule(r, r.getFrequencyMs(), TimeUnit.MILLISECONDS));
  }

  public void schedule(Reloader<?> reloader) {
    executor.schedule(reloader, reloader.getFrequencyMs(), TimeUnit.MILLISECONDS);
  }
}
