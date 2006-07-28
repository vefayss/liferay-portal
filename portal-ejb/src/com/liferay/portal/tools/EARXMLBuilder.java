/**
 * Copyright (c) 2000-2006 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.tools;

import com.liferay.portal.util.EntityResolver;
import com.liferay.util.FileUtil;

import java.io.File;
import java.io.IOException;

import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * <a href="EARXMLBuilder.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 *
 */
public class EARXMLBuilder {

	public static String[] EJB_PATHS = {
		"../counter-ejb", "../documentlibrary-ejb", "../lock-ejb",
		"../mail-ejb", "../portal-ejb"
	};

	public static String[] WEB_PATHS = {
		"../cms-web", "../laszlo-web", "../portal-web-complete", "../tunnel-web"
	};

	public static void main(String[] args) {
		new EARXMLBuilder();
	}

	public EARXMLBuilder() {
		try {
			_buildGeronimoXML();
			_buildPramatiXML();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void _buildGeronimoXML() throws Exception {
		StringBuffer sb = new StringBuffer();

		SAXReader reader = new SAXReader();

		reader.setEntityResolver(new EntityResolver());

		Document doc = reader.read(new File("classes/META-INF/ejb-jar.xml"));

		Iterator itr = doc.getRootElement().element("enterprise-beans").elements("session").iterator();

		while (itr.hasNext()) {
			Element entity = (Element)itr.next();

			String displayName = entity.elementText("display-name");

			sb.append("\t\t\t\t<session>\n");
			sb.append("\t\t\t\t\t<ejb-name>").append(entity.elementText("ejb-name")).append("</ejb-name>\n");

			if (displayName.endsWith("LocalServiceEJB")) {
				sb.append("\t\t\t\t\t<jndi-name>ejb/liferay/").append(displayName.substring(0, displayName.length() - 3)).append("Home</jndi-name>\n");
			}
			else {
				sb.append("\t\t\t\t\t<jndi-name>").append(entity.elementText("ejb-name")).append("</jndi-name>\n");
			}

			sb.append("\t\t\t\t\t<resource-ref>\n");
			sb.append("\t\t\t\t\t\t<ref-name>jdbc/LiferayPool</ref-name>\n");
			sb.append("\t\t\t\t\t\t<resource-link>LiferayPool</resource-link>\n");
			sb.append("\t\t\t\t\t</resource-ref>\n");
			sb.append("\t\t\t\t\t<resource-ref>\n");
			sb.append("\t\t\t\t\t\t<ref-name>mail/MailSession</ref-name>\n");
			sb.append("\t\t\t\t\t\t<resource-link>LiferayMailSession</resource-link>\n");
			sb.append("\t\t\t\t\t</resource-ref>\n");
			sb.append("\t\t\t\t</session>\n");
		}

		// geronimo-application.xml

		File outputFile = new File("../portal-ear/modules/META-INF/geronimo-application.xml");

		String content = FileUtil.read(outputFile);
		String newContent = content;

		int x = content.indexOf("portal-ejb.jar");

		x = content.indexOf("<enterprise-beans>", x) + 20;

		int y = content.indexOf("</enterprise-beans>", x) - 3;

		newContent =
			content.substring(0, x - 1) + sb.toString() +
				content.substring(y, content.length());

		if (!content.equals(newContent)) {
			FileUtil.write(outputFile, newContent);

			System.out.println(outputFile.toString());
		}
	}

	private void _buildPramatiXML() throws Exception {

		// pramati-j2ee-server.xml

		StringBuffer sb = new StringBuffer();

		sb.append("<?xml version=\"1.0\"?>\n");
		sb.append("<!DOCTYPE pramati-j2ee-server PUBLIC \"-//Pramati Technologies //DTD Pramati J2ee Server 3.5 SP5//EN\" \"http://www.pramati.com/dtd/pramati-j2ee-server_3_5.dtd\">\n");

		sb.append("\n<pramati-j2ee-server>\n");
		sb.append("\t<vhost-name>default</vhost-name>\n");
		sb.append("\t<auto-start>TRUE</auto-start>\n");
		sb.append("\t<realm-name>PortalRealm</realm-name>\n");

		for (int i = 0; i < EJB_PATHS.length; i++) {
			sb.append(_buildPramatiXMLEJBModule(EJB_PATHS[i]));
		}

		for (int i = 0; i < WEB_PATHS.length; i++) {
			sb.append(_buildPramatiXMLWebModule(WEB_PATHS[i]));
		}

		for (int i = 0; i < EJB_PATHS.length; i++) {
			sb.append(_buildPramatiXMLRoleMapping(EJB_PATHS[i], "jar"));
		}

		for (int i = 0; i < WEB_PATHS.length; i++) {
			sb.append(_buildPramatiXMLRoleMapping(WEB_PATHS[i], "war"));
		}

		sb.append("</pramati-j2ee-server>");

		File outputFile = new File("../portal-ear/modules/pramati-j2ee-server.xml");

		if (!outputFile.exists() ||
			!FileUtil.read(outputFile).equals(sb.toString())) {

			FileUtil.write(outputFile, sb.toString());

			System.out.println(outputFile.toString());
		}
	}

	private String _buildPramatiXMLEJBJar(String path)
		throws IOException {

		File file = new File(path + "/classes/pramati-or-map.xml");

		if (file.exists()) {
			String content = FileUtil.read(file);

			int x = content.indexOf("<ejb-jar>");
			int y = content.indexOf("</ejb-jar>");

			if (x != -1 && y != -1) {
				return content.substring(x - 1, y + 11);
			}
		}

		StringBuffer sb = new StringBuffer();

		sb.append("\t<ejb-jar>\n");
		sb.append("\t\t<jar-name>").append(path.substring(3, path.length())).append(".jar</jar-name>\n");
		sb.append("\t</ejb-jar>\n");

		return sb.toString();
	}

	private String _buildPramatiXMLEJBModule(String path)
		throws IOException {

		File file = new File(path + "/classes/pramati-j2ee-server.xml");

		if (file.exists()) {
			String content = FileUtil.read(file);

			int x = content.indexOf("<ejb-module>");
			int y = content.indexOf("</ejb-module>");

			if (x != -1 && y != -1) {
				return content.substring(x - 1, y + 14);
			}
		}

		return "";
	}

	private String _buildPramatiXMLRoleMapping(String path, String extension)
		throws IOException {

		StringBuffer sb = new StringBuffer();

		sb.append("\t<role-mapping>\n");
		sb.append("\t\t<module-name>").append(path.substring(3, path.length())).append(".").append(extension).append("</module-name>\n");
		sb.append("\t\t<role-name>users</role-name>\n");
		sb.append("\t\t<role-link>everybody</role-link>\n");
		sb.append("\t</role-mapping>\n");

		return sb.toString();
	}

	private String _buildPramatiXMLWebModule(String path)
		throws DocumentException, IOException {

		String contextRoot = path.substring(2, path.length() - 4);
		String filePath = path + "/docroot/WEB-INF/web.xml";

		if (path.endsWith("-complete")) {
			contextRoot = "/";
			filePath =
				path.substring(0, path.length() - 9) +
				"/docroot/WEB-INF/web.xml";
		}

		StringBuffer sb = new StringBuffer();

		sb.append("\t<web-module>\n");
		sb.append("\t\t<name>").append(contextRoot).append("</name>\n");

		sb.append("\t\t<module-name>");

		sb.append(path.substring(3, path.length())).append(".war</module-name>\n");

		SAXReader reader = new SAXReader();

		reader.setEntityResolver(new EntityResolver());

		Document doc = reader.read(new File(filePath));

		Iterator itr = doc.getRootElement().elements("ejb-local-ref").iterator();

		while (itr.hasNext()) {
			Element ejbLocalRef = (Element)itr.next();

			sb.append("\t\t<ejb-local-ref>\n");
			sb.append("\t\t\t<ejb-ref-name>").append(ejbLocalRef.elementText("ejb-ref-name")).append("</ejb-ref-name>\n");
			sb.append("\t\t\t<ejb-link>").append(ejbLocalRef.elementText("ejb-link")).append("</ejb-link>\n");
			sb.append("\t\t</ejb-local-ref>\n");
		}

		itr = doc.getRootElement().elements("resource-ref").iterator();

		while (itr.hasNext()) {
			Element resourceRef = (Element)itr.next();

			sb.append("\t\t<resource-mapping>\n");
			sb.append("\t\t\t<resource-name>").append(resourceRef.elementText("res-ref-name")).append("</resource-name>\n");
			sb.append("\t\t\t<resource-type>").append(resourceRef.elementText("res-type")).append("</resource-type>\n");
			sb.append("\t\t\t<resource-link>").append(resourceRef.elementText("res-ref-name")).append("</resource-link>\n");
			sb.append("\t\t</resource-mapping>\n");
		}

		sb.append("\t</web-module>\n");

		return sb.toString();
	}

}