package org.apache.hadoop.metrics2.sink.timeline;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.codehaus.jackson.map.ObjectMapper;

public class App {

	public static void main(String[] args) {
		ContainerMetric metric = new ContainerMetric();
		metric.setContainerId("container_1450744875949_0001_01_000001");
		metric.setHostName("host1");
		metric.setPmemLimit(2048);
		metric.setVmemLimit(2048);
		metric.setPmemUsedAvg(1024);
		metric.setPmemUsedMin(1024);
		metric.setPmemUsedMax(1024);
		metric.setLaunchDuration(2000);
		metric.setLocalizationDuration(3000);
		long startTime = System.currentTimeMillis();
		long finishTime = startTime + 5000;
		metric.setStartTime(startTime);
		metric.setFinishTime(finishTime);
		metric.setExitCode(0);
		List<ContainerMetric> list = Arrays.asList(metric);
		ObjectMapper mapper  = new ObjectMapper();
		String jsonData = null;
		try {
			jsonData = mapper.writeValueAsString(list);
			System.out.println(jsonData);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
