/*
 *
 * Copyright (c) 2013 - 2018 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.ocsp.qa.shell.completer;

import java.util.LinkedList;
import java.util.List;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.console.karaf.AbstractEnumCompleter;
import org.xipki.ocsp.qa.Occurrence;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */
@Service
public class OccurrenceCompleter extends AbstractEnumCompleter {

  public OccurrenceCompleter() {
    List<String> enums = new LinkedList<>();
    for (Occurrence entry : Occurrence.values()) {
      enums.add(entry.name());
    }
    setTokens(enums);
  }

}
