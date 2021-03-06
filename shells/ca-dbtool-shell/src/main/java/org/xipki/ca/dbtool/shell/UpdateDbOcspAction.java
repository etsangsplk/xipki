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

package org.xipki.ca.dbtool.shell;

import java.util.Map;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.dbtool.LiquibaseDatabaseConf;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "ca", name = "updatedb-ocsp", description = "update the OCSP database schema")
@Service
public class UpdateDbOcspAction extends LiquibaseAction {

  private static final String SCHEMA_FILE = "xipki/sql/ocsp-init.xml";

  @Override
  protected Object execute0() throws Exception {
    Map<String, LiquibaseDatabaseConf> dbConfs = getDatabaseConfs();

    for (String dbName : dbConfs.keySet()) {
      if (!dbName.toLowerCase().contains("ocsp")) {
        continue;
      }

      LiquibaseDatabaseConf dbConf = dbConfs.get(dbName);
      update(dbConf, SCHEMA_FILE);
    }
    return null;
  }

}
