/**
 * Copyright (c) 2011, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.crunch;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.newInputStreamSupplier;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.cloudera.crunch.fn.MapKeysFn;
import com.cloudera.crunch.fn.MapValuesFn;
import com.cloudera.crunch.impl.mr.MRPipeline;
import com.cloudera.crunch.io.From;
import com.cloudera.crunch.lib.Cogroup;
import com.cloudera.crunch.type.PTableType;
import com.cloudera.crunch.type.PTypeFamily;
import com.cloudera.crunch.type.avro.AvroTypeFamily;
import com.cloudera.crunch.type.writable.WritableTypeFamily;
import com.google.common.base.Splitter;
import com.google.common.io.Files;

public class CogroupTest {

  private static class WordSplit extends DoFn<String, Pair<String, Long>> {
    @Override
    public void process(String input, Emitter<Pair<String, Long>> emitter) {
      for (String word : Splitter.on(' ').split(input)) {
        emitter.emit(Pair.of(word, 1L));
      }
    }
  }

  public static PTable<String, Long> join(PCollection<String> w1,
      PCollection<String> w2, PTypeFamily ptf) {
    PTableType<String, Long> ntt = ptf.tableOf(ptf.strings(), ptf.longs());
    PTable<String, Long> ws1 = w1.parallelDo("ws1", new WordSplit(), ntt);
    PTable<String, Long> ws2 = w2.parallelDo("ws2", new WordSplit(), ntt);
    PTable<String, Pair<Collection<Long>, Collection<Long>>> cg = Cogroup.cogroup(ws1, ws2);
    PTable<String, Long> sums = cg.parallelDo(
        "wc",
        new MapValuesFn<String, Pair<Collection<Long>, Collection<Long>>, Long>() {
          @Override
          public Long map(Pair<Collection<Long>, Collection<Long>> v) {
            long sum = 0L;
            for (Long value : v.first()) {
              sum += value;
            }
            for (Long value : v.second()) {
              sum += value;
            }
            return sum;
          }
        }, ntt);
    return sums.parallelDo("firstletters", new MapKeysFn<String, String, Long>() {
      @Override
      public String map(String k1) {
        if (k1.length() > 0) {
          return k1.substring(0, 1).toLowerCase();
        } else {
          return "";
        }
      }
    }, ntt).groupByKey().combineValues(CombineFn.<String>SUM_LONGS());
  }

  @Test
  public void testWritableJoin() throws Exception {
    run(new MRPipeline(CogroupTest.class), WritableTypeFamily.getInstance());
  }
  
  @Test
  public void testAvroJoin() throws Exception {
    run(new MRPipeline(CogroupTest.class), AvroTypeFamily.getInstance());
  }
  
  public void run(Pipeline pipeline, PTypeFamily typeFamily) throws IOException {
    File shakesInput = File.createTempFile("shakes", "txt");
    shakesInput.deleteOnExit();
    Files.copy(newInputStreamSupplier(getResource("shakes.txt")), shakesInput);
    File maughamInput = File.createTempFile("maugham", "txt");
    maughamInput.deleteOnExit();
    Files.copy(newInputStreamSupplier(getResource("maugham.txt")), maughamInput);
    
    File output = File.createTempFile("output", "");
    String outputPath = output.getAbsolutePath();
    output.delete();
    
    PCollection<String> shakespeare = pipeline.read(From.textFile(shakesInput.getAbsolutePath()));
    PCollection<String> maugham = pipeline.read(From.textFile(maughamInput.getAbsolutePath()));
    pipeline.writeTextFile(join(shakespeare, maugham, typeFamily), outputPath);
    pipeline.done();
    
    File outputFile = new File(output, "part-r-00000");
    List<String> lines = Files.readLines(outputFile, Charset.defaultCharset());
    boolean passed = false;
    for (String line : lines) {
      if (line.equals("j\t705")) {
        passed = true;
        break;
      }
    }
    assertTrue(passed);
    
    output.deleteOnExit();
  }
}
