package ai.haley.embedded.temphumiditysensor

import java.io.BufferedReader;
import java.io.InputStreamReader;

class HaleyTempHumiditySensorManager {
	private static final String DECIMAL_EXTRACT_REGEX = "[^\\d.]";
	private static final String TEMP_HUM_SPLIT_REGEX = "Humidity";
	private static String line;
	private static String[] data;
	private static float humidity;
	private static float temperature

	private static final String PIN_NUM = "4"
	private static final String PYTHON_COMMAND_LINE = "python /home/pi/Desktop/Adafruit_Python_DHT/examples/AdafruitDHT.py 2302 ";
	
	public HaleyTempHumiditySensorManager(){
		measureTempHumidity();
	}
	
	public static measureTempHumidity() throws Exception {
		// TODO Auto-generated method stub
		Runtime rt= Runtime.getRuntime();
		Process p=rt.exec(PYTHON_COMMAND_LINE+PIN_NUM);
		 BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
		 if((line = bri.readLine()) != null){
			 //if(!(line.contains("ERR_CRC") || line.contains("ERR_RNG"))){

				data=line.split(TEMP_HUM_SPLIT_REGEX);
				//System.out.println("data 1 "+data[0]);
				//System.out.println("data 2 "+data[1]);
				temperature=Float.parseFloat(data[0].replaceAll(DECIMAL_EXTRACT_REGEX,""));
				humidity=Float.parseFloat(data[1].replaceAll(DECIMAL_EXTRACT_REGEX,""));
			 //}
			 //else
			 //	System.out.println("Data Error");
		 }
		  //System.out.println(line);
		  bri.close();
		  p.waitFor();
		  //System.out.println("Temperature is : "+temperature+" 'C Humidity is :"+ humidity+" %RH");
		  //System.out.println("Done.");
	}
	
	public static float getTemp() {
		measureTempHumidity();
		return temperature;
	}
	
	public static float getHumidity() {
		measureTempHumidity();
		return humidity;
	}
}
