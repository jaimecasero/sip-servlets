package org.mobicents.xdm.server.appusage.oma.userprofile;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;
import javax.naming.NamingException;
import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;

import junit.framework.JUnit4TestAdapter;

import org.junit.Test;
import org.mobicents.xcap.client.XcapClient;
import org.mobicents.xcap.client.XcapConstant;
import org.mobicents.xcap.client.XcapResponse;
import org.mobicents.xcap.client.header.Header;
import org.mobicents.xcap.client.impl.XcapClientImpl;
import org.mobicents.xcap.client.uri.DocumentSelectorBuilder;
import org.mobicents.xcap.client.uri.UriBuilder;
import org.openxdm.xcap.common.error.InternalServerErrorException;
import org.openxdm.xcap.common.error.NotUTF8ConflictException;
import org.openxdm.xcap.common.error.NotWellFormedConflictException;
import org.openxdm.xcap.common.xml.TextWriter;
import org.openxdm.xcap.common.xml.XMLValidator;

public class ConstraintsFailureTest extends AbstractT {
	
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(ConstraintsFailureTest.class);
	}
	
	private String user = "sip:bob@example.com";
	private String documentName = "user-profile";
	
	@Test
	public void test() throws IOException, JAXBException, InterruptedException, TransformerException, NotWellFormedConflictException, NotUTF8ConflictException, InternalServerErrorException, InstanceNotFoundException, MBeanException, ReflectionException, URISyntaxException, MalformedObjectNameException, NullPointerException, NamingException {
		
		initRmiAdaptor();

		try {
			createUser(user,user);
		}
		catch (RuntimeMBeanException e) {
			if (!(e.getCause() instanceof IllegalStateException)) {
				e.printStackTrace();
			}
		}
		
		XcapClient client = new XcapClientImpl();
		
		Header[] headers = new Header[1];
		headers[0] = client.getHeaderFactory().getBasicHeader(XcapConstant.HEADER_X_3GPP_Asserted_Identity,user);
			
		
		// create uri to put rls-services doc		
		String documentSelector = DocumentSelectorBuilder.getUserDocumentSelectorBuilder(OMAUserProfileAppUsage.ID,user,documentName).toPercentEncodedString();
		UriBuilder uriBuilder = new UriBuilder()
			.setSchemeAndAuthority("http://localhost:8080")
			.setXcapRoot("/mobicents")
			.setDocumentSelector(documentSelector);
		URI documentURI = uriBuilder.toURI();
		
		// read documents xml
		InputStream is = this.getClass().getResourceAsStream("user-profile.xml");
        String content = TextWriter.toString(XMLValidator.getWellFormedDocument(XMLValidator.getUTF8Reader(is)));
		is.close();
		
		// send put request for a doc with user-profile uri != xui and get response
		XcapResponse putResponse = client.put(documentURI,OMAUserProfileAppUsage.MIMETYPE,content,headers,null);
		System.out.println("Response got:\n"+putResponse);
		assertTrue("Put response must exists",putResponse != null);
		assertTrue("Put response code should be 409",putResponse.getCode() == 409);
		
		client.shutdown();
		
		removeUser(user);
		
	}
		
}