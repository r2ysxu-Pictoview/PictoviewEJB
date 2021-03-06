package com.test.albums.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.viewer.beans.AlbumBeanLocal;
import com.viewer.beans.PhotoAlbumBean;
import com.viewer.dao.AccountDAO;
import com.viewer.dao.AlbumDAO;
import com.viewer.dao.SQLConnector;
import com.viewer.dao.impl.SQLAccountDAO;
import com.viewer.dao.impl.SQLAlbumDAO;
import com.viewer.dao.impl.SQLAlbumDAO.MediaType;
import com.viewer.dto.MediaDTO;
import com.viewer.dto.UserDataDTO;
import com.viewer.file.ConfigProperties;

public class FileUploadTest {

	private AccountDAO accountDAO;
	private AlbumDAO albumDAO;

	private String mockUser1 = "StandardUser1";
	private String mockUser2 = "StandardUser2";
	private String mockUser3 = "StandardUser3";
	
	private long mockUserId1, mockUserId2, mockUserId3;

	@BeforeClass
	public static void initialize() {
		SQLConnector.connect();
	}

	@AfterClass
	public static void deInitialize() {
		SQLConnector.disconnect();
	}

	@Before
	public void setUp() {
		// Initialize Global Objects
		accountDAO = new SQLAccountDAO();
		albumDAO = SQLAlbumDAO.createPhotoDAO();
		// Creating Mock User
		mockUserId1 = createMockUser(mockUser1);
		mockUserId2 = createMockUser(mockUser2);
		mockUserId3 = createMockUser(mockUser3);
	}

	private boolean deleteMockUsers(String username) {
		try {
			return accountDAO.deleteUser(username, "password");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	@After
	public void tearDown() {
		deleteMockUsers(mockUser1);
		deleteMockUsers(mockUser2);
		deleteMockUsers(mockUser3);

		// Delete Mockuser data files
		File thumbnailDirectory = new File(ConfigProperties.getProperty("thumbnailDirectory") + mockUser1);
		File fullImageDirectory = new File(ConfigProperties.getProperty("albumDirectory") + mockUser1);
		/*
		for (File albumFile : thumbnailDirectory.listFiles())
			for (File photoFile : albumFile.listFiles())
				photoFile.delete();
		thumbnailDirectory.delete();
		for (File albumFile : fullImageDirectory.listFiles())
			for (File photoFile : albumFile.listFiles())
				photoFile.delete();
		fullImageDirectory.delete(); */
	}

	private long createMockUser(String username) {
		try {
			UserDataDTO userData = UserDataDTO.createRegularUser(username, "password", "Mock#" + username, true,
					username + "@email.com", "Test User #" + username);
			return accountDAO.registerUser(userData);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}

	@Test
	public void test1() {
		try {
			InputStream data = new FileInputStream(
					new File(ConfigProperties.getProperty("testStorageDirectory") + "image1.jpg"));

			AlbumBeanLocal albumBean = new PhotoAlbumBean();
			long albumid = albumBean.createAlbum(mockUserId1, "TestAlbum1", "", "Description", 0);
			MediaDTO photo1 = albumBean.uploadMedium(mockUserId1, albumid, "image1.jpg", "jpg", data, 0);

			albumDAO.deleteAlbum(mockUserId1, albumid);
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}
	}
}
