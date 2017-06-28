package org.sonar.api.batch.fs.internal.charhandler;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.annotation.CheckForNull;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

public class FileHashComputer extends CharHandler {
  private static final char LINE_FEED = '\n';

  
  private MessageDigest globalMd5Digest = DigestUtils.getMd5Digest();
  private StringBuilder sb = new StringBuilder();
  private final CharsetEncoder encoder;
  private final String filePath;

  public FileHashComputer(String filePath) {
    encoder = StandardCharsets.UTF_8.newEncoder()
      .onMalformedInput(CodingErrorAction.REPLACE)
      .onUnmappableCharacter(CodingErrorAction.REPLACE);
    this.filePath = filePath;
  }

  @Override
  public void handleIgnoreEoL(char c) {
    sb.append(c);
  }

  @Override
  public void newLine() {
    sb.append(LINE_FEED);
    processBuffer();
    sb.setLength(0);
  }

  @Override
  public void eof() {
    if (sb.length() > 0) {
      processBuffer();
    }
  }

  private void processBuffer() {
    try {
      if (sb.length() > 0) {
        ByteBuffer encoded = encoder.encode(CharBuffer.wrap(sb));
        globalMd5Digest.update(encoded.array(), 0, encoded.limit());
      }
    } catch (CharacterCodingException e) {
      throw new IllegalStateException("Error encoding line hash in file: " + filePath, e);
    }
  }

  @CheckForNull
  public String getHash() {
    return Hex.encodeHexString(globalMd5Digest.digest());
  }
}