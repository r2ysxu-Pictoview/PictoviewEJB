package com.viewer.beans;

import java.io.InputStream;
import java.util.List;

import javax.ejb.Local;
import javax.imageio.stream.ImageInputStream;

import com.viewer.dto.AlbumDTO;
import com.viewer.dto.AlbumTagsDTO;
import com.viewer.dto.PhotoDTO;
import com.viewer.dto.SearchQueryDTO;

@Local
public interface AlbumBeanLocal {

	/**
	 * Fetches all Albums associated with user
	 * 
	 * @param userid
	 * @param parentId
	 * @return List of DTO encapsulating information regarding album
	 */
	public List<AlbumDTO> fetchAllUserAlbums(long userid, long parentId);

	/**
	 * Fetches all albums matching search criteria (Regardless of file hierarchy
	 * duplication)
	 * 
	 * @param userid
	 * @param searchQuery
	 *            DTO encapsulating information regarding search criteria. This
	 *            includes the name and list of category tag associations.
	 * @return List of DTO encapsulating information regarding album
	 */
	public List<AlbumDTO> fetchSearchedUserAlbums(long userid,
			SearchQueryDTO searchQuery);

	/**
	 * Fetches a single Album associated with user
	 * 
	 * @param userid
	 * @param albumid
	 * @return List of DTO encapsulating information regarding album
	 */
	public AlbumDTO fetchUserAlbumInfo(long userid, long albumid);

	/**
	 * Fetches meta-data for list of photos
	 * 
	 * @param userid
	 * @param albumid
	 * @return List of DTO encapsulating information regarding photo including
	 *         source directory
	 */
	public List<PhotoDTO> fetchUserAlbumPhotos(long userid, long albumid);

	/**
	 * Fetches meta-data of photo
	 * 
	 * @param userid
	 * @param photoid
	 * @return DTO encapsulating information regarding photo including source
	 *         directory
	 */
	public PhotoDTO fetchPhoto(long userid, long photoid);

	/**
	 * Create a new empty album
	 * 
	 * @param userid
	 *            ID of user
	 * @param name
	 *            Name of album
	 * @param subtitle
	 *            Additional name information of the album
	 * @param parentId
	 *            ID of the album's parent
	 * @return ID of created album
	 */
	public long createAlbum(long userid, String name, String subtitle,
			long parentId);

	/**
	 * Fetches the data encapsulated in the photo
	 * 
	 * @param userid
	 * @param photoid
	 * @return Stream of photo file data
	 */
	public ImageInputStream fetchPhotoData(long userid, long photoid);

	/**
	 * Fetches the data encapsulated in the photo thumbnail
	 * 
	 * @param userid
	 * @param photoid
	 * @param flags
	 * @return Stream of photo thumbnail file data
	 */
	public ImageInputStream fetchPhotoThumbnailData(long userid, long photoid,
			int flags);

	/**
	 * Fetches the data encapsulated in the photo thumbnail
	 * 
	 * @param userid
	 * @param photoid
	 * @return Stream of photo thumbnail file data
	 */
	public ImageInputStream fetchPhotoThumbnailData(long userid, long photoid);

	/**
	 * Fetches all the tags and categories associated with the album
	 * 
	 * @param userid
	 * @param albumid
	 * @return DTO encapsulating the id and map of category and tags
	 */
	public AlbumTagsDTO fetchUserAlbumTags(long userid, long albumid);

	/**
	 * Creates a tag associated to the album
	 * 
	 * @param userid
	 * @param albumid
	 * @param tag
	 * @param category
	 * @return Success status of action
	 */
	boolean tagUserAlbum(long userid, long albumid, String tag, String category);

	/**
	 * Creates an category and associates it with the user
	 * 
	 * @param userid
	 * @param category
	 * @return Success status of action
	 */
	boolean createCategory(long userid, String category);

	/**
	 * Fetches all categories associated with the user
	 * 
	 * @param userid
	 * @return List of categories
	 */
	public List<String> fetchAllUserCategories(long userid);

	/**
	 * Uploads a photo to server file repository
	 * 
	 * @param userid
	 * @param albumId
	 * @param name
	 *            New name of file
	 * @param data
	 *            InputStream to file being transferred
	 * @return Success status of action
	 */
	public boolean uploadPhoto(long userid, long albumId, String name,
			InputStream data, int flags);
}