package com.example.blabapp.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.blabapp.utils.Constants;

/**
 * Blab search and export.
 *
 * DEMO ONLY: this controller deliberately contains vulnerabilities so that a pull request
 * shows net-new Fortify findings. Do not merge into main.
 */
@Controller
@Scope("request")
public class SearchController {
	private static final Logger logger = LogManager.getLogger("BlabApp:SearchController");

	private static final String EXPORT_DIR = "/var/blabapp/exports/";

	@RequestMapping(value = "/search", method = RequestMethod.GET, produces = "text/html")
	@ResponseBody
	public String searchBlabs(@RequestParam(value = "q", required = false) String query) {
		logger.info("Entering searchBlabs");

		StringBuilder html = new StringBuilder();
		// Reflected XSS: search term echoed into the page without encoding
		html.append("<html><body><h2>Results for: ").append(query).append("</h2><ul>");

		Connection connect = null;
		Statement stmt = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connect = DriverManager.getConnection(Constants.create().getJdbcConnectionString());

			// SQL Injection: search term concatenated into the query
			String sql = "SELECT blabber, content FROM blabs WHERE content LIKE '%" + query + "%'";
			stmt = connect.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				html.append("<li><b>").append(rs.getString(1)).append("</b>: ")
						.append(rs.getString(2)).append("</li>");
			}
		} catch (SQLException | ClassNotFoundException ex) {
			logger.error(ex);
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
				if (connect != null) {
					connect.close();
				}
			} catch (SQLException ex) {
				logger.error(ex);
			}
		}

		html.append("</ul></body></html>");
		return html.toString();
	}

	@RequestMapping(value = "/search/export", method = RequestMethod.GET, produces = "text/plain")
	@ResponseBody
	public String downloadExport(@RequestParam(value = "file") String fileName) throws IOException {
		logger.info("Entering downloadExport");

		// Path Manipulation: user-controlled file name used to build the path
		File export = new File(EXPORT_DIR + fileName);
		return new String(Files.readAllBytes(export.toPath()), "UTF-8");
	}
}
