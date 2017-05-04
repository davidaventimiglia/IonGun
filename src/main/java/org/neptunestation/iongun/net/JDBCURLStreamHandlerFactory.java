package org.neptunestation.iongun.util;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import javax.sql.*;
import javax.sql.rowset.*;
import org.neptunestation.iongun.util.*;

public class JDBCURLStreamHandlerFactory implements URLStreamHandlerFactory {
    public static final String ACCEPT = "Accept";
    public static final String FS = "FS";
    public static final String GS = "GS";
    public static final String RS = "RS";
    public static final String US = "US";

    List<JDBCURLStreamHandler>
	streamHandlers = new ArrayList<>();

    List<QueryHandler>
	queryHandlers = new ArrayList<>();

    public JDBCURLStreamHandlerFactory () {
	queryHandlers.add(new DefaultQueryHandler());
	queryHandlers.add(new ShowTablesHandler());
	streamHandlers.add(new DefaultURLStreamHandler() {
		@Override
		public boolean accepts (String protocol) {return "sql".equals(protocol);}});
	streamHandlers.add(new DefaultURLStreamHandler() {
		@Override
		public boolean accepts (String protocol) {return "sqlite".equals(protocol);}
		@Override
		protected URLConnection openConnection (final URL u) throws IOException {
		    return (new URL(String.format("jdbc:sqlite:%s?%s", u.getPath(), u.getQuery()))).openConnection();}});
	streamHandlers.add(new DefaultURLStreamHandler() {
		@Override
		public boolean accepts (String protocol) {return "sqlite2".equals(protocol);}
		@Override
		protected URLConnection openConnection (final URL u) throws IOException {
		    return (new URL(String.format("sqlite:%s", schemeSpecificPart))).openConnection();}});
	streamHandlers.add(new DefaultURLStreamHandler() {
		@Override
		public boolean accepts (String protocol) {return "sqlite3".equals(protocol);}
		@Override
		protected URLConnection openConnection (final URL u) throws IOException {
		    return (new URL(String.format("sqlite:%s", schemeSpecificPart))).openConnection();}});
	streamHandlers.add(new DefaultURLStreamHandler() {
		String subname;
		@Override
		public boolean accepts (String protocol) {return "jdbc".equals(protocol);}
		@Override
		protected void parseURL (final URL u, final String spec, final int start, final int end) {
		    int delimiter = spec.indexOf(":", start);
		    subname = spec.substring(start, delimiter);
		    super.parseURL(u, spec, delimiter+1, end);}
		@Override
		protected URLConnection openConnection (final URL u) {
		    return new URLConnection (u) {
			Map<String, List<String>> properties;
			@Override
			public String getContentType () {
			    for (String s : properties.get(ACCEPT)) return s;
			    return "text/csv";}
			@Override
			public synchronized void connect () {
			    properties = getRequestProperties();
			    try (Connection c = getConnection(u, subname)) {connected = true;}
			    catch (Exception e) {throw new RuntimeException(e);}}
			@Override
			public InputStream getInputStream () throws IOException {
			    if (!connected) connect();
			    final PipedInputStream in = new PipedInputStream();
			    final PrintStream out = new PrintStream(new PipedOutputStream(in));
			    Thread t = new Thread(()->{
				    try (Connection c = getConnection(u, subname)) {
					for (QueryHandler qh : queryHandlers)
					    if (qh.accepts(u.getQuery())) {
						qh.handle(c, u.getQuery(), ResultSetHandlerFactory.createResultSetHandler(getContentType(), properties), out);
						break;}
					out.close();}
				    catch (Exception e) {throw new RuntimeException(e);}});
			    t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
				    public void uncaughtException (Thread th, Throwable ex) {
					out.close();}});
			    t.start();
			    return in;}};}});}

    public String getUrl (final URL u, final String subname) {
	return String.format("%s:%s:%s%s", "jdbc", subname,
			     u.getHost().equals("") ? "" :
			     u.getPort()<0 ? String.format("//%s", u.getHost()) :
			     String.format("//%s:%s", u.getHost(), u.getPort()),
			     u.getPath());}

    public Connection getConnection (final URL u, final String subname) throws SQLException {
	return u.getUserInfo()==null ?
	    DriverManager.getConnection(getUrl(u, subname)) :
	    DriverManager.getConnection(getUrl(u, subname), u.getUserInfo().split(":")[0], u.getUserInfo().split(":")[1]);}

    @Override
    public URLStreamHandler createURLStreamHandler (final String protocol) {
	for (JDBCURLStreamHandler sh : streamHandlers)
	    if (sh.accepts(protocol)) return sh;
	return null;}}
