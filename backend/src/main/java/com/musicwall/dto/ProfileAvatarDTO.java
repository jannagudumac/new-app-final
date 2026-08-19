package com.musicwall.dto;

public class ProfileAvatarDTO {
    private final byte[] image;
    private final String contentType;

    public ProfileAvatarDTO(byte[] image, String contentType) {
        this.image = image;
        this.contentType = contentType;
    }

    public byte[] getImage() { return image; }
    public String getContentType() { return contentType; }
}
