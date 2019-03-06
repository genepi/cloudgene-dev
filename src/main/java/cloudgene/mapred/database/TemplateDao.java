package cloudgene.mapred.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloudgene.mapred.util.Template;
import genepi.db.Database;
import genepi.db.IRowMapper;
import genepi.db.JdbcDataAccessObject;

public class TemplateDao extends JdbcDataAccessObject {

	private static final Logger log = LoggerFactory.getLogger(TemplateDao.class);

	public TemplateDao(Database database) {
		super(database);
	}

	public boolean insert(Template snippet) {
		StringBuilder sql = new StringBuilder();
		sql.append("insert into html_snippets (`key`, text) ");
		sql.append("values (?,?)");

		try {

			Object[] params = new Object[2];
			params[0] = snippet.getKey();
			params[1] = snippet.getText();

			update(sql.toString(), params);

			log.debug("insert html snippet successful.");

		} catch (SQLException e) {
			log.error("insert  html snippet  failed.", e);
			return false;
		}

		return true;
	}

	public boolean update(Template snippet) {
		StringBuilder sql = new StringBuilder();
		sql.append("update html_snippets SET text = ? where `key` = ? ");

		try {

			Object[] params = new Object[2];
			params[0] = snippet.getText();
			params[1] = snippet.getKey();

			update(sql.toString(), params);

			log.debug("update html snippet successful.");

		} catch (SQLException e) {
			log.error("update  html snippet  failed.", e);
			return false;
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public List<Template> findAll() {

		StringBuilder sql = new StringBuilder();
		sql.append("select * ");
		sql.append("from html_snippets ");

		List<Template> result = new Vector<Template>();

		try {

			result = query(sql.toString(), new TemplateMapper());

			log.debug("find all html snippets successful. results: "
					+ result.size());

			return result;
		} catch (SQLException e) {
			log.error("find all html snippets failed", e);
			return null;
		}
	}

	public Template findByKey(String key) {

		StringBuffer sql = new StringBuffer();

		sql.append("select * ");
		sql.append("from html_snippets ");
		sql.append("where `key` = ?");

		Object[] params = new Object[1];
		params[0] = key;

		Template result = null;

		try {
			result = (Template) queryForObject(sql.toString(), params,
					new TemplateMapper());

			log.debug("find html snippet by key '" + key + "' successful.");

		} catch (SQLException e1) {

			log.error("find html snippet by key '" + key + "'  failed.", e1);

		}
		return result;
	}

	class TemplateMapper implements IRowMapper {

		@Override
		public Object mapRow(ResultSet rs, int row) throws SQLException {
			return new Template(rs.getString("key"), rs.getString("text"));
		}

	}

}
