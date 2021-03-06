/*
 * Copyright 2015, Hridesh Rajan, Robert Dyer, Hoan Nguyen
 *                 and Iowa State University of Science and Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boa.datagen;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.BZip2Codec;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.DefaultCodec;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.io.compress.SnappyCodec;

import com.google.protobuf.CodedInputStream;

import boa.datagen.util.Properties;
import boa.types.Toplevel.Paper;

/**
 * @author hoan
 * @author hridesh
 */
public class SeqCombiner {

	public static void main(String[] args) throws IOException {
		CompressionType compressionType = CompressionType.BLOCK;
		CompressionCodec compressionCode = new DefaultCodec();
		Configuration conf = new Configuration();
//		conf.set("fs.default.name", "hdfs://boa-njt/");
		FileSystem fileSystem = FileSystem.get(conf);
		String base = Properties.getProperty("output.path", DefaultProperties.OUTPUT);

		if (args.length > 0) {
			base = args[0];
		}
		if (args.length > 1) {
			if (args[1].toLowerCase().equals("d"))
				compressionCode = new DefaultCodec();
			else if (args[1].toLowerCase().equals("g"))
				compressionCode = new GzipCodec();
			else if (args[1].toLowerCase().equals("b"))
				compressionCode = new BZip2Codec();
			else if (args[1].toLowerCase().equals("s"))
				compressionCode = new SnappyCodec();
		}

		SequenceFile.Writer paperWriter = SequenceFile.createWriter(fileSystem, conf,
				new Path(base + "/papers.seq"), Text.class, BytesWritable.class, compressionType, compressionCode);

		FileStatus[] files = fileSystem.listStatus(new Path(base + "/paper"), new PathFilter() {
			@Override
			public boolean accept(Path path) {
				String name = path.getName();
				return name.endsWith(".seq") && name.contains("-");
			}

		});

		for (int i = 0; i < files.length; i++) {
			
			FileStatus file = files[i];
			String name = file.getPath().getName();
			System.out.println("Reading file " + (i + 1) + " in " + files.length + ": " + name);
			SequenceFile.Reader r = new SequenceFile.Reader(fileSystem, file.getPath(), conf);
			Text textKey = new Text();
			BytesWritable value = new BytesWritable();
			try {
				while (r.next(textKey, value)) {
					Paper p = Paper.parseFrom(CodedInputStream.newInstance(value.getBytes(), 0, value.getLength()));
					Paper.Builder pb = Paper.newBuilder(p);
					paperWriter.append(textKey, new BytesWritable(pb.build().toByteArray()));
				}
			} catch (Exception e) {
				System.err.println(name);
				e.printStackTrace();
			} finally {
				r.close();
			}
		}
		paperWriter.close();
		fileSystem.close();
	}

}
