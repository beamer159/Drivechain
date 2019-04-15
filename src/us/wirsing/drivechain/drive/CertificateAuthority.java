package us.wirsing.drivechain.drive;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import us.wirsing.drivechain.util.Crypto;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

/**
 * Represents a certificate authority
 * Responsible for issuing certificates to
 */
public class CertificateAuthority {

	private KeyPair keyPair = Crypto.generateKey();
	public X509Certificate certificate = generateCertificate();

	private X509Certificate generateCertificate() {
		X500Name nameDn = new X500Name("CN=caDrive");
		X509v3CertificateBuilder builderCert = getBuilderCert(nameDn, nameDn,
				SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));
		BasicConstraints basicConstraints = new BasicConstraints(true);
		JcaX509CertificateConverter converterCert = new JcaX509CertificateConverter();
		try {
			builderCert.addExtension(Extension.basicConstraints, true, basicConstraints);
			ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRsa").build(keyPair.getPrivate());
			return converterCert.getCertificate(builderCert.build(contentSigner));
		} catch (CertIOException | OperatorCreationException | CertificateException e) {
			e.printStackTrace();
		}
		return null;
	}

	public X509Certificate issueCertificate(PKCS10CertificationRequest csr) {
		X500Name issuer = new X500Name(certificate.getSubjectDN().getName());
		X509v3CertificateBuilder builderCert = getBuilderCert(issuer, csr.getSubject(), csr.getSubjectPublicKeyInfo());
		try {
			ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRsa").build(keyPair.getPrivate());
			return new JcaX509CertificateConverter().getCertificate(builderCert.build(contentSigner));
		} catch (OperatorCreationException | CertificateException e) {
			e.printStackTrace();
		}
		return null;
	}

	private X509v3CertificateBuilder getBuilderCert(X500Name issuer, X500Name subject, SubjectPublicKeyInfo keyPublic) {
		BigInteger serialNo = new BigInteger(ByteBuffer.allocate(Long.BYTES).putLong(System.currentTimeMillis()).array());
		Date dateStart = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(dateStart);
		calendar.add(Calendar.YEAR, 1);
		Date dateEnd = calendar.getTime();
		return new X509v3CertificateBuilder(issuer, serialNo, dateStart, dateEnd, subject, keyPublic);
	}
}
