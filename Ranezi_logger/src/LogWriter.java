

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class LogWriter {

	private static final String PATH_PREFIX = "/Users/sedanurdoganay/Desktop/FightingICE_Workspace/FTG_ver3/log/state/";
	private static String DATE;
	File file;
	FileWriter fw;
	BufferedWriter bw;
	String fileName = "";
	
	String bufferingStr = "";
	
	
	public LogWriter(String fileName) {
		this.fileName=fileName;
		DATE = getDate();
		createFile();
		
	}
	
	private String getDate(){
		Date date = new Date();
		LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		int year  = localDate.getYear();
		int month = localDate.getMonthValue();
		int day   = localDate.getDayOfMonth();
		return ""+year+month+day;
	}
	
	private void createFile() {
		int i=0;
		do{
			String totalPath = PATH_PREFIX+DATE+"_"+this.fileName+"_"+i+".csv";
			file = new File(totalPath);
			i++;
		}while(file.exists());
		
		if (!file.exists()) {
			try {
				file.createNewFile();
				fw = new FileWriter(file.getAbsoluteFile());
				bw = new BufferedWriter(fw);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void bufferCSVString(String str){
		bufferingStr = (bufferingStr.equals(""))? str : bufferingStr + "," + str;
		
	}
	

	public void bufferStringWithLine(String str){
		bufferingStr = (bufferingStr.equals(""))? str : bufferingStr + str;
		bufferNewLine();
		
	}
	
	
	public void bufferNewLine(){
		bufferingStr = bufferingStr + "\n";

	}
	
	public void writeBufferedString(){
		try {
			bw.write(bufferingStr);
			bufferingStr="";

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void writeLine(String content) {

		try {
			bw.write(content+"\n");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void write(String content) {

		try {
			bw.write(content);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void close(){

		try {
			bw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

//	public void startNewFile() {
//		// TODO Auto-generated method stub
//		try {
//			bw.close();
//			bufferingStr = "";
//			createFile();
//			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}

}
