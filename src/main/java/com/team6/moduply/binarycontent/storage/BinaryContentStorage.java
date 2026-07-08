package com.team6.moduply.binarycontent.storage;

public interface BinaryContentStorage {

  String upload(String key, byte[] bytes, String contentType);

  String generateUrl(String key, String contentType);

  String delete(String key);
}
