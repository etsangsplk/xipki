package org.xipki.dbi;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.database.api.DataSource;
import org.xipki.dbi.ca.jaxb.CainfoType;
import org.xipki.dbi.ca.jaxb.CertStoreType;
import org.xipki.dbi.ca.jaxb.CertStoreType.Cainfos;
import org.xipki.dbi.ca.jaxb.CertStoreType.Certprofileinfos;
import org.xipki.dbi.ca.jaxb.CertStoreType.CertsFiles;
import org.xipki.dbi.ca.jaxb.CertStoreType.Crls;
import org.xipki.dbi.ca.jaxb.CertStoreType.Requestorinfos;
import org.xipki.dbi.ca.jaxb.CertStoreType.Users;
import org.xipki.dbi.ca.jaxb.CertType;
import org.xipki.dbi.ca.jaxb.CertprofileinfoType;
import org.xipki.dbi.ca.jaxb.CertsType;
import org.xipki.dbi.ca.jaxb.CrlType;
import org.xipki.dbi.ca.jaxb.ObjectFactory;
import org.xipki.dbi.ca.jaxb.RequestorinfoType;
import org.xipki.dbi.ca.jaxb.UserType;
import org.xipki.security.api.PasswordResolverException;
import org.xipki.security.common.IoCertUtil;
import org.xipki.security.common.ParamChecker;

class CaCertStoreDbExporter extends DbPorter{
	
	private static final Logger LOG = LoggerFactory.getLogger(CaCertStoreDbExporter.class);
	private final Marshaller marshaller;
	private final SHA1Digest sha1md = new SHA1Digest();

	private final int COUNT_CERTS_IN_ONE_FILE  = 100;

	CaCertStoreDbExporter(DataSource dataSource, Marshaller marshaller, String baseDir) 
			throws SQLException, PasswordResolverException, IOException
	{
		super(dataSource, baseDir);
		ParamChecker.assertNotNull("marshaller", marshaller);
		this.marshaller = marshaller;
	}
	
	public void export() throws Exception
	{
		CertStoreType certstore = new CertStoreType();
		certstore.setVersion(VERSION);

		certstore.setCainfos(export_cainfo());
		certstore.setRequestorinfos(export_requestorinfo());
		certstore.setCertprofileinfos(export_certprofileinfo());
		certstore.setUsers(export_user());
		certstore.setCrls(export_crl());
		certstore.setCertsFiles(export_cert());
		
		JAXBElement<CertStoreType> root = new ObjectFactory().createCertStore(certstore);
		marshaller.marshal(root, new File(baseDir + File.separator + FILENAME_CA_CertStore));
	}

	private Crls export_crl()
	throws SQLException, IOException
	{
		Crls crls = new Crls();
		Statement stmt = null;
		try{
			stmt = createStatement();
			String sql = "SELECT id, cainfo_id, crl_number, thisUpdate, nextUpdate, crl"
					+ " FROM crl";
			ResultSet rs = stmt.executeQuery(sql);		

			File crlDir = new File(baseDir + File.separator + DIRNAME_CRL);
			while(rs.next()){
				int id = rs.getInt("id");
				int cainfo_id = rs.getInt("cainfo_id");
				String crl_number = rs.getString("crl_number");
				String thisUpdate = rs.getString("thisUpdate");
				String nextUpdate = rs.getString("nextUpdate");
				Blob blob = rs.getBlob("crl");					

				byte[] encodedCrl = readBlob(blob);
				String fp = fp(encodedCrl);
				File f = new File(crlDir, fp);
				IoCertUtil.save(f, encodedCrl);

				CrlType crl = new CrlType();
				
				crl.setId(id);
				crl.setCainfoId(cainfo_id);
				crl.setCrlNumber(crl_number);
				crl.setThisUpdate(thisUpdate);
				crl.setNextUpdate(nextUpdate);
				crl.setCrlFile("CRL/" + fp);
				
				crls.getCrl().add(crl);
			}
			
			rs.close();
			rs = null;
		}finally
		{
			closeStatement(stmt);
		}

		return crls;
	}

	private Cainfos export_cainfo()
	throws SQLException
	{
		Cainfos cainfos = new Cainfos();
				
		Statement stmt = null;
		try{
			stmt = createStatement();
			String sql = "SELECT id, subject, cert, sha1_fp_cert"
					+ " FROM cainfo";
			ResultSet rs = stmt.executeQuery(sql);		

			while(rs.next()){
				int id = rs.getInt("id");
				String subject = rs.getString("subject");
				String cert = rs.getString("cert");
				String sha1_fp_cert = rs.getString("sha1_fp_cert");

				CainfoType cainfo = new CainfoType();
				cainfo.setId(id);
				cainfo.setSubject(subject);
				cainfo.setCert(cert);
				cainfo.setSha1FpCert(sha1_fp_cert);
				
				cainfos.getCainfo().add(cainfo);
			}
			
			rs.close();
			rs = null;
		}finally
		{
			closeStatement(stmt);
		}

		return cainfos;
	}

	private Requestorinfos export_requestorinfo()
	throws SQLException
	{
		Requestorinfos infos = new Requestorinfos();
				
		Statement stmt = null;
		try{
			stmt = createStatement();
			String sql = "SELECT id, subject, cert, sha1_fp_cert"
					+ " FROM requestorinfo";
			ResultSet rs = stmt.executeQuery(sql);		

			while(rs.next()){
				int id = rs.getInt("id");
				String subject = rs.getString("subject");
				String cert = rs.getString("cert");
				String sha1_fp_cert = rs.getString("sha1_fp_cert");

				RequestorinfoType info = new RequestorinfoType();
				info.setId(id);
				info.setSubject(subject);
				info.setCert(cert);
				info.setSha1FpCert(sha1_fp_cert);
				
				infos.getRequestorinfo().add(info);
			}
			
			rs.close();
			rs = null;
		}finally
		{
			closeStatement(stmt);
		}

		return infos;
	}

	private Users export_user()
	throws SQLException
	{
		Users users = new Users();
				
		Statement stmt = null;
		try{
			stmt = createStatement();
			String sql = "SELECT id, name"
					+ " FROM user";
			ResultSet rs = stmt.executeQuery(sql);		

			while(rs.next()){
				int id = rs.getInt("id");
				String name = rs.getString("name");

				UserType user = new UserType();
				user.setId(id);
				user.setName(name);
				
				users.getUser().add(user);
			}
			
			rs.close();
			rs = null;
		}finally
		{
			closeStatement(stmt);
		}

		return users;
	}

	private Certprofileinfos export_certprofileinfo()
	throws SQLException
	{
		Certprofileinfos infos = new Certprofileinfos();
				
		Statement stmt = null;
		try{
			stmt = createStatement();
			String sql = "SELECT id, name"
					+ " FROM certprofileinfo";
			ResultSet rs = stmt.executeQuery(sql);		

			while(rs.next()){
				int id = rs.getInt("id");
				String name = rs.getString("name");

				CertprofileinfoType info = new CertprofileinfoType();
				info.setId(id);
				info.setName(name);
				
				infos.getCertprofileinfo().add(info);
			}
			
			rs.close();
			rs = null;
		}finally
		{
			closeStatement(stmt);
		}

		return infos;
	}

	private CertsFiles export_cert()
	throws SQLException, IOException, JAXBException
	{	
		CertsFiles certsFiles = new CertsFiles();
	
		String certSql_Part1 = "SELECT id, cainfo_id, serial, certprofileinfo_id," +
				" requestorinfo_id, subject, last_update, notbefore, notafter," +
				" revocated, rev_reason, rev_time, rev_invalidity_time, user_id," +
				" sha1_fp_pk";
		String certSql_Part2 = 	
				" FROM cert" +
				" WHERE id > ? AND id < ?";		
		
		boolean sha1_fp_subject_present = true;
		// test whether filed sha1_fp_subject is available		
		PreparedStatement ps = prepareStatement(certSql_Part1 + ", sha1_fp_subject" + certSql_Part2);
		try{
			ps.setInt(1, 0);
			ps.setInt(2, 2);
			ps.executeQuery();
		}catch(SQLException e)
		{
			sha1_fp_subject_present = false;
			ps = prepareStatement(certSql_Part1 + certSql_Part2);
		}
		
		String rawCertSql = "SELECT sha1_fp, cert FROM rawcert WHERE cert_id = ?";
		PreparedStatement rawCertPs = prepareStatement(rawCertSql);
		
		File certDir = new File(baseDir, "CERT");
		
		final int minCertId = getMinCertId();
		final int maxCertId = getMaxCertId();
		
		final ObjectFactory objFact = new ObjectFactory();
		
		int numCertInCurrentFile = 0;
		int startIdInCurrentFile = minCertId;
		CertsType certsInCurrentFile = new CertsType();
		
		final int n = 100;
		try{
			for(int i = minCertId; i <= maxCertId; i += n)
			{
				ps.setInt(1, i - 1);
				ps.setInt(2, i + n + 1);

				ResultSet rs = ps.executeQuery();		

				while(rs.next()){
					int id = rs.getInt("id");
					String cainfo_id = rs.getString("cainfo_id");
					String serial = rs.getString("serial");
					String certprofileinfo_id = rs.getString("certprofileinfo_id");
					String requestorinfo_id = rs.getString("requestorinfo_id");
					String subject = rs.getString("subject");
					String last_update = rs.getString("last_update");
					String notbefore = rs.getString("notbefore");
					String notafter = rs.getString("notafter");
					boolean revocated = rs.getBoolean("revocated");
					String rev_reason = rs.getString("rev_reason");
					String rev_time = rs.getString("rev_time");
					String rev_invalidity_time = rs.getString("rev_invalidity_time");
					String user_id = rs.getString("user_id");
					String sha1_fp_pk = rs.getString("sha1_fp_pk");
					String sha1_fp_subject = null;
					if(sha1_fp_subject_present)
					{
						sha1_fp_subject = rs.getString("sha1_fp_subject");
						if(sha1_fp_subject.length() != 40) // invalid value
						{
							sha1_fp_subject = null;
						}
					}

					String sha1_fp_cert;
					rawCertPs.setInt(1, id);
					ResultSet rawCertRs = rawCertPs.executeQuery();
					try{
						rawCertRs.next();
						sha1_fp_cert = rawCertRs.getString("sha1_fp");
						String b64Cert = rawCertRs.getString("cert");
						IoCertUtil.save(new File(certDir, sha1_fp_cert), Base64.decode(b64Cert));
					}finally
					{
						rawCertRs.close();
					}

					CertType cert = new CertType();
					cert.setId(id);
					cert.setCainfoId(cainfo_id);
					cert.setSerial(serial);
					cert.setCertprofileinfoId(certprofileinfo_id);
					cert.setRequestorinfoId(requestorinfo_id);
					cert.setSubject(subject);
					cert.setLastUpdate(last_update);
					cert.setNotbefore(notbefore);
					cert.setNotafter(notafter);
					cert.setRevocated(revocated);
					cert.setRevReason(rev_reason);
					cert.setRevTime(rev_time);
					cert.setRevInvalidityTime(rev_invalidity_time);
					cert.setUserId(user_id);
					cert.setSha1FpPk(sha1_fp_pk);
					cert.setSha1FpSubject(sha1_fp_subject);
					cert.setSha1FpCert(sha1_fp_cert);
					cert.setCertFile(DIRNAME_CERT + File.separator + sha1_fp_cert);

					if(certsInCurrentFile.getCert().isEmpty())
					{
						startIdInCurrentFile = id;
					}
					
					certsInCurrentFile.getCert().add(cert);
					numCertInCurrentFile ++;
					
					if(numCertInCurrentFile == COUNT_CERTS_IN_ONE_FILE)
					{
						String fn = PREFIX_FILENAME_CERTS + startIdInCurrentFile + ".xml";						
						marshaller.marshal(objFact.createCerts(certsInCurrentFile), 
								new File(baseDir + File.separator + fn));
						
						certsFiles.getCertsFile().add(fn);
						
						certsInCurrentFile = new CertsType();
						numCertInCurrentFile = 0;
					}
				}
			}
			
			if(numCertInCurrentFile > 0)
			{
				String fn = "certs-" + startIdInCurrentFile + ".xml";						
				marshaller.marshal(objFact.createCerts(certsInCurrentFile), 
						new File(baseDir + File.separator + fn));
				
				certsFiles.getCertsFile().add(fn);
			}

		}finally
		{
			closeStatement(ps);
		}

		
		return certsFiles;
	}

	private int getMinCertId()
	throws SQLException
	{
		Statement stmt = null;
		try{
			stmt = createStatement();
			final String sql = "SELECT min(id) FROM cert";
			ResultSet rs = stmt.executeQuery(sql);
			
			rs.next();
			int minCertId = rs.getInt(1);
			
			rs.close();
			rs = null;
			
			return minCertId;
		}finally
		{
			closeStatement(stmt);
		}
	}

	private int getMaxCertId()
	throws SQLException
	{
		Statement stmt = null;
		try{
			stmt = createStatement();
			final String sql = "SELECT max(id) FROM cert";
			ResultSet rs = stmt.executeQuery(sql);
			
			rs.next();
			int maxCertId = rs.getInt(1);
			
			rs.close();
			rs = null;
			
			return maxCertId;
		}finally
		{
			closeStatement(stmt);
		}
	}

	private String fp(byte[] data)
	{
		synchronized (sha1md) {
			sha1md.reset();
			sha1md.update(data, 0, data.length);
			byte[] digestValue = new byte[20];
			sha1md.doFinal(digestValue, 0);
			return Hex.toHexString(digestValue).toUpperCase();
		}
	}
	
	private static byte[] readBlob(Blob blob)
	{
		InputStream is;
		try {
			is = blob.getBinaryStream();
		} catch (SQLException e) {
			String msg = "Could not getBinaryStream from Blob"; 
			LOG.warn(msg + " {}", e.getMessage());
			LOG.debug(msg, e);
			return null;
		}
		try{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			byte[] buffer = new byte[2048];
			int readed;
			
			try {
				while((readed = is.read(buffer)) != -1)
				{				
					if(readed > 0)
					{
						out.write(buffer, 0, readed);
					}
				}
			} catch (IOException e) {
				String msg = "Could not read CRL from Blob"; 
				LOG.warn(msg + " {}", e.getMessage());
				LOG.debug(msg, e);
				return null;
			}
			
			return out.toByteArray();
		}finally{
			try{
				is.close();
			}catch(IOException e)
			{				
			}
		}
	}

	
}