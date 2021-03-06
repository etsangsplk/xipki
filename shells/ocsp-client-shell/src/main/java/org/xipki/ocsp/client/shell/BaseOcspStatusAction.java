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

package org.xipki.ocsp.client.shell;

import java.math.BigInteger;
import java.net.URL;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.cert.AttributeCertificateIssuer;
import org.bouncycastle.cert.X509AttributeCertificateHolder;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.xipki.common.RequestResponseDebug;
import org.xipki.common.RequestResponsePair;
import org.xipki.common.util.BigIntegerRange;
import org.xipki.common.util.CollectionUtil;
import org.xipki.common.util.IoUtil;
import org.xipki.common.util.StringUtil;
import org.xipki.console.karaf.IllegalCmdParamException;
import org.xipki.ocsp.client.api.OcspRequestor;
import org.xipki.ocsp.client.api.RequestOptions;
import org.xipki.security.HashAlgo;
import org.xipki.security.IssuerHash;
import org.xipki.security.ObjectIdentifiers;
import org.xipki.security.util.X509Util;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

public abstract class BaseOcspStatusAction extends CommonOcspStatusAction {

  protected static final Map<ASN1ObjectIdentifier, String> EXTENSION_OIDNAME_MAP
      = new HashMap<>();

  @Option(name = "--verbose", aliases = "-v", description = "show status verbosely")
  protected Boolean verbose = Boolean.FALSE;

  @Option(name = "--resp-issuer", description = "certificate file of the responder's issuer")
  @Completion(FileCompleter.class)
  private String respIssuerFile;

  @Option(name = "--url", description = "OCSP responder URL")
  private String serverUrl;

  @Option(name = "--req-out", description = "where to save the request")
  @Completion(FileCompleter.class)
  private String reqout;

  @Option(name = "--resp-out", description = "where to save the response")
  @Completion(FileCompleter.class)
  private String respout;

  @Option(name = "--hex", description = "serial number without prefix is hex number")
  private Boolean hex = Boolean.FALSE;

  @Option(name = "--serial", aliases = "-s",
      description = "comma-separated serial numbers or ranges (like 1,3,6-10)\n"
          + "(at least one of serial and cert must be specified)")
  private String serialNumberList;

  @Option(name = "--cert", aliases = "-c", multiValued = true, description = "certificate")
  @Completion(FileCompleter.class)
  private List<String> certFiles;

  @Option(name = "--ac", description = "the certificates are attribute certificates")
  @Completion(FileCompleter.class)
  private Boolean isAttrCert = Boolean.FALSE;

  @Reference
  private OcspRequestor requestor;

  static {
    EXTENSION_OIDNAME_MAP.put(OCSPObjectIdentifiers.id_pkix_ocsp_archive_cutoff, "ArchiveCutoff");
    EXTENSION_OIDNAME_MAP.put(OCSPObjectIdentifiers.id_pkix_ocsp_crl, "CrlID");
    EXTENSION_OIDNAME_MAP.put(OCSPObjectIdentifiers.id_pkix_ocsp_nonce, "Nonce");
    EXTENSION_OIDNAME_MAP.put(ObjectIdentifiers.id_pkix_ocsp_extendedRevoke, "ExtendedRevoke");
  }

  /**
   * TODO.
   * @param respIssuer
   *          Expected responder issuer. Could be {@code null}.
   * @param serialNumbers
   *          Expected serial numbers. Must not be {@code null}.
   * @param encodedCerts
   *          Map of serial number and the corresponding certificate. Could be {@code null}.
   */
  protected abstract void checkParameters(X509Certificate respIssuer,
      List<BigInteger> serialNumbers, Map<BigInteger, byte[]> encodedCerts) throws Exception;

  /**
   * TODO.
   * @param response
   *          OCSP response. Must not be {@code null}.
   * @param respIssuer
   *          Expected responder issuer. Could be {@code null}.
   * @param issuerHash
   *          Expected issuer hash. Must not be {@code null}.
   * @param serialNumbers
   *          Expected serial numbers. Must not be {@code null}.
   * @param encodedCerts
   *          Map of serial number and the corresponding certificate. Could be {@code null}.
   */
  protected abstract Object processResponse(OCSPResp response, X509Certificate respIssuer,
      IssuerHash issuerHash, List<BigInteger> serialNumbers, Map<BigInteger, byte[]> encodedCerts)
      throws Exception;

  @Override
  protected final Object execute0() throws Exception {
    if (StringUtil.isBlank(serialNumberList) && isEmpty(certFiles)) {
      throw new IllegalCmdParamException("Neither serialNumbers nor certFiles is set");
    }

    X509Certificate issuerCert = X509Util.parseCert(issuerCertFile);

    Map<BigInteger, byte[]> encodedCerts = null;
    List<BigInteger> sns = new LinkedList<>();

    if (isNotEmpty(certFiles)) {
      encodedCerts = new HashMap<>(certFiles.size());

      String ocspUrl = null;

      X500Name issuerX500Name = null;

      for (String certFile : certFiles) {
        BigInteger sn;
        List<String> ocspUrls;

        if (isAttrCert) {
          if (issuerX500Name == null) {
            issuerX500Name = X500Name.getInstance(
                issuerCert.getSubjectX500Principal().getEncoded());
          }

          X509AttributeCertificateHolder cert =
              new X509AttributeCertificateHolder(IoUtil.read(certFile));
          // no signature validation
          AttributeCertificateIssuer reqIssuer = cert.getIssuer();
          if (reqIssuer != null && issuerX500Name != null) {
            X500Name reqIssuerName = reqIssuer.getNames()[0];
            if (!issuerX500Name.equals(reqIssuerName)) {
              throw new IllegalCmdParamException("certificate " + certFile
                  + " is not issued by the given issuer");
            }
          }

          ocspUrls = extractOcspUrls(cert);
          sn = cert.getSerialNumber();
        } else {
          X509Certificate cert = X509Util.parseCert(certFile);
          if (!X509Util.issues(issuerCert, cert)) {
            throw new IllegalCmdParamException(
                "certificate " + certFile + " is not issued by the given issuer");
          }
          ocspUrls = extractOcspUrls(cert);
          sn = cert.getSerialNumber();
        }

        if (isBlank(serverUrl)) {
          if (CollectionUtil.isEmpty(ocspUrls)) {
            throw new IllegalCmdParamException("could not extract OCSP responder URL");
          } else {
            String url = ocspUrls.get(0);
            if (ocspUrl != null && !ocspUrl.equals(url)) {
              throw new IllegalCmdParamException("given certificates have different"
                  + " OCSP responder URL in certificate");
            } else {
              ocspUrl = url;
            }
          }
        } // end if

        sns.add(sn);

        byte[] encodedCert = IoUtil.read(certFile);
        encodedCerts.put(sn, encodedCert);
      } // end for

      if (isBlank(serverUrl)) {
        serverUrl = ocspUrl;
      }
    } else {
      StringTokenizer st = new StringTokenizer(serialNumberList, ", ");
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        StringTokenizer st2 = new StringTokenizer(token, "-");
        BigInteger from = toBigInt(st2.nextToken(), hex);
        BigInteger to = st2.hasMoreTokens() ? toBigInt(st2.nextToken(), hex) : null;
        if (to == null) {
          sns.add(from);
        } else {
          BigIntegerRange range = new BigIntegerRange(from, to);
          if (range.getDiff().compareTo(BigInteger.valueOf(10)) > 0) {
            throw new IllegalCmdParamException("to many serial numbers");
          }

          BigInteger sn = range.getFrom();
          while (range.isInRange(sn)) {
            sns.add(sn);
            sn = sn.add(BigInteger.ONE);
          }
        }
      }
    }

    if (isBlank(serverUrl)) {
      throw new IllegalCmdParamException("could not get URL for the OCSP responder");
    }

    X509Certificate respIssuer = null;
    if (respIssuerFile != null) {
      respIssuer = X509Util.parseCert(IoUtil.expandFilepath(respIssuerFile));
    }

    URL serverUrlObj = new URL(serverUrl);
    RequestOptions options = getRequestOptions();
    checkParameters(respIssuer, sns, encodedCerts);
    boolean saveReq = isNotBlank(reqout);
    boolean saveResp = isNotBlank(respout);
    RequestResponseDebug debug = null;
    if (saveReq || saveResp) {
      debug = new RequestResponseDebug(saveReq, saveResp);
    }

    IssuerHash issuerHash = new IssuerHash(
        HashAlgo.getNonNullInstance(options.getHashAlgorithmId()),
        Certificate.getInstance(issuerCert.getEncoded()));
    OCSPResp response;
    try {
      response = requestor.ask(issuerCert, sns.toArray(new BigInteger[0]), serverUrlObj,
          options, debug);
    } finally {
      if (debug != null && debug.size() > 0) {
        RequestResponsePair reqResp = debug.get(0);
        if (saveReq) {
          byte[] bytes = reqResp.getRequest();
          if (bytes != null) {
            IoUtil.save(reqout, bytes);
          }
        }

        if (saveResp) {
          byte[] bytes = reqResp.getResponse();
          if (bytes != null) {
            IoUtil.save(respout, bytes);
          }
        }
      } // end if
    } // end finally

    return processResponse(response, respIssuer, issuerHash, sns, encodedCerts);
  } // method execute0

  public static List<String> extractOcspUrls(X509Certificate cert)
      throws CertificateEncodingException {
    byte[] extnValue = X509Util.getCoreExtValue(cert, Extension.authorityInfoAccess);
    if (extnValue == null) {
      return Collections.emptyList();
    }

    AuthorityInformationAccess aia = AuthorityInformationAccess.getInstance(extnValue);
    return extractOcspUrls(aia);
  }

  public static List<String> extractOcspUrls(X509AttributeCertificateHolder cert)
      throws CertificateEncodingException {
    byte[] extValue = X509Util.getCoreExtValue(cert, Extension.authorityInfoAccess);
    if (extValue == null) {
      return Collections.emptyList();
    }
    AuthorityInformationAccess aia = AuthorityInformationAccess.getInstance(extValue);
    return extractOcspUrls(aia);
  }

  public static List<String> extractOcspUrls(AuthorityInformationAccess aia)
      throws CertificateEncodingException {
    AccessDescription[] accessDescriptions = aia.getAccessDescriptions();
    List<AccessDescription> ocspAccessDescriptions = new LinkedList<>();
    for (AccessDescription accessDescription : accessDescriptions) {
      if (accessDescription.getAccessMethod().equals(X509ObjectIdentifiers.id_ad_ocsp)) {
        ocspAccessDescriptions.add(accessDescription);
      }
    }

    final int n = ocspAccessDescriptions.size();
    List<String> ocspUris = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      GeneralName accessLocation = ocspAccessDescriptions.get(i).getAccessLocation();
      if (accessLocation.getTagNo() == GeneralName.uniformResourceIdentifier) {
        String ocspUri = ((ASN1String) accessLocation.getName()).getString();
        ocspUris.add(ocspUri);
      }
    }

    return ocspUris;
  }

}
