package userdrawmr;

import java.io.IOException;
import java.text.SimpleDateFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import userdrawmr.UserDrawMapReduce2.MyMap2;
import userdrawmr.UserDrawMapReduce2.MyReduce2;
import userdrawputinhbase.UserDrawPutInHbaseMap;
import userdrawputinhbase.UserDrawPutInHbaseReduce;
import util.Config;
import util.TextArrayWritable;

public class UserDrawMapReduce {
	public static Config cong = new Config();

	public static class MyMap extends
			Mapper<LongWritable, Text, Text, TextArrayWritable> {
		Text k = new Text();

		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			String line = value.toString();
			String[] dataArray = line.split(cong.Separator);
			String uiqkey = dataArray[Integer.parseInt(cong.MDN)]
						+ dataArray[Integer.parseInt(cong.appID)]; // MDN + appID
			String[] val = new String[5];
			String timenow = dataArray[Integer.parseInt(cong.Date)];
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			val[0] = sdf.format(Long.parseLong(timenow));//时间
			val[1] = dataArray[Integer.parseInt(cong.MDN)];// 手机号
			val[2] = dataArray[Integer.parseInt(cong.appID)];// appID
			val[3] = "1";// 计数
			val[4] = dataArray[Integer.parseInt(cong.ProcedureTime)];// 使用时长
			k.set(uiqkey);
			context.write(k, new TextArrayWritable(val));

		}
	}

	public static class MyReduce extends
			Reducer<Text, TextArrayWritable, Text, Text> {
		Text v = new Text();
		
		
		public void reduce(Text key, Iterable<TextArrayWritable> values,
				Context context) throws IOException, InterruptedException {
			long sum = 0;
			int count = 0;
			String[] res = new String[5];
			boolean flg = true;
			for (TextArrayWritable t : values) {
				String[] vals = t.toStrings();
				if (flg) {
					res = vals;
				}
				if (vals[3] != null) {
					count = count + 1;

				}
				if (vals[4] != null) {
					sum += Long.valueOf(vals[4]);
				}
			}
			res[3] = String.valueOf(count);
			res[4] = String.valueOf(sum);

			StringBuffer sb = new StringBuffer();
			sb.append(res[0]).append("|");// 时间
			sb.append(res[1]).append("|");// 手机号
			sb.append(res[2]).append("|");// appID
			sb.append(res[3]).append("|");// 计数
			sb.append(res[4]);// 使用时长
			v.set(sb.toString());
			context.write(null, v);
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Job job1 = Job.getInstance(conf, "UserDrawMapReduceJob1");
		job1.setJarByClass(UserDrawMapReduce.class);

		job1.setMapperClass(MyMap.class);
		job1.setReducerClass(MyReduce.class);

		job1.setMapOutputKeyClass(Text.class);
		job1.setMapOutputValueClass(TextArrayWritable.class);
		job1.setOutputKeyClass(Text.class);
		job1.setOutputValueClass(Text.class);

		job1.setInputFormatClass(TextInputFormat.class);
		job1.setOutputFormatClass(TextOutputFormat.class);

		FileInputFormat.addInputPath(job1, new Path(args[0]));// 输入路径
		FileOutputFormat.setOutputPath(job1, new Path(args[1]));// 输出路径

		Boolean state1 = job1.waitForCompletion(true);
		System.out.println("job1执行成功！！！");
			if (state1) {
				conf = new Configuration();
				Job job2 = Job.getInstance(conf, "UserDrawMapReduceJob2");
				job2.setJarByClass(UserDrawMapReduce.class);

				job2.setMapperClass(MyMap2.class);
				job2.setReducerClass(MyReduce2.class);

				job2.setMapOutputKeyClass(Text.class);
				job2.setMapOutputValueClass(Text.class);
				job2.setOutputKeyClass(Text.class);
				job2.setOutputValueClass(Text.class);

				job2.setInputFormatClass(TextInputFormat.class);
				job2.setOutputFormatClass(TextOutputFormat.class);

				FileInputFormat.addInputPath(job2, new Path(args[1]));// 输入路径
				FileOutputFormat.setOutputPath(job2, new Path(args[2]));// 输出路径

				Boolean state2 = job2.waitForCompletion(true);
				System.out.println("job2执行成功！！！");
				if (state2) {
					conf = new Configuration();
					// 设置zookeeper
					conf.set(cong.consite, cong.hbaseip);
					// 设置hbase表名称
					conf.set(TableOutputFormat.OUTPUT_TABLE, cong.tableDraw);
					// 将该值改大，防止hbase超时退出
					conf.set(cong.coftime, cong.time);
					Job job3 = Job.getInstance(conf,
							"UserDrawPutInHbase");
					job3.setJarByClass(UserDrawMapReduce.class);
					TableMapReduceUtil.addDependencyJars(job3);

					FileInputFormat.setInputPaths(job3, new Path(args[2]));

					job3.setMapperClass(UserDrawPutInHbaseMap.class);
					job3.setMapOutputKeyClass(Text.class);
					job3.setMapOutputValueClass(Text.class);

					job3.setReducerClass(UserDrawPutInHbaseReduce.class);
					job3.setOutputFormatClass(TableOutputFormat.class);

					job3.waitForCompletion(true);
				}
		}
	}
}
