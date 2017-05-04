package org.neptunestation.iongun.plugins;

import java.io.*;
import java.sql.*;
import java.util.*;
import javax.sql.*;
import javax.sql.rowset.*;
import org.neptunestation.iongun.sql.*;
import org.neptunestation.iongun.util.*;

public class WebXMLResultSetHandler extends AbstractResultSetHandler {
    @Override
    public boolean accepts (String mimeType) {
	return "text/web-xml".equalsIgnoreCase(mimeType);}

    @Override
    public void print (ResultSet r, PrintStream out) throws IOException, SQLException {
	RowSetProvider.newFactory().createWebRowSet().writeXml(r, out);}}
