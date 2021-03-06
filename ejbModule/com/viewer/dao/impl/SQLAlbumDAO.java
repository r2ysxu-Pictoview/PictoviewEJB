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
import com.viewer.dto.MediaDTO;
import com.viewer.dto.SearchQueryDTO;
import com.viewer.util.MediaFileUtils;

public class SQLAlbumDAO implements AlbumDAO {

	public enum MediaType {
		UNKNOWN, PHOTO, VIDEO;
	}

	public static SQLAlbumDAO createPhotoDAO() {
		return new SQLAlbumDAO(MediaType.PHOTO);
	}
	
	public static SQLAlbumDAO createVideoDAO() {
		return new SQLAlbumDAO(MediaType.VIDEO);
	}

	private SQLAlbumDAO(MediaType mediaType) {
		this.mediaType = mediaType;
	}

	private static final String ALBUM_BASIC_USER_PROJECTION = "Albums.id, Albums.owner, Albums.name, Albums.subtitle, Albums.description, UserSubscriptions.id";
	private final MediaType mediaType;

	/** Album Fetch Methods **/

	private String fetchOrdering(int ordering) {
		switch (ordering) {
		case 1 : return "name";
		default : return "lastModifiedDate";
		}
	}

	public List<AlbumDTO> fetchAllPublicAlbums(int limit, int offset) throws SQLException {
		List<AlbumDTO> dto = new ArrayList<AlbumDTO>();
		Connection conn = SQLConnector.connect();

		// Create Statement
		String sql = "SELECT Albums.id, Albums.owner, Albums.name, Albums.subtitle, Albums.description FROM Albums"
				+ " WHERE Albums.permission = 'PUBLIC' AND Albums.parent = 0 GROUP BY Albums.id LIMIT " + limit + " OFFSET "
				+ offset + " AND Albums.mediaType = " + mediaType.ordinal();
		PreparedStatement stmt = conn.prepareStatement(sql);

		// Execute Statement
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			AlbumDTO album = new AlbumDTO(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), 0, rs.getString(5),
					false);
			dto.add(album);
		}
		stmt.close();

		// conn.close();
		return dto;
	}

	public List<AlbumDTO> fetchViewableAlbums(long userid, long parentId, int ordering, int limit, int offset) throws SQLException {
		List<AlbumDTO> dto = new ArrayList<AlbumDTO>();
		Connection conn = SQLConnector.connect();

		// Create Statement
		String selectViewable = "SELECT " + ALBUM_BASIC_USER_PROJECTION + ", AVG(AlbumRatings.rating)" + " FROM AlbumAccess"
				+ " LEFT JOIN Albums ON Albums.owner = AlbumAccess.owner AND Albums.id = AlbumAccess.albumid"
				+ " LEFT JOIN UserSubscriptions ON (AlbumAccess.visitor = UserSubscriptions.uid OR Albums.permission = 'PUBLIC' ) AND Albums.id = UserSubscriptions.albumid"
				+ " LEFT JOIN AlbumRatings ON Albums.id = AlbumRatings.albumid"
				+ " WHERE (AlbumAccess.visitor = ? OR Albums.permission = 'PUBLIC') AND Albums.parent = ? AND Albums.mediaType = " + mediaType.ordinal()
				+ " GROUP BY Albums.id"
				+ " ORDER BY Albums."+ fetchOrdering(ordering) +" DESC"
				+ " LIMIT ? OFFSET ?";
		PreparedStatement stmt = conn.prepareStatement(selectViewable);
		stmt.setLong(1, userid);
		stmt.setLong(2, parentId);
		stmt.setLong(3, limit);
		stmt.setLong(4, offset);

		// Execute Statement
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			AlbumDTO album = new AlbumDTO(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), 0, rs.getString(5),
					rs.getString(6) != null);
			album.setRating(rs.getDouble(7));
			dto.add(album);
		}
		// conn.close();
		return dto;
	}

	public List<AlbumDTO> fetchUserAlbums(long userid, long parentId, int ordering, int limit, int offset) throws SQLException {
		List<AlbumDTO> dto = new ArrayList<AlbumDTO>();
		Connection conn = SQLConnector.connect();

		// Create Statement
		String selectViewable = "SELECT " + ALBUM_BASIC_USER_PROJECTION  + ", AVG(AlbumRatings.rating)" + " FROM Albums"
				+ " LEFT JOIN UserSubscriptions ON Albums.owner = UserSubscriptions.uid AND Albums.id = UserSubscriptions.albumid"
				+ " LEFT JOIN AlbumRatings ON Albums.id = AlbumRatings.albumid"
				+ " WHERE Albums.owner = ? AND Albums.parent = ? AND Albums.mediaType = " + mediaType.ordinal()
				+ " GROUP BY Albums.id"
				+ " ORDER BY Albums." + fetchOrdering(ordering) + " DESC"
				+ " LIMIT ? OFFSET ?";
		PreparedStatement stmt = conn.prepareStatement(selectViewable);
		stmt.setLong(1, userid);
		stmt.setLong(2, parentId);
		stmt.setLong(3, limit);
		stmt.setLong(4, offset);

		// Execute Statement
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			AlbumDTO album = new AlbumDTO(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), 0, rs.getString(5),
					rs.getString(6) != null);
			dto.add(album);
		}
		// conn.close();
		return dto;
	}

	public List<AlbumDTO> fetchAllSubscribedAlbums(long userid, long parentId, int ordering, int limit, int offset) throws SQLException {
		List<AlbumDTO> dto = new ArrayList<AlbumDTO>();
		Connection conn = SQLConnector.connect();

		// Create Statement
		String selectSubscribed = "SELECT " + ALBUM_BASIC_USER_PROJECTION + ", AVG(AlbumRatings.rating)" + " FROM AlbumAccess"
				+ " LEFT JOIN Albums ON Albums.owner = AlbumAccess.owner AND Albums.id = AlbumAccess.albumid"
				+ " LEFT JOIN UserSubscriptions ON (AlbumAccess.visitor = UserSubscriptions.uid OR Albums.permission = 'PUBLIC' ) AND Albums.id = UserSubscriptions.albumid"
				+ " LEFT JOIN AlbumRatings ON Albums.id = AlbumRatings.albumid"
				+ " WHERE (AlbumAccess.visitor = ? OR Albums.permission = 'PUBLIC') AND Albums.parent = ? AND Albums.mediaType = " + mediaType.ordinal()
				+ " AND UserSubscriptions.uid = ?"
				+ " GROUP BY Albums.id"
				+ " ORDER BY Albums." + fetchOrdering(ordering) + " DESC"
				+ " LIMIT ? OFFSET ?";
		PreparedStatement stmt = conn.prepareStatement(selectSubscribed);
		stmt.setLong(1, userid);
		stmt.setLong(2, parentId);
		stmt.setLong(3, userid);
		stmt.setLong(4, limit);
		stmt.setLong(5, offset);

		// Execute Statement
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			AlbumDTO album = new AlbumDTO(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), parentId,
					rs.getString(5), true);
			dto.add(album);
		}
		// conn.close();
		return dto;
	}

	private List<AlbumDTO> fetchSearchAlbums(long userid, SearchQueryDTO searchQuery, String sql) throws SQLException {
		List<AlbumDTO> dto = new ArrayList<AlbumDTO>();
		Connection conn = SQLConnector.connect();

		List<String> names = searchQuery.getNames();
		List<CategoryDTO> categories = searchQuery.getAllTags();

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
		sql += " GROUP BY Albums.id";
		sql += " ORDER BY Albums.lastModifiedDate";
		sql += " LIMIT ? OFFSET ?";
		PreparedStatement stmt = conn.prepareStatement(sql);

		// Set Statement
		stmt.setLong(1, userid);
		stmt.setLong(2, userid);
		int index = 3;
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
		stmt.setLong(index++, searchQuery.getLimit());
		stmt.setLong(index++, searchQuery.getOffset());

		// Execute Statement
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			AlbumDTO album = new AlbumDTO(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getLong(7),
					rs.getString(5), rs.getString(6) != null);
			dto.add(album);
		}
		// conn.close();
		return dto;
	}

	public List<AlbumDTO> fetchSearchUserViewableAlbums(long userid, SearchQueryDTO searchQuery) throws SQLException {
		String selectViewable = "SELECT " + ALBUM_BASIC_USER_PROJECTION + ", Albums.parent FROM AlbumAccess"
				+ " LEFT JOIN Albums ON Albums.owner = AlbumAccess.owner AND Albums.id = AlbumAccess.albumid"
				+ " LEFT JOIN UserSubscriptions ON AlbumAccess.visitor = UserSubscriptions.uid AND Albums.id = UserSubscriptions.albumid"
				+ " LEFT JOIN AlbumTags ON AlbumTags.albumid = Albums.id"
				+ " LEFT JOIN TagCategory ON AlbumTags.cateid = TagCategory.id"
				+ " WHERE (AlbumAccess.visitor = ? OR AlbumAccess.owner = ? OR Albums.permission = 'PUBLIC')"
				+ " AND Albums.mediaType = " + mediaType.ordinal();
		return fetchSearchAlbums(userid, searchQuery, selectViewable);
	}

	public List<AlbumDTO> fetchSearchUserAlbums(long userid, SearchQueryDTO searchQuery) throws SQLException {
		String sql = "SELECT " + ALBUM_BASIC_USER_PROJECTION + ", Albums.parent FROM Albums"
				+ " LEFT JOIN UserSubscriptions ON Albums.owner = UserSubscriptions.uid AND Albums.id = UserSubscriptions.albumid"
				+ " LEFT JOIN AlbumTags ON AlbumTags.albumid = Albums.id"
				+ " LEFT JOIN TagCategory ON AlbumTags.cateid = TagCategory.id"
				+ " WHERE (Albums.owner = ? AND Albums.owner = ? OR Albums.permission = 'PUBLIC')"
				+ " AND Albums.mediaType = " + mediaType.ordinal();
		return fetchSearchAlbums(userid, searchQuery, sql);
	}

	public List<AlbumDTO> fetchSearchUserSubscribedAlbums(long userid, SearchQueryDTO searchQuery) throws SQLException {
		String sql = "SELECT " + ALBUM_BASIC_USER_PROJECTION + ", Albums.parent FROM AlbumAccess"
				+ " LEFT JOIN Albums ON Albums.owner = AlbumAccess.owner AND Albums.id = AlbumAccess.albumid"
				+ " LEFT JOIN UserSubscriptions ON AlbumAccess.visitor = UserSubscriptions.uid AND Albums.id = UserSubscriptions.albumid"
				+ " LEFT JOIN AlbumTags ON AlbumTags.albumid = Albums.id"
				+ " LEFT JOIN TagCategory ON AlbumTags.cateid = TagCategory.id"
				+ " WHERE (AlbumAccess.visitor = ? AND AlbumAccess.owner = ? OR Albums.permission = 'PUBLIC')"
				+ " AND Albums.mediaType = " + mediaType.ordinal();
		return fetchSearchAlbums(userid, searchQuery, sql);
	}

	/** Album Access Methods **/

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

	public long createAlbum(long userid, String name, String subtitle, String description, String permission)
			throws SQLException {
		Connection conn = SQLConnector.connect();

		String sql = "INSERT INTO Albums VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
		stmt.setString(1, name);
		stmt.setString(2, subtitle);
		stmt.setLong(3, userid);
		stmt.setLong(4, 0); // Coverid
		stmt.setLong(5, 0); // ParentId
		stmt.setString(6, description);
		stmt.setString(7, permission);
		stmt.setInt(8, mediaType.ordinal());
		stmt.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
		int albumid = stmt.executeUpdate();
		ResultSet rs = stmt.getGeneratedKeys();
		if (rs.next()) {
			albumid = rs.getInt(1);
		}
		stmt.close();
		addOwnerPermissionToAlbum(userid, albumid, conn);
		tagAlbum(userid, new ArrayList<String>(), albumid, "tags");
		// conn.close();
		return albumid;
	}

	public long createAlbum(long userid, String name, String subtitle, String description, long parentId) throws SQLException {
		Connection conn = SQLConnector.connect();

		String sql = "INSERT INTO Albums VALUES (NULL, ?, ?, ?, ?, ?, ?, (SELECT Albums.permission FROM Albums WHERE id = ?), ?, ?)";
		PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
		stmt.setString(1, name);
		stmt.setString(2, subtitle);
		stmt.setLong(3, userid);
		stmt.setLong(4, 0); // Coverid
		stmt.setLong(5, parentId);
		stmt.setString(6, description);
		stmt.setLong(7, parentId);
		stmt.setInt(8, mediaType.ordinal());
		stmt.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
		int albumid = stmt.executeUpdate();
		ResultSet rs = stmt.getGeneratedKeys();
		if (rs.next()) {
			albumid = rs.getInt(1);
		}
		stmt.close();

		// Add Permission
		addOwnerPermissionToAlbum(userid, albumid, conn);
		tagAlbum(userid, new ArrayList<String>(), albumid, "tags");

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

	public boolean deleteAlbum(long userid, long albumId) throws SQLException {
		Connection conn = SQLConnector.connect();

		String deleteMedia = "DELETE FROM Media WHERE Media.AlbumId = ? AND EXISTS (SELECT Users.uid FROM Users WHERE Users.uid = ?)";

		PreparedStatement stmt = conn.prepareStatement(deleteMedia);
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

	/** Category & Tag Methods **/

	public boolean voteAlbumRating(long userid, long albumId, int rating) throws SQLException {
		Connection conn = SQLConnector.connect();

		// Create Statement
		String sql = "INSERT OR REPLACE INTO AlbumRatings SELECT NULL, ?, ?, ? "
				+ " WHERE EXISTS (SELECT 1 FROM AlbumAccess WHERE AlbumAccess.albumid = ? AND (AlbumAccess.visitor = ? OR Albums.permission = 'PUBLIC'))";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setInt(1, rating);
		stmt.setLong(2, albumId);
		stmt.setLong(3, userid);

		stmt.setLong(4, albumId);
		stmt.setLong(5, userid);

		int row = stmt.executeUpdate();
		stmt.close();
		// conn.close();
		return row > 0;
	}

	public int fetchAlbumAverageRating(long userid, long albumId) throws SQLException {
		Connection conn = SQLConnector.connect();
		int rating = -1;

		// Create Statement
		String sql = "SELECT AVG(rating) FROM AlbumRatings WHERE albumid = ?" + " AND EXISTS ("
				+ "SELECT 1 FROM AlbumAccess WHERE AlbumAccess.albumid = ? AND (AlbumAccess.visitor = ? OR Albums.permission = 'PUBLIC')"
				+ ")" + " GROUP BY albumid";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setLong(1, albumId);
		stmt.setLong(2, userid);

		ResultSet rs = stmt.executeQuery();
		if (rs.next()) rating = rs.getInt(1);
		stmt.close();
		// conn.close();
		return rating;
	}

	public int fetchAlbumUserRating(long userid, long albumId) throws SQLException {
		Connection conn = SQLConnector.connect();
		int rating = -1;

		// Create Statement
		String sql = "SELECT rating FROM AlbumRatings WHERE albumid = ? AND userid = ?";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setLong(1, albumId);
		stmt.setLong(2, userid);

		ResultSet rs = stmt.executeQuery();
		if (rs.next()) rating = rs.getInt(1);
		stmt.close();
		// conn.close();
		return rating;
	}

	public boolean tagAlbum(long userid, String name, long albumid, String category) throws SQLException {
		Connection conn = SQLConnector.connect();
		String sql = "INSERT INTO AlbumTags VALUES (NULL, ?, ?, (SELECT TagCategory.id FROM TagCategory WHERE TagCategory.name = ?), 1)";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setString(1, name);
		stmt.setLong(2, albumid);
		stmt.setString(3, category);
		stmt.executeUpdate();
		// conn.close();
		return true;
	}

	public boolean tagAlbum(long userid, List<String> names, long albumid, String category) throws SQLException {
		if (names == null || names.isEmpty()) return false;
		Connection conn = SQLConnector.connect();
		String categorySQL = "SELECT TagCategory.id FROM TagCategory WHERE TagCategory.name = ?";
		PreparedStatement categoryStmt = conn.prepareStatement(categorySQL);
		categoryStmt.setString(1, category);
		ResultSet categorySet = categoryStmt.executeQuery();

		if (categorySet.next() && !names.isEmpty()) {
			long cateId = categorySet.getLong(1);

			String sql = "INSERT INTO AlbumTags VALUES (NULL, ?, ?, ?, 1)";
			PreparedStatement stmt = conn.prepareStatement(sql);

			for (String name : names) {
				stmt.setString(1, name);
				stmt.setLong(2, albumid);
				stmt.setLong(3, cateId);
				stmt.addBatch();
			}
			stmt.executeBatch();
		} else {
			return false;
		}
		// conn.close();
		return true;
	}

	public boolean tagRelevanceAlbum(long tagId) throws SQLException {
		Connection conn = SQLConnector.connect();
		String sql = "UPDATE AlbumTags SET relevance = relevance + 1 WHERE id = ?";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setLong(1, tagId);
		stmt.executeUpdate();
		// conn.close();
		return true;
	}

	public boolean clearAlbumTag(long albumid) throws SQLException {
		Connection conn = SQLConnector.connect();
		String sql = "DELETE FROM AlbumTags WHERE albumid = ?";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setLong(1, albumid);
		stmt.executeUpdate();
		// conn.close();
		return true;
	}

	public AlbumTagsDTO fetchUserAlbumTags(long userid, long albumid) throws SQLException {
		Connection conn = SQLConnector.connect();

		// Create Statement
		String sql = "SELECT AlbumTags.id, AlbumTags.name, TagCategory.name, AlbumTags.relevance FROM AlbumTags"
				+ " LEFT JOIN TagCategory ON AlbumTags.cateid = TagCategory.id"
				+ " LEFT JOIN Albums ON Albums.id = AlbumTags.albumid"
				+ " LEFT JOIN AlbumAccess ON Albums.owner = AlbumAccess.owner AND Albums.id = AlbumAccess.albumid"
				+ " WHERE (AlbumAccess.visitor = ? OR Albums.permission = 'PUBLIC') AND Albums.id = ?";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setLong(1, userid);
		stmt.setLong(2, albumid);

		// Execute Statement
		ResultSet rs = stmt.executeQuery();
		AlbumTagsDTO tags = new AlbumTagsDTO(albumid);
		while (rs.next()) {
			tags.insertTag(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getInt(4));
		}
		stmt.close();
		// conn.close();
		return tags;
	}

	public int createCategory(long userid, String name) throws SQLException {
		Connection conn = SQLConnector.connect();
		String sql = "INSERT OR IGNORE INTO TagCategory VALUES (NULL, ?, 'PUBLIC')";
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

	/** Photo Methods **/

	public MediaDTO fetchAlbumCoverPhoto(long userid, long albumid) throws SQLException {
		Connection conn = SQLConnector.connect();

		// Create Statement
		String selectMedia = "SELECT Albums.coverid, Media.name, Media.ext FROM Albums"
				+ " LEFT JOIN Media ON Media.id = Albums.coverid"
				+ " LEFT JOIN AlbumAccess ON AlbumAccess.albumid = Albums.id"
				+ " WHERE (AlbumAccess.visitor = ? OR Albums.permission = 'PUBLIC') AND Media.albumid = ? ";
		PreparedStatement stmt = conn.prepareStatement(selectMedia);
		stmt.setLong(1, userid);
		stmt.setLong(2, albumid);

		// Execute Statement
		MediaDTO photo = null;
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			photo = new MediaDTO(rs.getLong(1), rs.getString(2), rs.getString(3), albumid, userid, mediaType.ordinal());
		}
		// conn.close();
		return photo;
	}

	public MediaDTO insertMedia(long userid, long albumId, String name, String ext) throws SQLException {
		Connection conn = SQLConnector.connect();

		// Create Statement
		String sql = "INSERT INTO Media VALUES (NULL, ?, ?, ?, ?, ?, ?)";
		PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
		stmt.setString(1, name);
		stmt.setString(2, ext);
		stmt.setLong(3, albumId);
		stmt.setLong(4, userid);
		stmt.setLong(5, MediaFileUtils.convertType(ext).ordinal());
		stmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));

		// Get Id
		MediaDTO photo = null;
		stmt.executeUpdate();
		ResultSet rs = stmt.getGeneratedKeys();
		if (rs.next()) photo = new MediaDTO(rs.getLong(1), name, ext, albumId, userid, mediaType.ordinal());
		stmt.close();
		// conn.close();
		return photo;
	}

	/** Photo Methods **/

	public List<MediaDTO> fetchUserAlbumMedia(long userid, long albumid) throws SQLException {
		List<MediaDTO> media = new ArrayList<MediaDTO>();
		Connection conn = SQLConnector.connect();

		// Create Statement
		String selectMedia = "SELECT Media.id, Media.name, Albums.id FROM Media"
				+ " LEFT JOIN Albums ON Media.albumid = Albums.id"
				+ " LEFT JOIN AlbumAccess ON AlbumAccess.albumid = Albums.id AND AlbumAccess.owner = Albums.owner"
				+ " WHERE (AlbumAccess.visitor = ? OR Albums.permission = 'PUBLIC') AND Media.albumid = ? ORDER BY Media.name";
		PreparedStatement stmt = conn.prepareStatement(selectMedia);
		stmt.setLong(1, userid);
		stmt.setLong(2, albumid);

		// Execute Statement
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			media.add(new MediaDTO(rs.getLong(1), rs.getString(2), rs.getString(3), albumid, userid, mediaType.ordinal()));
		}
		// conn.close();
		return media;
	}

	public MediaDTO fetchMedia(long userid, long photoid) throws SQLException {
		Connection conn = SQLConnector.connect();

		// Create Statement
		String selectMedia = "SELECT Media.id, Media.name, Media.ext, Media.albumid FROM Media"
				+ " WHERE Media.owner = ? AND Media.id = ?";
		PreparedStatement stmt = conn.prepareStatement(selectMedia);
		stmt.setLong(1, userid);
		stmt.setLong(2, photoid);

		// Execute Statement
		MediaDTO photo = null;
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
			photo = new MediaDTO(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4), userid, mediaType.ordinal());
		}
		// conn.close();
		return photo;
	}
}