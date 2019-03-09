package userdrawputinhbase;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;


public class UserDrawPutInHbaseMap extends Mapper<LongWritable, Text, Text, Text>{
	Text k2 = new Text();
	Text v2 = new Text();
	
	@Override
	protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, Text>.Context context)
			throws IOException, InterruptedException {
		String line = value.toString();
		String[] splited = line.split("\\|");
		
		
			k2.set(splited[1]);
			v2.set(line);
	
		context.write(k2, v2);
	}
}
