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

package org.xipki.ca.server.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.List;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.CertificationRequest;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.dsa.DSAUtil;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.ca.api.BadCertTemplateException;
import org.xipki.ca.api.CaUris;
import org.xipki.ca.api.OperationException;
import org.xipki.ca.api.OperationException.ErrorCode;
import org.xipki.ca.api.PublicCaInfo;
import org.xipki.ca.api.profile.CertLevel;
import org.xipki.ca.api.profile.CertValidity;
import org.xipki.ca.api.profile.CertprofileException;
import org.xipki.ca.api.profile.ExtensionValue;
import org.xipki.ca.api.profile.ExtensionValues;
import org.xipki.ca.api.profile.SubjectInfo;
import org.xipki.ca.server.mgmt.api.CaEntry;
import org.xipki.common.ConfPairs;
import org.xipki.common.InvalidConfException;
import org.xipki.common.ObjectCreationException;
import org.xipki.common.util.CollectionUtil;
import org.xipki.common.util.ParamUtil;
import org.xipki.security.ConcurrentBagEntrySigner;
import org.xipki.security.ConcurrentContentSigner;
import org.xipki.security.SecurityFactory;
import org.xipki.security.SignerConf;
import org.xipki.security.exception.NoIdleSignerException;
import org.xipki.security.exception.XiSecurityException;
import org.xipki.security.util.KeyUtil;
import org.xipki.security.util.X509Util;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

class SelfSignedCertBuilder {

  static class GenerateSelfSignedResult {

    private final String signerConf;

    private final X509Certificate cert;

    GenerateSelfSignedResult(String signerConf, X509Certificate cert) {
      this.signerConf = signerConf;
      this.cert = cert;
    }

    String getSignerConf() {
      return signerConf;
    }

    X509Certificate getCert() {
      return cert;
    }

  } // class GenerateSelfSignedResult

  private static final Logger LOG = LoggerFactory.getLogger(SelfSignedCertBuilder.class);

  private SelfSignedCertBuilder() {
  }

  public static GenerateSelfSignedResult generateSelfSigned(SecurityFactory securityFactory,
      String signerType, String signerConf, IdentifiedCertprofile certprofile,
      CertificationRequest csr, BigInteger serialNumber, CaUris caUris, ConfPairs extraControl)
          throws OperationException, InvalidConfException {
    ParamUtil.requireNonNull("securityFactory", securityFactory);
    ParamUtil.requireNonBlank("signerType", signerType);
    ParamUtil.requireNonNull("certprofile", certprofile);
    ParamUtil.requireNonNull("csr", csr);
    ParamUtil.requireNonNull("serialNumber", serialNumber);
    if (serialNumber.compareTo(BigInteger.ZERO) != 1) {
      throw new IllegalArgumentException(
          "serialNumber must not be non-positive: " + serialNumber);
    }

    CertLevel level = certprofile.getCertLevel();
    if (CertLevel.RootCA != level) {
      throw new IllegalArgumentException("certprofile is not of level " + CertLevel.RootCA);
    }

    if (!securityFactory.verifyPopo(csr, null)) {
      throw new InvalidConfException("could not validate POP for the CSR");
    }

    if ("pkcs12".equalsIgnoreCase(signerType) || "jks".equalsIgnoreCase(signerType)) {
      ConfPairs keyValues = new ConfPairs(signerConf);
      String keystoreConf = keyValues.value("keystore");
      if (keystoreConf == null) {
        throw new InvalidConfException(
          "required parameter 'keystore' for types PKCS12 and JKS, is not specified");
      }
    }

    ConcurrentContentSigner signer;
    try {
      List<String[]> signerConfs = CaEntry.splitCaSignerConfs(signerConf);
      List<String> restrictedSigAlgos = certprofile.getSignatureAlgorithms();

      String thisSignerConf = null;
      if (CollectionUtil.isEmpty(restrictedSigAlgos)) {
        thisSignerConf = signerConfs.get(0)[1];
      } else {
        for (String algo : restrictedSigAlgos) {
          for (String[] m : signerConfs) {
            if (m[0].equals(algo)) {
              thisSignerConf = m[1];
              break;
            }
          }

          if (thisSignerConf != null) {
            break;
          }
        }
      }

      if (thisSignerConf == null) {
        throw new OperationException(ErrorCode.SYSTEM_FAILURE,
          "CA does not support any signature algorithm restricted by the cert profile");
      }

      signer = securityFactory.createSigner(signerType, new SignerConf(thisSignerConf),
          (X509Certificate[]) null);
    } catch (XiSecurityException | ObjectCreationException ex) {
      throw new OperationException(ErrorCode.SYSTEM_FAILURE, ex);
    }

    SubjectPublicKeyInfo publicKeyInfo;
    if (signer.getCertificate() != null) {
      // this cert is the dummy one which can be considered only as public key container
      Certificate bcCert;
      try {
        bcCert = Certificate.getInstance(signer.getCertificate().getEncoded());
      } catch (Exception ex) {
        throw new OperationException(ErrorCode.SYSTEM_FAILURE,
            "could not reparse certificate: " + ex.getMessage());
      }
      publicKeyInfo = bcCert.getSubjectPublicKeyInfo();
    } else {
      PublicKey signerPublicKey = signer.getPublicKey();
      try {
        publicKeyInfo = KeyUtil.createSubjectPublicKeyInfo(signerPublicKey);
      } catch (InvalidKeyException ex) {
        throw new OperationException(ErrorCode.SYSTEM_FAILURE,
            "cannot generate SubjectPublicKeyInfo from publicKey: " + ex.getMessage());
      }
    }

    X509Certificate newCert = generateCertificate(signer, certprofile, csr, serialNumber,
        publicKeyInfo, caUris, extraControl);

    return new GenerateSelfSignedResult(signerConf, newCert);
  } // method generateSelfSigned

  private static X509Certificate generateCertificate(ConcurrentContentSigner signer,
      IdentifiedCertprofile certprofile, CertificationRequest csr, BigInteger serialNumber,
      SubjectPublicKeyInfo publicKeyInfo, CaUris caUris, ConfPairs extraControl)
      throws OperationException {

    try {
      publicKeyInfo = X509Util.toRfc3279Style(publicKeyInfo);
    } catch (InvalidKeySpecException ex) {
      LOG.warn("SecurityUtil.toRfc3279Style", ex);
      throw new OperationException(ErrorCode.BAD_CERT_TEMPLATE, ex);
    }

    try {
      certprofile.checkPublicKey(publicKeyInfo);
    } catch (CertprofileException ex) {
      throw new OperationException(ErrorCode.SYSTEM_FAILURE,
          "exception in cert profile " + certprofile.getIdent());
    } catch (BadCertTemplateException ex) {
      LOG.warn("certprofile.checkPublicKey", ex);
      throw new OperationException(ErrorCode.BAD_CERT_TEMPLATE, ex);
    }

    X500Name requestedSubject = csr.getCertificationRequestInfo().getSubject();

    SubjectInfo subjectInfo;
    // subject
    try {
      subjectInfo = certprofile.getSubject(requestedSubject);
    } catch (CertprofileException ex) {
      throw new OperationException(ErrorCode.SYSTEM_FAILURE,
          "exception in cert profile " + certprofile.getIdent());
    } catch (BadCertTemplateException ex) {
      LOG.warn("certprofile.getSubject", ex);
      throw new OperationException(ErrorCode.BAD_CERT_TEMPLATE, ex);
    }

    Date notBefore = certprofile.getNotBefore(null);
    if (notBefore == null) {
      notBefore = new Date();
    }

    CertValidity validity = certprofile.getValidity();
    if (validity == null) {
      throw new OperationException(ErrorCode.BAD_CERT_TEMPLATE,
          "no validity specified in the profile " + certprofile.getIdent());
    }

    Date notAfter = validity.add(notBefore);

    X500Name grantedSubject = subjectInfo.getGrantedSubject();

    X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(grantedSubject,
        serialNumber, notBefore, notAfter, grantedSubject, publicKeyInfo);

    PublicCaInfo publicCaInfo = new PublicCaInfo(grantedSubject, serialNumber, null, null,
        caUris, extraControl);

    Extensions extensions = null;
    ASN1Set attrs = csr.getCertificationRequestInfo().getAttributes();
    for (int i = 0; i < attrs.size(); i++) {
      Attribute attr = Attribute.getInstance(attrs.getObjectAt(i));
      if (PKCSObjectIdentifiers.pkcs_9_at_extensionRequest.equals(attr.getAttrType())) {
        extensions = Extensions.getInstance(attr.getAttributeValues()[0]);
      }
    }

    try {
      addExtensions(certBuilder, certprofile, requestedSubject, grantedSubject, extensions,
          publicKeyInfo, publicCaInfo, notBefore, notAfter);

      ConcurrentBagEntrySigner signer0 = signer.borrowSigner();
      X509CertificateHolder certHolder;
      try {
        certHolder = certBuilder.build(signer0.value());
      } finally {
        signer.requiteSigner(signer0);
      }
      Certificate bcCert = certHolder.toASN1Structure();
      return X509Util.parseCert(bcCert.getEncoded());
    } catch (BadCertTemplateException ex) {
      throw new OperationException(ErrorCode.BAD_CERT_TEMPLATE, ex);
    } catch (NoIdleSignerException | CertificateException | IOException
        | CertprofileException ex) {
      throw new OperationException(ErrorCode.SYSTEM_FAILURE, ex);
    }
  } // method generateCertificate

  private static void addExtensions(X509v3CertificateBuilder certBuilder,
      IdentifiedCertprofile profile, X500Name requestedSubject, X500Name grantedSubject,
      Extensions extensions, SubjectPublicKeyInfo requestedPublicKeyInfo,
      PublicCaInfo publicCaInfo, Date notBefore, Date notAfter)
      throws CertprofileException, IOException, BadCertTemplateException {
    ExtensionValues extensionTuples = profile.getExtensions(requestedSubject, grantedSubject,
        extensions, requestedPublicKeyInfo, publicCaInfo, null, notBefore, notAfter);
    if (extensionTuples == null) {
      return;
    }

    for (ASN1ObjectIdentifier extType : extensionTuples.getExtensionTypes()) {
      ExtensionValue extValue = extensionTuples.getExtensionValue(extType);
      certBuilder.addExtension(extType, extValue.isCritical(), extValue.getValue());
    }
  } // method addExtensions

  public static AsymmetricKeyParameter generatePublicKeyParameter(PublicKey key)
      throws InvalidKeyException {
    ParamUtil.requireNonNull("key", key);
    if (key instanceof RSAPublicKey) {
      RSAPublicKey rsakey = (RSAPublicKey) key;
      return new RSAKeyParameters(false, rsakey.getModulus(), rsakey.getPublicExponent());
    } else if (key instanceof ECPublicKey) {
      return ECUtil.generatePublicKeyParameter(key);
    } else if (key instanceof DSAPublicKey) {
      return DSAUtil.generatePublicKeyParameter(key);
    } else {
      throw new InvalidKeyException("unknown key " + key.getClass().getName());
    }
  } // method generatePublicKeyParameter

}
