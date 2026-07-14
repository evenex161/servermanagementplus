package com.servermanagement.updater;

public record UpdateInfo(String version, String downloadUrl, String changelog, String releaseDate, String source) {
}
