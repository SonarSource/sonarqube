package org.sonar.microbenchmark;

import net.jpountz.xxhash.XXHash32;
import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 *
 * See https://code.google.com/p/xxhash/ and https://github.com/jpountz/lz4-java
 *
 */
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 3)
@Measurement(iterations = 3)
@BenchmarkMode(Mode.Throughput)
public class HashBenchmark {

  @Param({"1", "100", "1000", "10000", "100000", "1000000"})
  public int size;

  XXHash32 xxHash32 = XXHashFactory.fastestInstance().hash32();
  XXHash64 xxHash64 = XXHashFactory.fastestInstance().hash64();

  byte[] bytes;

  @Setup
  public void setup() throws Exception {
    bytes = StringUtils.repeat("3", size).getBytes();
  }

  @Benchmark
  public String commonsCodecMd5() throws Exception {
    return DigestUtils.md5Hex(bytes);
  }

  @Benchmark
  public String commonsCodecSha1() throws Exception {
    return DigestUtils.sha1Hex(bytes);
  }

  @Benchmark
  public int xxhash32() throws Exception {
    int seed = 0x9747b28c; // used to initialize the hash value, use whatever
    // value you want, but always the same
    return xxHash32.hash(bytes, 0, bytes.length, seed);
  }

  @Benchmark
  public long xxhash64() throws Exception {
    int seed = 0x9747b28c; // used to initialize the hash value, use whatever
    // value you want, but always the same
    return xxHash64.hash(bytes, 0, bytes.length, seed);
  }

  /**
   * You can this benchmark with maven command-line (see run.sh) or by executing this method
   * in IDE
   */
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
      .include(HashBenchmark.class.getSimpleName())
      .build();
    new Runner(opt).run();
  }
}
