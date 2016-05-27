package com.viewer.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.viewer.dao.AlbumDAO;
import com.viewer.dao.SQLConnector;
import com.viewer.dto.AlbumDTO;
import com.viewer.dto.AlbumTagsDTO;
import com.viewer.dto.CategoryDTO;
import com.viewer.dto.PhotoDTO;
import com.viewer.dto.SearchQueryDTO;

public class SQLAlbumDAO implements AlbumDAO {

	/** Album Fetch Methods **/

	public List<AlbumDTO> fetchAllPublicAlbums(int limit, int offset) throws SQLException {
		List<AlbumDTO> dto = new ArrayList<AlbumDTO>();
		Connection conn = SQLConnector.connect();

		// Create Statement
		String sql = "SELECT Albums.id, Albums.owner, Albums.name, Albums.subtitle FROM Albums"
				+ " WHERE Albums.permission = 'PUBLIC' AND Albums.parent = 0 GROUP BY Albums.id LIMIT " + limit + " OFFSET "
				+ offset;
		PreparedStatement stmt = conn.prepareStatement(sql);

		// Execute Statement
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			AlbumDTO album = new AlbumDTO(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), 0);
			dto.add(album);
		}
		stmt.close();

		// conn.close();
		return dto;
	}

	@Override
	public List<AlbumDTO> fetchViewableAlbums(long userid, long parentId) throws SQLException {
		List<AlbumDTO> dto = new ArrayList<AlbumDTO>();
		Connection conn = SQLConnector.connect();

		// Create Statement
		String selectViewable = "SELECT Albums.id, Albums.owner, Albums.name, Albums.subtitle FROM AlbumAccess"
				+ " LEFT JOIN Albums ON Albums.owner = AlbumAccess.owner AND Albums.id = AlbumAccess.albumid"
				+ " WHERE (AlbumAccess.visitor = ? OR Albums.permission = 'PUBLIC') AND Albums.parent = ?";
		PreparedStatement stmt = conn.prepareStatement(selectViewable);
		stmt.setLong(1, userid);
		stmt.setLong(2, parentId);

		// Execute Statement
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			AlbumDTO album = new AlbumDTO(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), 0);
			dto.add(album);
		}
		// conn.close();
		return dto;
	}

	@Override
	public List<AlbumDTO> fetchAllSubscribedAlbums(long userid, long parentId) throws SQLException {
		List<AlbumDTO> dto = new ArrayList<AlbumDTO>();
		Connection conn = SQLConnector.connect();

		// Create Statement
		String selectSubscribed = "SELECT Albums.id, Albums.owner, Albums.name, Albums.subtitle FROM AlbumAccess"
				+ " LEFT JOIN Albums ON Albums.owner = AlbumAccess.owner AND Albums.id = AlbumAccess.albumid"
				+ " LEFT JOIN UserSubscriptions ON AlbumAccess.visitor = UserSubscriptions.uid AND Albums.id = UserSubscriptions.albumid"
				+ " WHERE (AlbumAccess.visitor = ? OR Albums.permission = 'PUBLIC') AND Albums.parent = ?";
		PreparedStatement stmt = conn.prepareStatement(selectSubscribed);
		stmt.setLong(1, userid);
		stmt.setLong(2, parentId);

		// Execute Statement
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			AlbumDTO album = new AlbumDTO(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), parentId);
			dto.add(album);
		}
		// conn.close();
		return dto;
	}

	@Override
	public List<AlbumDTO> fetchSearchUserAlbums(long userid, SearchQueryDTO searchQuery) throws SQLException {
		List<AlbumDTO> dto = new ArrayList<AlbumDTO>();
		Connection conn = SQLConnector.connect();

		List<String> names = searchQuery.getNames();
		List<CategoryDTO> categories = searchQuery.getAllTags();

		// Create Statement
		String sql = "SELECT Albums.id, Albums.owner, Albums.name, Albums.subtitle FROM AlbumAccess"
				+ " LEFT JOIN Albums ON Albums.owner = AlbumAccess.owner AND Albums.id = AlbumAccess.albumid"
				+ " WHERE (AlbumAccess.visitor = ? OR Albums.permission = 'PUBLIC') AND Albums.parent = ?";
		for (int i = 0; i < names.size(); i++) {
			sql += " AND Albums.name LIKE ?";
		}
		for (CategoryDTO category : categories) {
			sql += " AND (TagCategory.name = ?";
			for (int i = 0; i < category.getTags().size(); i++) {
				sql += " AND AlbumTags.name = ?";
			}
			sql += ")";
		}
		PreparedStatement stmt = conn.prepareStatement(sql);

		// Set Statement
		stmt.setLong(1, userid);
		int index = 2;
		for (String name : names) {
			stmt.setString(index, '%' + name + '%');
			index++;
		}
		for (CategoryDTO category : categories) {
			stmt.setString(index, category.getCategory());
			index++;
			for (String tag : category.getTags()) {
				stmt.setString(index, tag);
				index++;
			}
		}

		// Execute Statement
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			AlbumDTO album = new AlbumDTO(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getLong(7));
			dto.add(album);
		}
		// conn.close();
		return dto;
	}

	@Override
	public AlbumDTO fetchUserAlbumInfo(long userid, long albumid) throws SQLException {
		Connection conn = SQLConnector.connect();

		// Create Statement
		String sql = "SELECT Albums.id, Albums.owner, Albums.name, Albums.subtitle, Albums.parent FROM AlbumAccess"
				+ " LEFT JOIN Albums ON Albums.owner = AlbumAccess.owner AND Albums.id = AlbumAccess.albumid"
				+ " WHERE (AlbumAccess.visitor = ? OR Albums.permission = 'PUBLIC') AND Albums.id = ?";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setLong(1, userid);

		// Execute Statement
		ResultSet rs = stmt.executeQuery();
		AlbumDTO album = null;
		if (rs.next()) {
			album = new AlbumDTO(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getLong(5));
		}
		// conn.close();
		return album;
	}

	/** Album Access Methods **/

	@Override
	public boolean subscribeToAlbum(long userid, long albumId) throws SQLException {
		Connection conn = SQLConnector.connect();

		// Create Statement
		String sql = "INSERT OR IGNORE INTO UserSubscriptions VALUES (NULL, ?, ?, ?)";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setLong(1, userid);
		stmt.setLong(2, albumId);
		stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));

		// Execute Statement
		int result = stmt.executeUpdate();
		stmt.close();
		// conn.close();
		return result > 0;
	}

	@Override
	public boolean unsubscribeToAlbum(long userid, long albumId) throws SQLException {
		Connection conn = SQLConnector.connect();

		// Create Statement
		String sql = "DELETE FROM UserSubscriptions WHERE UserSubscriptions.albumid IN ("
				+ "SELECT UserSubscriptions.albumid FROM UserSubscriptions"
				+ " WHERE UserSubscriptions.uid = ? AND UserSubscriptions.albumid = ?" + ")";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setLong(1, userid);
		stmt.setLong(2, albumId);

		// Execute Statement
		int result = stmt.executeUpdate();
		stmt.close();
		// conn.close();
		return result > 0;
	}

	@Override
	public void addPermissionToAlbum(long ownerid, long albumId, String user) throws SQLException {
		Connection conn = SQLConnector.connect();

		// Create Statement
		String sql = "INSERT INTO AlbumAccess VALUES (NULL, ?, ?, (SELECT Users.uid FROM Users WHERE Users.username = ?))";
		PreparedStatement stmt = conn.prepareStatement(sql);

		stmt.setLong(1, albumId);
		stmt.setLong(2, ownerid);
		stmt.setString(3, user);
		stmt.executeUpdate();
		stmt.close();
	}

	@Override
	public void addPermissionToAlbum(long ownerid, long albumId, List<String> users) throws SQLException {
		if (users.isEmpty()) return;
		Connection conn = SQLConnector.connect();

		// Create Statement
		String sql = "INSERT INTO AlbumAccess VALUES (NULL, ?, ?, (SELECT Users.uid FROM Users WHERE Users.username = ?))";
		PreparedStatement stmt = conn.prepareStatement(sql);

		for (String user : users) {
			stmt.setLong(1, albumId);
			stmt.setLong(2, ownerid);
			stmt.setString(3, user);
			stmt.addBatch();
		}
		stmt.executeBatch();
		stmt.close();
	}

	@Override
	public void revokePermissionToAlbum(long ownerid, long albumId, List<String> users) throws SQLException {
		if (users.isEmpty()) return;
		Connection conn = SQLConnector.connect();

		// Create Statement
		String sql = "DELETE FROM AlbumAccess WHERE albumid = ? AND owner = ? AND visitor IN (SELECT Users.uid FROM Users WHERE Users.username = ?)";
		PreparedStatement stmt = conn.prepareStatement(sql);

		for (String user : users) {
			stmt.setLong(1, albumId);
			stmt.setLong(2, ownerid);
			stmt.setString(3, user);
			stmt.addBatch();
		}
		stmt.executeBatch();
		stmt.close();
	}

	@Override
	public long albumExist(long userid, String name, long parentId) throws SQLException {
		Connection conn = SQLConnector.connect();
		long albumId = -1;

		// Create Statement
		String sql = "SELECT id FROM Albums WHERE Albums.owner = ? AND Albums.name = ? AND Albums.parent = ?";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setLong(1, userid);
		stmt.setString(2, name);
		stmt.setLong(3, parentId);

		// Execute Statement
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			albumId = rs.getInt(1);
		}
		// conn.close();
		return albumId;
	}

	/** Create Albums **/

	@Override
	public long createAlbum(long userid, String name, String subtitle, String description, String permission)
			throws SQLException {
		Connection conn = SQLConnector.connect();

		String sql = "INSERT INTO Albums VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?)";
		PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
		stmt.setString(1, name);
		stmt.setString(2, subtitle);
		stmt.setLong(3, userid);
		stmt.setLong(4, 0); // Coverid
		stmt.setLong(5, 0); // ParentId
		stmt.setString(6, description);
		stmt.setString(7, permission);
		stmt.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
		int albumid = stmt.executeUpdate();
		ResultSet rs = stmt.getGeneratedKeys();
		if (rs.next()) {
			albumid = rs.getInt(1);
		}
		stmt.close();
		addOwnerPermissionToAlbum(userid, albumid, conn);
		// conn.close();
		return albumid;
	}

	@Override
	public long createAlbum(long userid, String name, String subtitle, String description, long parentId) throws SQLException {
		Connection conn = SQLConnector.connect();

		String sql = "INSERT INTO Albums VALUES (NULL, ?, ?, ?, ?, ?, ?, (SELECT Albums.permission FROM Albums WHERE id = ?), ?)";
		PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
		stmt.setString(1, name);
		stmt.setString(2, subtitle);
		stmt.setLong(3, userid);
		stmt.setLong(4, 0); // Coverid
		stmt.setLong(5, parentId);
		stmt.setString(6, description);
		stmt.setLong(7, parentId);
		stmt.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
		int albumid = stmt.executeUpdate();
		ResultSet rs = stmt.getGeneratedKeys();
		if (rs.next()) {
			albumid = rs.getInt(1);
		}
		stmt.close();

		// Add Permission
		addOwnerPermissionToAlbum(userid, albumid, conn);

		// conn.close();
		return albumid;
	}

	private void addOwnerPermissionToAlbum(long userid, long albumid, Connection conn) throws SQLException {
		// Create Statement
		String sql = "INSERT INTO AlbumAccess VALUES (NULL, ?, ?, ?)";
		PreparedStatement stmt = conn.prepareStatement(sql);

		stmt.setLong(1, albumid);
		stmt.setLong(2, userid);
		stmt.setLong(3, userid);
		stmt.executeUpdate();
		stmt.close();
	}

	@Override
	public void setAlbumCoverPhoto(long userid, long albumid, long photoid) throws SQLException {
		Connection conn = SQLConnector.connect();

		String sql = "UPDATE Albums SET coverid = ? WHERE id = ? AND owner = ?";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setLong(1, photoid);
		stmt.setLong(2, albumid);
		stmt.setLong(3, userid);
		stmt.executeUpdate();
		// conn.close();
	}

	@Override
	public boolean deleteAlbum(long userid, long albumId) throws SQLException {
		Connection conn = SQLConnector.connect();

		String deletePhotos = "DELETE FROM Photos WHERE Photos.AlbumId = ? AND EXISTS (SELECT Users.uid FROM Users WHERE Users.uid = ?)";

		PreparedStatement stmt = conn.prepareStatement(deletePhotos);
		stmt.setLong(1, albumId);
		stmt.setLong(2, userid);
		int row = stmt.executeUpdate();
		stmt.close();

		String deleteAlbum = "DELETE FROM Albums WHERE Albums.id = ? AND EXISTS (SELECT Users.uid FROM Users WHERE Users.uid = ?)";
		PreparedStatement stmt2 = conn.prepareStatement(deleteAlbum);
		stmt2.setLong(1, albumId);
		stmt2.setLong(2, userid);
		row = stmt2.executeUpdate();
		stmt2.close();

		if (row > 0) {
			String deleteAlbumAccess = "DELETE FROM AlbumAccess WHERE AlbumAccess.albumid = ?";
			PreparedStatement stmt3 = conn.prepareStatement(deleteAlbumAccess);
			stmt3.setLong(1, albumId);
			stmt3.executeUpdate();
			stmt3.close();
		}

		// conn.close();
		return true;
	}

	/** Photo Methods **/

	@Override
	public List<PhotoDTO> fetchUserAlbumPhotos(long userid, long albumid) throws SQLException {
		List<PhotoDTO> photos = new ArrayList<PhotoDTO>();
		Connection conn = SQLConnector.connect();

		// Create Statement
		String selectPhotos = "SELECT Photos.id, Photos.name, Albums.id FROM Photos"
				+ " LEFT JOIN Albums ON Photos.albumid = Albums.id"
				+ " LEFT JOIN AlbumAccess ON AlbumAccess.albumid = Albums.id AND AlbumAccess.owner = Albums.owner"
				+ " WHERE (AlbumAccess.visitor = ? OR Albums.permission = 'PUBLIC') AND Photos.albumid = ? ORDER BY Photos.name";
		PreparedStatement stmt = conn.prepareStatement(selectPhotos);
		stmt.setLong(1, userid);
		stmt.setLong(2, albumid);

		// Execute Statement
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			photos.add(new PhotoDTO(rs.getLong(1), rs.getString(2), rs.getString(3), albumid, userid));
		}
		// conn.close();
		return photos;
	}

	public PhotoDTO fetchAlbumCoverPhoto(long userid, long albumid) throws SQLException {
		Connection conn = SQLConnector.connect();

		// Create Statement
		String selectPhotos = "SELECT MIN(Photos.id), Photos.name, Albums.id, Photos.ext FROM Photos"
				+ " LEFT JOIN Albums ON Photos.albumid = Albums.id"
				+ " LEFT JOIN AlbumAccess ON AlbumAccess.albumid = Albums.id AND AlbumAccess.visitor = Users.uid"
				+ " WHERE (AlbumAccess.visitor = ? OR Albums.permission = 'PUBLIC') AND Photos.albumid = ? " + " GROUP BY AlbumId"
				+ " ORDER BY Photos.name";
		PreparedStatement stmt = conn.prepareStatement(selectPhotos);
		stmt.setLong(1, userid);
		stmt.setLong(2, albumid);

		// Execute Statement
		PhotoDTO photo = null;
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			photo = new PhotoDTO(rs.getLong(1), rs.getString(2), rs.getString(3), albumid, userid);
		}
		// conn.close();
		return photo;
	}

	@Override
	public PhotoDTO fetchPhoto(long photoid) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/** Category & Tag Methods **/

	@Override
	public boolean tagAlbum(long userid, String name, long albumid, String category) throws SQLException {
		Connection conn = SQLConnector.connect();
		String sql = "INSERT INTO AlbumTags VALUES (NULL, ?, (SELECT TagCategory.id FROM TagCategory WHERE TagCategory.name = ?), ?)";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setString(1, name);
		stmt.setString(2, category);
		stmt.setLong(3, albumid);
		stmt.executeUpdate();
		// conn.close();
		return true;
	}

	@Override
	public boolean clearAlbumTag(long albumid) throws SQLException {
		Connection conn = SQLConnector.connect();
		String sql = "DELETE FROM AlbumTags WHERE albumid = ?";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setLong(1, albumid);
		stmt.executeUpdate();
		// conn.close();
		return true;
	}

	@Override
	public AlbumTagsDTO fetchUserAlbumTags(long userid, long albumid) throws SQLException {
		Connection conn = SQLConnector.connect();

		// Create Statement
		String sql = "SELECT AlbumTags.id, AlbumTags.name, TagCategory.name FROM AlbumTags"
				+ " INNER JOIN TagCategory ON AlbumTags.cateid = Category.id"
				+ " LEFT JOIN Albums ON Albums.id = AlbumTags.albumid"
				+ " WHERE Albums.owner = ? AND AlbumTags.albumid = ?";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setLong(1, userid);
		stmt.setLong(2, albumid);

		// Execute Statement
		ResultSet rs = stmt.executeQuery();
		AlbumTagsDTO tags = new AlbumTagsDTO(albumid);
		while (rs.next()) {
			tags.insertTag(rs.getLong(1), rs.getString(2), rs.getString(3));
		}
		stmt.close();
		// conn.close();
		return tags;
	}

	@Override
	public int createCategory(long userid, String name) throws SQLException {
		Connection conn = SQLConnector.connect();
		String sql = "INSERT OR IGNORE INTO TagCategory VALUES (NULL, ?)";
		PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
		stmt.setString(1, name);
		int id = stmt.executeUpdate();
		ResultSet rs = stmt.getGeneratedKeys();
		if (rs.next()) {
			id = rs.getInt(1);
		}
		stmt.close();
		// conn.close();
		return id;
	}

	@Override
	public List<String> fetchAllCategories(String username) throws SQLException {
		Connection conn = SQLConnector.connect();

		// Create Statement
		String sql = "SELECT name FROM TagCategory WHERE visibility = ? OR visibility = 'FULL'";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setString(1, username);

		// Execute Statement
		ResultSet rs = stmt.executeQuery();
		List<String> categories = new ArrayList<String>();
		while (rs.next())
			categories.add(rs.getString(1));
		stmt.close();
		// conn.close();
		return categories;
	}

	@Override
	public PhotoDTO insertPhoto(long userid, long albumId, String name, String ext) throws SQLException {
		Connection conn = SQLConnector.connect();

		// Create Statement
		String sql = "INSERT INTO Photos VALUES (NULL, ?, ?, ?, ?, ?)";
		PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
		stmt.setString(1, name);
		stmt.setString(2, ext);
		stmt.setLong(3, albumId);
		stmt.setLong(4, userid);
		stmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));

		// Get Id
		PhotoDTO photo = null;
		int id = stmt.executeUpdate();
		ResultSet rs = stmt.getGeneratedKeys();
		if (rs.next()) {
			photo = new PhotoDTO(rs.getLong(1), name, ext, albumId, userid);
		}
		stmt.close();
		// conn.close();
		return photo;
	}
}