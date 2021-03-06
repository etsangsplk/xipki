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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.xipki.ca.dbtool.diffdb.DigestDiffWorker;
import org.xipki.ca.dbtool.port.DbPortWorker;
import org.xipki.common.util.IoUtil;
import org.xipki.console.karaf.completer.DirCompleter;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "ca", name = "diff-digest-db", description = "diff digest XiPKI/EJBCA database")
@Service
public class DiffDigestDbAction extends DbPortAction {

  @Option(name = "--ref-db", required = true,
      description = "database configuration file of the reference system")
  @Completion(FileCompleter.class)
  private String refDbConf;

  @Option(name = "--target", required = true,
      description = "configuration file of the target database to be evaluated")
  @Completion(FileCompleter.class)
  private String dbconfFile;

  @Option(name = "--report-dir", required = true, description = "report directory")
  @Completion(DirCompleter.class)
  private String reportDir;

  @Option(name = "--revoked-only", description = "considers only the revoked certificates")
  private Boolean revokedOnly = Boolean.FALSE;

  @Option(name = "-k", description = "number of certificates per SELECT")
  private Integer numCertsPerSelect = 1000;

  @Option(name = "--target-threads", description = "number of threads to query the target database")
  private Integer numTargetThreads = 40;

  @Option(name = "--ca-cert", multiValued = true,
      description = "Certificate of CAs to be considered")
  @Completion(FileCompleter.class)
  private List<String> caCertFiles;

  protected DbPortWorker getDbPortWorker() throws Exception {
    Set<byte[]> caCerts = null;
    if (caCertFiles != null && !caCertFiles.isEmpty()) {
      caCerts = new HashSet<>(caCertFiles.size());
      for (String fileName : caCertFiles) {
        caCerts.add(IoUtil.read(fileName));
      }
    }

    return new DigestDiffWorker(datasourceFactory, passwordResolver, revokedOnly,
        refDbConf, dbconfFile, reportDir, numCertsPerSelect, numTargetThreads, caCerts);
  }

}
