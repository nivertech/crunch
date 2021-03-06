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
package com.cloudera.crunch.io;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Scan;

import com.cloudera.crunch.SourceTarget;
import com.cloudera.crunch.io.avro.AvroFileSourceTarget;
import com.cloudera.crunch.io.hbase.HBaseSourceTarget;
import com.cloudera.crunch.io.seq.SeqFileSourceTarget;
import com.cloudera.crunch.io.seq.SeqFileTableSourceTarget;
import com.cloudera.crunch.io.text.TextFileSourceTarget;
import com.cloudera.crunch.type.PType;
import com.cloudera.crunch.type.PTypeFamily;
import com.cloudera.crunch.type.avro.AvroType;

/**
 * Static factory methods for creating various {@link SourceTarget} types.
 *
 */
public class At {
  public static <T> SourceTarget<T> avroFile(String pathName, AvroType<T> avroType) {
	return avroFile(new Path(pathName), avroType);
  }
  
  public static <T> SourceTarget<T> avroFile(Path path, AvroType<T> avroType) {
	return new AvroFileSourceTarget<T>(path, avroType);
  }
  
  public static HBaseSourceTarget hbaseTable(String table) {
	return hbaseTable(table, new Scan());
  }
  
  public static HBaseSourceTarget hbaseTable(String table, Scan scan) {
	return new HBaseSourceTarget(table, scan);
  }
  
  public static <T> SourceTarget<T> sequenceFile(String pathName, PType<T> ptype) {
	return sequenceFile(new Path(pathName), ptype);
  }
  
  public static <T> SourceTarget<T> sequenceFile(Path path, PType<T> ptype) {
	return new SeqFileSourceTarget<T>(path, ptype);
  }
  
  public static <K, V> SeqFileTableSourceTarget<K, V> sequenceFile(String pathName, PType<K> keyType,
      PType<V> valueType) {
	return sequenceFile(new Path(pathName), keyType, valueType);
  }
  
  public static <K, V> SeqFileTableSourceTarget<K, V> sequenceFile(Path path, PType<K> keyType,
      PType<V> valueType) {
	PTypeFamily ptf = keyType.getFamily();
	return new SeqFileTableSourceTarget<K, V>(path, ptf.tableOf(keyType, valueType));
  }
  
  public static SourceTarget<String> textFile(String pathName) {
	return textFile(new Path(pathName));
  }
  
  public static SourceTarget<String> textFile(Path path) {
	return new TextFileSourceTarget(path);
  }  
}
