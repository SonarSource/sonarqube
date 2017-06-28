package org.sonar.api.batch.fs.internal.charhandler;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.apache.commons.codec.digest.DigestUtils;
import org.sonar.api.batch.fs.internal.FileMetadata.LineHashConsumer;

public class LineHashComputer extends CharHandler {
  private final MessageDigest lineMd5Digest = DigestUtils.getMd5Digest();
  private final CharsetEncoder encoder;
  private final StringBuilder sb = new StringBuilder();
  private final LineHashConsumer consumer;
  private final File file;
  private int line = 1;

  public LineHashComputer(LineHashConsumer consumer, File f) {
    this.consumer = consumer;
    this.file = f;
    this.encoder = StandardCharsets.UTF_8.newEncoder()
      .onMalformedInput(CodingErrorAction.REPLACE)
      .onUnmappableCharacter(CodingErrorAction.REPLACE);
  }

  @Override
  public void handleIgnoreEoL(char c) {
    if (!Character.isWhitespace(c)) {
      sb.append(c);
    }
  }

  @Override
  public void newLine() {
    processBuffer();
    sb.setLength(0);
    line++;
  }

  @Override
  public void eof() {
    if (this.line > 0) {
      processBuffer();
    }
  }

  private void processBuffer() {
    try {
      if (sb.length() > 0) {
        ByteBuffer encoded = encoder.encode(CharBuffer.wrap(sb));
        lineMd5Digest.update(encoded.array(), 0, encoded.limit());
        consumer.consume(line, lineMd5Digest.digest());
      }
    } catch (CharacterCodingException e) {
      throw new IllegalStateException("Error encoding line hash in file: " + file.getAbsolutePath(), e);
    }
  }
}