<%--
 Copyright 2004-2005 Sun Microsystems, Inc.  All rights reserved.
 Use is subject to license terms.
--%>

<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<fmt:setBundle basename="LocalStrings"/>

<html>
<body>
<!--
  Copyright (c) 1999 The Apache Software Foundation.  All rights 
  reserved.
-->

<jsp:useBean id="main5" scope="session" class="roart.beans.session.control.Main" />

<jsp:setProperty name="main5" property="*" />
<jsp:directive.page import="java.util.List"/>
<jsp:directive.page import="java.util.ArrayList"/>
<jsp:directive.page import="java.util.TreeMap"/>
<!--%
	main.processRequest(request);
%-->

<%
	//HttpSession session = request.getSession();
	String filesystemlucene = request.getParameter("filesystemlucene");
	String lucene = request.getParameter("lucene");
	String filesystem = request.getParameter("filesystem");
	String luceneadd = request.getParameter("luceneadd");
	String lucenereindex = request.getParameter("lucenereindex");
	String notindexed = request.getParameter("notindexed");
	String filesystemadd = request.getParameter("filesystemadd");
	String filesystemluceneadd = request.getParameter("filesystemluceneadd");
	String cleanup = request.getParameter("cleanup");
	String cleanup2 = request.getParameter("cleanup2");
	String cleanupfs = request.getParameter("cleanupfs");
	String memoryusage = request.getParameter("memoryusage");
	List<String> strarr = null;
	if (filesystemlucene != null) {
 	   strarr = main5.filesystemlucene();
	}
	if (filesystem != null) {
 	   strarr = main5.traverse();
	}
	if (lucene != null) {
 	   strarr = main5.index();
	}
	if (notindexed != null) {
 	   strarr = main5.notindexed();
	}
	if (filesystemluceneadd != null) {
 	   strarr = main5.filesystemlucene(filesystemluceneadd);
	}
	if (filesystemadd != null) {
 	   strarr = main5.traverse(filesystemadd);
	}
	if (luceneadd != null) {
 	   strarr = main5.index(luceneadd, false);
	}
	if (lucenereindex != null) {
 	   strarr = main5.index(lucenereindex, true);
	}
	if (cleanup != null) {
 	   strarr = main5.cleanup();
	}
	if (cleanup2 != null) {
 	   strarr = main5.cleanup2();
	}
	if (cleanupfs != null) {
 	   strarr = main5.cleanupfs(cleanupfs);
	}
	if (memoryusage != null) {
 	   strarr = main5.memoryusage();
	}
  	   for (int i=0; i<strarr.size(); i++) {
	     String str = strarr.get(i);
%>
<%= str %>
<br/>
<%
          }
%>
<h1>finished, or something</h1>
<br/>

</body>
</html>
