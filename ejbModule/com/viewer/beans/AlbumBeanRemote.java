package com.viewer.beans;

import java.util.List;

import javax.ejb.Remote;

import com.viewer.dto.AlbumDTO;
import com.viewer.dto.AlbumTagsDTO;
import com.viewer.dto.MediaDTO;

@Remote
public interface AlbumBeanRemote {
	public List<AlbumDTO> fetchAllUserAlbums(long userid, long parentId);
	
	public List<AlbumDTO> fetchSearchedUserAlbums(long userid, String searchName, String... tags);

	public AlbumDTO fetchUserAlbumInfo(long userid, long albumid);

	public List<MediaDTO> fetchUserAlbumPhotos(long userid, long albumid);

	public MediaDTO fetchPhoto(long userid, long photoid);

	public boolean createAlbum(long userid, AlbumDTO album);

	public byte[] fetchPhotoData(long userid, long photoid);
	
	public byte[] fetchPhotoThumbnailData(long userid, long photoid, int flags);
	
	public byte[] fetchPhotoThumbnailData(long userid, long photoid);
	
	public AlbumTagsDTO fetchUserAlbumTags(long userid, long albumid);

	boolean tagUserAlbum(long userid, long albumid, String tag, long cateid);
}
